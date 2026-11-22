package ru.nsu.habitgrid.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.nsu.habitgrid.data.AddHabitResult
import ru.nsu.habitgrid.data.HabitDatabase
import ru.nsu.habitgrid.data.HabitEntity
import ru.nsu.habitgrid.data.HabitRepository
import ru.nsu.habitgrid.domain.CellState
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class CalendarViewModelTest {

    private val today = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(today.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val todayFlow = MutableStateFlow(today)

    private lateinit var database: HabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitDatabase::class.java,
        ).build()
        repository = HabitRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        runBlocking(Dispatchers.Main) { CalendarViewModel(savedStateHandle, repository, todayFlow) }

    private fun CalendarViewModel.onMain(block: CalendarViewModel.() -> Unit) =
        runBlocking(Dispatchers.Main) { block() }

    private suspend fun CalendarViewModel.awaitState(predicate: (CalendarUiState) -> Boolean) =
        withTimeout(5_000) { uiState.first { !it.isLoading && predicate(it) } }

    private fun CalendarUiState.day(dayOfMonth: Int): CalendarDay =
        weeks.flatten().filterNotNull().first { it.date.dayOfMonth == dayOfMonth }

    @Test
    fun startsOnCurrentMonth() = runBlocking {
        val state = createViewModel().awaitState { true }

        assertEquals(YearMonth.of(2026, 10), state.month)
        assertTrue(state.isCurrentMonth)
    }

    @Test
    fun switchesMonthsBackAndForward() = runBlocking {
        val viewModel = createViewModel()

        viewModel.onMain { onNextMonth() }
        assertEquals(YearMonth.of(2026, 11), viewModel.awaitState { it.month.monthValue == 11 }.month)

        viewModel.onMain { onPreviousMonth(); onPreviousMonth() }
        val state = viewModel.awaitState { it.month.monthValue == 9 }
        assertEquals(YearMonth.of(2026, 9), state.month)
        assertFalse(state.isCurrentMonth)
        assertEquals(30, state.weeks.flatten().filterNotNull().size)
    }

    @Test
    fun nextAfterDecemberIsJanuaryOfNextYear() = runBlocking {
        val viewModel = createViewModel(SavedStateHandle(mapOf(CalendarViewModel.MONTH_KEY to "2026-12")))

        viewModel.onMain { onNextMonth() }

        assertEquals(YearMonth.of(2027, 1), viewModel.awaitState { it.month.year == 2027 }.month)
    }

    @Test
    fun previousBeforeJanuaryIsDecemberOfPreviousYear() = runBlocking {
        val viewModel = createViewModel(SavedStateHandle(mapOf(CalendarViewModel.MONTH_KEY to "2027-01")))

        viewModel.onMain { onPreviousMonth() }

        assertEquals(YearMonth.of(2026, 12), viewModel.awaitState { it.month.year == 2026 }.month)
    }

    @Test
    fun selectedMonthIsRestoredFromSavedState() = runBlocking {
        val savedStateHandle = SavedStateHandle(mapOf(CalendarViewModel.MONTH_KEY to "2026-05"))
        val viewModel = createViewModel(savedStateHandle)

        assertEquals(YearMonth.of(2026, 5), viewModel.awaitState { true }.month)

        viewModel.onMain { onNextMonth() }
        viewModel.awaitState { it.month.monthValue == 6 }
        assertEquals("2026-06", savedStateHandle.get<String>(CalendarViewModel.MONTH_KEY))
    }

    @Test
    fun todayReturnsToCurrentMonth() = runBlocking {
        val viewModel = createViewModel(SavedStateHandle(mapOf(CalendarViewModel.MONTH_KEY to "2025-03")))
        viewModel.awaitState { true }

        viewModel.onMain { onTodayClick() }

        assertTrue(viewModel.awaitState { it.month.year == 2026 }.isCurrentMonth)
    }

    @Test
    fun cellsShowHabitsAndCompletions() = runBlocking {
        val habit = (repository.addHabit("Чтение") as AddHabitResult.Added).habit
        repository.toggleCompletion(habit.id, today)
        val viewModel = createViewModel()

        val state = viewModel.awaitState { it.habits.isNotEmpty() }

        assertEquals(CellState.DONE, state.day(15).cells[0])
        assertEquals(CellState.FUTURE, state.day(16).cells[0])
        assertEquals(CellState.NOT_TRACKED, state.day(14).cells[0])
        assertEquals(CellState.EMPTY_SLOT, state.day(15).cells[1])
        assertTrue(state.day(15).isToday)
    }

    @Test
    fun calendarUpdatesWhenCompletionChanges() = runBlocking {
        val habit = (repository.addHabit("Чтение") as AddHabitResult.Added).habit
        val viewModel = createViewModel()
        assertEquals(CellState.PENDING, viewModel.awaitState { it.habits.isNotEmpty() }.day(15).cells[0])

        repository.toggleCompletion(habit.id, today)

        assertEquals(CellState.DONE, viewModel.awaitState { it.day(15).cells[0] == CellState.DONE }.day(15).cells[0])
    }

    private suspend fun insertHabit(name: String, slot: Int, createdDate: LocalDate): Long =
        database.habitDao().insert(HabitEntity(name = name, slot = slot, createdDate = createdDate))

    @Test
    fun futureDayDoesNotOpenSheet() = runBlocking {
        insertHabit("Чтение", 0, today.minusDays(20))
        val viewModel = createViewModel()
        viewModel.awaitState { it.habits.isNotEmpty() }

        viewModel.onMain { onDayClick(today.plusDays(1)) }

        assertNull(viewModel.awaitState { true }.selectedDay)
    }

    @Test
    fun pastDayOpensSheetWithHabits() = runBlocking {
        insertHabit("Чтение", 0, today.minusDays(20))
        insertHabit("Спорт", 1, today.minusDays(20))
        val viewModel = createViewModel()
        viewModel.awaitState { it.habits.size == 2 }

        viewModel.onMain { onDayClick(today.minusDays(3)) }

        val day = viewModel.awaitState { it.selectedDay != null }.selectedDay!!
        assertEquals(today.minusDays(3), day.date)
        assertEquals(listOf("Чтение", "Спорт"), day.items.map { it.habit.name })
        assertEquals(listOf(CellState.MISSED, CellState.MISSED), day.items.map { it.state })
    }

    @Test
    fun toggleMarksDayGreenAndUnmarkTurnsItRed() = runBlocking {
        val id = insertHabit("Чтение", 0, today.minusDays(20))
        val past = today.minusDays(3)
        val viewModel = createViewModel()
        viewModel.awaitState { it.habits.isNotEmpty() }
        viewModel.onMain { onDayClick(past) }
        viewModel.awaitState { it.selectedDay != null }

        viewModel.onMain { onToggleHabit(id) }
        val done = viewModel.awaitState { it.day(12).cells[0] == CellState.DONE }
        assertTrue(done.selectedDay!!.items.single().isChecked)

        viewModel.onMain { onToggleHabit(id) }
        val missed = viewModel.awaitState { it.day(12).cells[0] == CellState.MISSED }
        assertFalse(missed.selectedDay!!.items.single().isChecked)
    }

    @Test
    fun habitBeforeCreationCannotBeToggled() = runBlocking {
        val id = insertHabit("Спорт", 0, today.minusDays(2))
        val viewModel = createViewModel()
        viewModel.awaitState { it.habits.isNotEmpty() }
        viewModel.onMain { onDayClick(today.minusDays(5)) }
        val item = viewModel.awaitState { it.selectedDay != null }.selectedDay!!.items.single()
        assertFalse(item.isEnabled)

        viewModel.onMain { onToggleHabit(id) }

        assertTrue(repository.completions(today.minusDays(5), today).first().isEmpty())
    }

    @Test
    fun dismissClosesSheet() = runBlocking {
        insertHabit("Чтение", 0, today.minusDays(20))
        val viewModel = createViewModel()
        viewModel.awaitState { it.habits.isNotEmpty() }
        viewModel.onMain { onDayClick(today) }
        viewModel.awaitState { it.selectedDay != null }

        viewModel.onMain { onDismissDay() }

        assertNull(viewModel.awaitState { it.selectedDay == null }.selectedDay)
    }

    @Test
    fun selectedDayIsRestoredFromSavedState() = runBlocking {
        insertHabit("Чтение", 0, today.minusDays(20))
        val savedStateHandle = SavedStateHandle(mapOf(CalendarViewModel.SELECTED_DATE_KEY to "2026-10-10"))
        val viewModel = createViewModel(savedStateHandle)

        assertEquals(LocalDate.of(2026, 10, 10), viewModel.awaitState { it.selectedDay != null }.selectedDay!!.date)
    }

    @Test
    fun nextDayTurnsUnmarkedYesterdayRed() = runBlocking {
        val id = insertHabit("Чтение", 0, today.minusDays(20))
        insertHabit("Спорт", 1, today.minusDays(20))
        repository.toggleCompletion(id, today)
        val viewModel = createViewModel()
        val before = viewModel.awaitState { it.habits.size == 2 }
        assertEquals(listOf(CellState.DONE, CellState.PENDING), before.day(15).cells.take(2))
        assertEquals(listOf(CellState.FUTURE, CellState.FUTURE), before.day(16).cells.take(2))
        assertTrue(before.day(15).isToday)

        todayFlow.value = today.plusDays(1)

        val after = viewModel.awaitState { it.today == today.plusDays(1) }
        assertEquals(listOf(CellState.DONE, CellState.MISSED), after.day(15).cells.take(2))
        assertEquals(listOf(CellState.PENDING, CellState.PENDING), after.day(16).cells.take(2))
        assertTrue(after.day(16).isToday)
        assertFalse(after.day(16).isFuture)
    }

    @Test
    fun dayAfterMidnightBecomesSelectable() = runBlocking {
        insertHabit("Чтение", 0, today.minusDays(20))
        val viewModel = createViewModel()
        viewModel.awaitState { it.habits.isNotEmpty() }
        viewModel.onMain { onDayClick(today.plusDays(1)) }
        assertNull(viewModel.awaitState { true }.selectedDay)

        todayFlow.value = today.plusDays(1)
        viewModel.awaitState { it.today == today.plusDays(1) }
        viewModel.onMain { onDayClick(today.plusDays(1)) }

        assertEquals(today.plusDays(1), viewModel.awaitState { it.selectedDay != null }.selectedDay!!.date)
    }

    @Test
    fun openTodayRequestShowsCurrentMonthWithTodaySheet() = runBlocking {
        insertHabit("Чтение", 0, today.minusDays(20))
        val viewModel = createViewModel(SavedStateHandle(mapOf(CalendarViewModel.MONTH_KEY to "2026-03")))
        viewModel.awaitState { it.habits.isNotEmpty() }

        viewModel.onMain { onOpenTodayRequest(42L) }

        val state = viewModel.awaitState { it.selectedDay != null }
        assertEquals(YearMonth.of(2026, 10), state.month)
        assertEquals(today, state.selectedDay!!.date)
        assertTrue(state.selectedDay!!.isToday)
    }

    @Test
    fun sameRequestIsHandledOnlyOnce() = runBlocking {
        insertHabit("Чтение", 0, today.minusDays(20))
        val viewModel = createViewModel()
        viewModel.awaitState { it.habits.isNotEmpty() }
        viewModel.onMain { onOpenTodayRequest(7L) }
        viewModel.awaitState { it.selectedDay != null }

        viewModel.onMain {
            onDismissDay()
            onPreviousMonth()
            onOpenTodayRequest(7L)
        }

        val state = viewModel.awaitState { it.month.monthValue == 9 }
        assertNull(state.selectedDay)
    }

    @Test
    fun newRequestUsesDateAtTapTime() = runBlocking {
        insertHabit("Чтение", 0, today.minusDays(20))
        val viewModel = createViewModel()
        viewModel.awaitState { it.habits.isNotEmpty() }
        viewModel.onMain { onOpenTodayRequest(1L) }
        viewModel.awaitState { it.selectedDay?.date == today }
        viewModel.onMain { onDismissDay() }

        todayFlow.value = LocalDate.of(2026, 11, 1)
        viewModel.onMain { onOpenTodayRequest(2L) }

        val state = viewModel.awaitState { it.selectedDay?.date == LocalDate.of(2026, 11, 1) }
        assertEquals(YearMonth.of(2026, 11), state.month)
    }

    @Test
    fun zeroRequestDoesNothing() = runBlocking {
        val viewModel = createViewModel(SavedStateHandle(mapOf(CalendarViewModel.MONTH_KEY to "2026-03")))
        viewModel.awaitState { true }

        viewModel.onMain { onOpenTodayRequest(0L) }

        assertEquals(YearMonth.of(2026, 3), viewModel.awaitState { true }.month)
    }
}
