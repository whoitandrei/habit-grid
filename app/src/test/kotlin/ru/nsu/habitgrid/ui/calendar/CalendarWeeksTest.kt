package ru.nsu.habitgrid.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.nsu.habitgrid.domain.CellState
import ru.nsu.habitgrid.domain.Habit
import ru.nsu.habitgrid.domain.monthWeeks
import java.time.LocalDate
import java.time.YearMonth

class CalendarWeeksTest {

    private val today = LocalDate.of(2026, 10, 15)
    private val october = YearMonth.of(2026, 10)
    private val reading = Habit(id = 1, name = "Чтение", slot = 0, createdDate = LocalDate.of(2026, 9, 1))
    private val sport = Habit(id = 2, name = "Спорт", slot = 3, createdDate = LocalDate.of(2026, 10, 10))

    private fun day(weeks: List<List<CalendarDay?>>, dayOfMonth: Int): CalendarDay =
        weeks.flatten().filterNotNull().first { it.date.dayOfMonth == dayOfMonth }

    @Test
    fun `layout matches month grid`() {
        val weeks = calendarWeeks(october, today, listOf(reading), emptyMap())

        assertEquals(monthWeeks(october), weeks.map { week -> week.map { it?.date } })
    }

    @Test
    fun `each day has four cells`() {
        val weeks = calendarWeeks(october, today, listOf(reading), emptyMap())

        weeks.flatten().filterNotNull().forEach { assertEquals(Habit.MAX_COUNT, it.cells.size) }
    }

    @Test
    fun `cells follow habit slots`() {
        val weeks = calendarWeeks(october, today, listOf(reading, sport), emptyMap())

        assertEquals(
            listOf(CellState.MISSED, CellState.EMPTY_SLOT, CellState.EMPTY_SLOT, CellState.MISSED),
            day(weeks, 12).cells,
        )
    }

    @Test
    fun `cell states reflect completions and dates`() {
        val completions = mapOf(reading.id to setOf(october.atDay(3), today))
        val weeks = calendarWeeks(october, today, listOf(reading, sport), completions)

        assertEquals(CellState.DONE, day(weeks, 3).cells[0])
        assertEquals(CellState.MISSED, day(weeks, 4).cells[0])
        assertEquals(CellState.DONE, day(weeks, 15).cells[0])
        assertEquals(CellState.PENDING, day(weeks, 15).cells[3])
        assertEquals(CellState.FUTURE, day(weeks, 16).cells[0])
        assertEquals(CellState.NOT_TRACKED, day(weeks, 9).cells[3])
        assertEquals(CellState.MISSED, day(weeks, 10).cells[3])
    }

    @Test
    fun `only today is marked as today`() {
        val weeks = calendarWeeks(october, today, listOf(reading), emptyMap())

        assertEquals(listOf(today), weeks.flatten().filterNotNull().filter { it.isToday }.map { it.date })
    }

    @Test
    fun `month without today has no today mark`() {
        val weeks = calendarWeeks(YearMonth.of(2026, 9), today, listOf(reading), emptyMap())

        assertNull(weeks.flatten().filterNotNull().firstOrNull { it.isToday })
    }

    @Test
    fun `only days after today are future`() {
        val weeks = calendarWeeks(october, today, listOf(reading), emptyMap())

        val days = weeks.flatten().filterNotNull()
        assertEquals((16..31).toList(), days.filter { it.isFuture }.map { it.date.dayOfMonth })
    }

    private fun stateWith(completions: Map<Long, Set<LocalDate>>): CalendarUiState {
        val habits = listOf(reading, sport)
        return CalendarUiState(
            month = october,
            today = today,
            habits = habits,
            weeks = calendarWeeks(october, today, habits, completions),
            isLoading = false,
        )
    }

    @Test
    fun `selected day lists habits with their states`() {
        val state = stateWith(mapOf(reading.id to setOf(october.atDay(12))))
            .withSelectedDate(october.atDay(12))

        val items = state.selectedDay!!.items
        assertEquals(listOf(reading, sport), items.map { it.habit })
        assertEquals(listOf(true, false), items.map { it.isChecked })
        assertEquals(listOf(true, true), items.map { it.isEnabled })
    }

    @Test
    fun `habit is disabled on days before its creation`() {
        val items = stateWith(emptyMap()).withSelectedDate(october.atDay(5)).selectedDay!!.items

        assertEquals(listOf(true, false), items.map { it.isEnabled })
        assertEquals(CellState.NOT_TRACKED, items[1].state)
    }

    @Test
    fun `today is selectable and pending`() {
        val day = stateWith(emptyMap()).withSelectedDate(today).selectedDay!!

        assertEquals(true, day.isToday)
        assertEquals(listOf(CellState.PENDING, CellState.PENDING), day.items.map { it.state })
        assertEquals(listOf(true, true), day.items.map { it.isEnabled })
    }

    @Test
    fun `date outside shown month selects nothing`() {
        assertNull(stateWith(emptyMap()).withSelectedDate(LocalDate.of(2026, 9, 30)).selectedDay)
        assertNull(stateWith(emptyMap()).withSelectedDate(null).selectedDay)
    }
}
