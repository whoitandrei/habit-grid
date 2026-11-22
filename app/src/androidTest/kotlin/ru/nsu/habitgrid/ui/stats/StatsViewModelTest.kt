package ru.nsu.habitgrid.ui.stats

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.nsu.habitgrid.data.HabitDatabase
import ru.nsu.habitgrid.data.HabitEntity
import ru.nsu.habitgrid.data.HabitRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class StatsViewModelTest {

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
        runBlocking(Dispatchers.Main) { StatsViewModel(savedStateHandle, repository, todayFlow) }

    private fun StatsViewModel.onMain(block: StatsViewModel.() -> Unit) =
        runBlocking(Dispatchers.Main) { block() }

    private suspend fun StatsViewModel.awaitState(predicate: (StatsUiState) -> Boolean) =
        withTimeout(5_000) { uiState.first { !it.isLoading && predicate(it) } }

    private suspend fun insertHabit(name: String, slot: Int, createdDate: LocalDate): Long =
        database.habitDao().insert(HabitEntity(name = name, slot = slot, createdDate = createdDate))

    private suspend fun markDays(habitId: Long, days: IntRange) {
        days.forEach { repository.toggleCompletion(habitId, LocalDate.of(2026, 10, it)) }
    }

    @Test
    fun currentMonthCountsPerHabitAndTotal() = runBlocking {
        val reading = insertHabit("Чтение", 0, LocalDate.of(2026, 9, 1))
        val sport = insertHabit("Спорт", 1, LocalDate.of(2026, 10, 10))
        markDays(reading, 1..7)
        markDays(sport, 10..11)

        val stats = createViewModel().awaitState { it.stats.habits.size == 2 }.stats

        assertEquals(listOf(7 to 14, 2 to 5), stats.habits.map { it.done to it.tracked })
        assertEquals(listOf(50, 40), stats.habits.map { it.percent })
        assertEquals(47, stats.percent)
    }

    @Test
    fun todayCountsOnlyWhenMarked() = runBlocking {
        val reading = insertHabit("Чтение", 0, LocalDate.of(2026, 10, 1))
        markDays(reading, 1..14)
        val viewModel = createViewModel()
        assertEquals(14 to 14, viewModel.awaitState { it.stats.habits.isNotEmpty() }.stats.let { it.done to it.tracked })

        markDays(reading, 15..15)

        val stats = viewModel.awaitState { it.stats.done == 15 }.stats
        assertEquals(15, stats.tracked)
        assertEquals(100, stats.percent)
    }

    @Test
    fun unmarkedTodayBecomesMissedAfterMidnight() = runBlocking {
        val reading = insertHabit("Чтение", 0, LocalDate.of(2026, 10, 1))
        markDays(reading, 1..14)
        val viewModel = createViewModel()
        assertEquals(100, viewModel.awaitState { it.stats.habits.isNotEmpty() }.stats.percent)

        todayFlow.value = today.plusDays(1)

        val stats = viewModel.awaitState { it.today == today.plusDays(1) }.stats
        assertEquals(14 to 15, stats.done to stats.tracked)
        assertEquals(93, stats.percent)
    }

    @Test
    fun opensOnMonthFromCalendar() = runBlocking {
        insertHabit("Чтение", 0, LocalDate.of(2026, 9, 1))
        val viewModel = createViewModel(SavedStateHandle(mapOf(StatsViewModel.MONTH_KEY to "2026-09")))

        val state = viewModel.awaitState { it.stats.habits.isNotEmpty() }

        assertEquals(YearMonth.of(2026, 9), state.month)
        assertEquals(0 to 30, state.stats.done to state.stats.tracked)
        assertEquals(0, state.stats.percent)
    }

    @Test
    fun futureMonthHasNoData() = runBlocking {
        insertHabit("Чтение", 0, LocalDate.of(2026, 9, 1))
        val viewModel = createViewModel()
        viewModel.awaitState { it.stats.habits.isNotEmpty() }

        viewModel.onMain { onNextMonth() }

        val state = viewModel.awaitState { it.month == YearMonth.of(2026, 11) }
        assertNull(state.stats.percent)
        assertNull(state.stats.habits.single().percent)
    }

    @Test
    fun monthSwitchingAndTodayButton() = runBlocking {
        val viewModel = createViewModel()
        viewModel.awaitState { true }

        viewModel.onMain { onPreviousMonth(); onPreviousMonth() }
        assertEquals(YearMonth.of(2026, 8), viewModel.awaitState { it.month.monthValue == 8 }.month)

        viewModel.onMain { onTodayClick() }
        assertTrue(viewModel.awaitState { it.month.monthValue == 10 }.isCurrentMonth)
    }

    @Test
    fun noHabitsMeansEmptyStats() = runBlocking {
        val state = createViewModel().awaitState { true }

        assertTrue(state.stats.habits.isEmpty())
        assertNull(state.stats.percent)
    }
}
