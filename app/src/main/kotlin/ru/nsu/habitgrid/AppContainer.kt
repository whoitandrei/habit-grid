package ru.nsu.habitgrid

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.nsu.habitgrid.data.HabitDatabase
import ru.nsu.habitgrid.data.HabitRepository
import ru.nsu.habitgrid.data.SettingsRepository
import ru.nsu.habitgrid.notifications.ReminderScheduler
import ru.nsu.habitgrid.time.SystemZoneClock
import ru.nsu.habitgrid.time.TodayProvider
import java.time.Clock

// создает в нужном порядке все объекты
class AppContainer(context: Context) {
    val clock: Clock = SystemZoneClock
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val todayProvider = TodayProvider(clock, applicationScope)
    private val database = HabitDatabase.create(context)
    val habitRepository = HabitRepository(database, clock)
    val settingsRepository = SettingsRepository(
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") },
    )
    val reminderScheduler = ReminderScheduler(context, clock)
}
