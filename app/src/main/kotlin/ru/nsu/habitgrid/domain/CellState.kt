package ru.nsu.habitgrid.domain

import java.time.LocalDate

enum class CellState {
    EMPTY_SLOT,
    NOT_TRACKED,
    FUTURE,
    DONE,
    PENDING,
    MISSED,
}

fun cellState(habit: Habit?, date: LocalDate, today: LocalDate, isDone: Boolean): CellState = when {
    habit == null -> CellState.EMPTY_SLOT
    date < habit.createdDate -> CellState.NOT_TRACKED
    date > today -> CellState.FUTURE
    isDone -> CellState.DONE
    date == today -> CellState.PENDING
    else -> CellState.MISSED
}
