package ru.nsu.habitgrid.ui.habits

import ru.nsu.habitgrid.domain.Habit
import ru.nsu.habitgrid.domain.HabitNameValidation

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val dialog: HabitDialog? = null,
    val isLoading: Boolean = true,
) {
    val canAdd: Boolean get() = !isLoading && habits.size < Habit.MAX_COUNT
}

sealed interface HabitDialog {
    data class Edit(
        val habitId: Long?,
        val name: String,
        val error: HabitNameValidation.Error? = null,
    ) : HabitDialog

    data class ConfirmDelete(val habitId: Long, val name: String) : HabitDialog
}
