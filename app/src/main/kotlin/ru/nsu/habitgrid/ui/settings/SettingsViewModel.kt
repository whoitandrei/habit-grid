package ru.nsu.habitgrid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.nsu.habitgrid.data.ReminderSettings
import ru.nsu.habitgrid.data.SettingsRepository
import ru.nsu.habitgrid.notifications.ReminderScheduler
import java.time.LocalTime

data class SettingsUiState(
    val reminder: ReminderSettings = ReminderSettings(),
    val isLoading: Boolean = true,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.reminder
        .map { SettingsUiState(reminder = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onReminderEnabledChange(enabled: Boolean) {
        viewModelScope.launch { reminderScheduler.sync(settingsRepository.setReminderEnabled(enabled)) }
    }

    fun onReminderTimeChange(time: LocalTime) {
        viewModelScope.launch { reminderScheduler.sync(settingsRepository.setReminderTime(time)) }
    }

    fun canScheduleExactAlarms(): Boolean = reminderScheduler.canScheduleExactAlarms()

    fun onResume() {
        viewModelScope.launch { reminderScheduler.sync(settingsRepository.reminder.first()) }
    }
}
