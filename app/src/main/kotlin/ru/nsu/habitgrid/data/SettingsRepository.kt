package ru.nsu.habitgrid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime

data class ReminderSettings(
    val enabled: Boolean = false,
    val time: LocalTime = DEFAULT_REMINDER_TIME,
)

val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(21, 0)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val reminder: Flow<ReminderSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toReminderSettings() }

    suspend fun setReminderEnabled(enabled: Boolean): ReminderSettings =
        dataStore.edit { it[REMINDER_ENABLED] = enabled }.toReminderSettings()

    suspend fun setReminderTime(time: LocalTime): ReminderSettings =
        dataStore.edit { it[REMINDER_MINUTES] = time.hour * 60 + time.minute }.toReminderSettings()

    private fun Preferences.toReminderSettings() = ReminderSettings(
        enabled = this[REMINDER_ENABLED] ?: false,
        time = this[REMINDER_MINUTES]?.let { LocalTime.of(it / 60, it % 60) } ?: DEFAULT_REMINDER_TIME,
    )

    private companion object {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
    }
}
