package ru.nsu.habitgrid.ui.habits

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.nsu.habitgrid.data.HabitRepository
import ru.nsu.habitgrid.domain.Habit
import ru.nsu.habitgrid.domain.HabitNameValidation
import ru.nsu.habitgrid.domain.HabitNameValidator

class HabitsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository,
) : ViewModel() {

    private var dialog: HabitDialog?
        get() = savedStateHandle.get<Bundle>(DIALOG_KEY)?.toDialog()
        set(value) {
            savedStateHandle[DIALOG_KEY] = value?.toBundle()
        }

    val uiState: StateFlow<HabitsUiState> =
        combine(repository.habits, savedStateHandle.getStateFlow<Bundle?>(DIALOG_KEY, null)) { habits, dialog ->
            HabitsUiState(habits = habits, dialog = dialog?.toDialog(), isLoading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    fun onAddClick() {
        if (uiState.value.canAdd) dialog = HabitDialog.Edit(habitId = null, name = "")
    }

    fun onRenameClick(habit: Habit) {
        dialog = HabitDialog.Edit(habitId = habit.id, name = habit.name)
    }

    fun onDeleteClick(habit: Habit) {
        dialog = HabitDialog.ConfirmDelete(habitId = habit.id, name = habit.name)
    }

    fun onNameChange(name: String) {
        val current = dialog
        if (current is HabitDialog.Edit) dialog = current.copy(name = name, error = null)
    }

    fun onDismissDialog() {
        dialog = null
    }

    fun onConfirmEdit(name: String) {
        val edit = dialog as? HabitDialog.Edit ?: return
        when (val validation = HabitNameValidator.validate(name)) {
            is HabitNameValidation.Error -> dialog = edit.copy(name = name, error = validation)
            is HabitNameValidation.Valid -> {
                dialog = null
                viewModelScope.launch {
                    if (edit.habitId == null) {
                        repository.addHabit(validation.name)
                    } else {
                        repository.renameHabit(edit.habitId, validation.name)
                    }
                }
            }
        }
    }

    fun onConfirmDelete() {
        val delete = dialog as? HabitDialog.ConfirmDelete ?: return
        dialog = null
        viewModelScope.launch { repository.deleteHabit(delete.habitId) }
    }

    private fun HabitDialog.toBundle(): Bundle = Bundle().apply {
        when (val dialog = this@toBundle) {
            is HabitDialog.Edit -> {
                putString(KIND, KIND_EDIT)
                putLong(HABIT_ID, dialog.habitId ?: NEW_HABIT_ID)
                putString(NAME, dialog.name)
                putBoolean(SHOW_ERROR, dialog.error != null)
            }
            is HabitDialog.ConfirmDelete -> {
                putString(KIND, KIND_DELETE)
                putLong(HABIT_ID, dialog.habitId)
                putString(NAME, dialog.name)
            }
        }
    }

    private fun Bundle.toDialog(): HabitDialog? {
        val habitId = getLong(HABIT_ID)
        val name = getString(NAME).orEmpty()
        return when (getString(KIND)) {
            KIND_EDIT -> HabitDialog.Edit(
                habitId = habitId.takeIf { it != NEW_HABIT_ID },
                name = name,
                error = if (getBoolean(SHOW_ERROR)) HabitNameValidator.validate(name) as? HabitNameValidation.Error else null,
            )
            KIND_DELETE -> HabitDialog.ConfirmDelete(habitId = habitId, name = name)
            else -> null
        }
    }

    private companion object {
        const val DIALOG_KEY = "dialog"
        const val KIND = "kind"
        const val KIND_EDIT = "edit"
        const val KIND_DELETE = "delete"
        const val HABIT_ID = "habitId"
        const val NAME = "name"
        const val SHOW_ERROR = "showError"
        const val NEW_HABIT_ID = -1L
    }
}
