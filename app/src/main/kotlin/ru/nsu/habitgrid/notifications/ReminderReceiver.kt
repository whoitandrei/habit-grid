package ru.nsu.habitgrid.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.nsu.habitgrid.HabitGridApplication

// работает по будильнику, проверяет action, читает настройки и ставит будильник на завтра
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val container = (context.applicationContext as HabitGridApplication).container
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                val settings = container.settingsRepository.reminder.first()
                if (settings.enabled) ReminderNotifications.show(context)
                container.reminderScheduler.sync(settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "ru.nsu.habitgrid.action.REMIND"
    }
}
