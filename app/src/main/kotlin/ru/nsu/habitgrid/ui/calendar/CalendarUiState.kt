package ru.nsu.habitgrid.ui.calendar

import ru.nsu.habitgrid.domain.CellState
import ru.nsu.habitgrid.domain.Habit
import ru.nsu.habitgrid.domain.cellState
import ru.nsu.habitgrid.domain.monthWeeks
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth,
    val today: LocalDate,
    val habits: List<Habit> = emptyList(),
    val weeks: List<List<CalendarDay?>> = emptyList(),
    val selectedDay: SelectedDay? = null,
    val isLoading: Boolean = true,
) {
    val isCurrentMonth: Boolean get() = month == YearMonth.from(today)

    fun withSelectedDate(date: LocalDate?): CalendarUiState {
        val day = date?.let { selected -> weeks.flatten().firstOrNull { it?.date == selected } }
            ?: return copy(selectedDay = null)
        return copy(
            selectedDay = SelectedDay(
                date = day.date,
                isToday = day.isToday,
                items = habits.map { DayHabitItem(it, day.cells[it.slot]) },
            ),
        )
    }
}

data class CalendarDay(
    val date: LocalDate,
    val cells: List<CellState>,
    val isToday: Boolean,
    val isFuture: Boolean,
)

data class SelectedDay(
    val date: LocalDate,
    val isToday: Boolean,
    val items: List<DayHabitItem>,
)

data class DayHabitItem(val habit: Habit, val state: CellState) {
    val isChecked: Boolean get() = state == CellState.DONE
    val isEnabled: Boolean
        get() = state == CellState.DONE || state == CellState.MISSED || state == CellState.PENDING
}

fun calendarWeeks(
    month: YearMonth,
    today: LocalDate,
    habits: List<Habit>,
    completions: Map<Long, Set<LocalDate>>,
): List<List<CalendarDay?>> {
    val habitsBySlot = habits.associateBy { it.slot }
    return monthWeeks(month).map { week ->
        week.map { date ->
            date?.let {
                CalendarDay(
                    date = it,
                    cells = List(Habit.MAX_COUNT) { slot ->
                        val habit = habitsBySlot[slot]
                        val isDone = habit != null && it in completions[habit.id].orEmpty()
                        cellState(habit, it, today, isDone)
                    },
                    isToday = it == today,
                    isFuture = it > today,
                )
            }
        }
    }
}
