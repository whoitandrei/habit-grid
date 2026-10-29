package ru.nsu.habitgrid.domain

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

data class HabitStats(val habit: Habit, val done: Int, val tracked: Int) {
    val percent: Int? get() = percentOf(done, tracked)
}

data class MonthStats(val habits: List<HabitStats>) {
    val done: Int get() = habits.sumOf { it.done }
    val tracked: Int get() = habits.sumOf { it.tracked }
    val percent: Int? get() = percentOf(done, tracked)
}

fun monthStats(
    month: YearMonth,
    today: LocalDate,
    habits: List<Habit>,
    completions: Map<Long, Set<LocalDate>>,
): MonthStats {
    val days = (1..month.lengthOfMonth()).map(month::atDay)
    return MonthStats(
        habits.map { habit ->
            val doneDates = completions[habit.id].orEmpty()
            val states = days.map { cellState(habit, it, today, it in doneDates) }
            HabitStats(
                habit = habit,
                done = states.count { it == CellState.DONE },
                tracked = states.count { it == CellState.DONE || it == CellState.MISSED },
            )
        },
    )
}

private fun percentOf(part: Int, whole: Int): Int? =
    if (whole == 0) null else (part * 100.0 / whole).roundToInt()
