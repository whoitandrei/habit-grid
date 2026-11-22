package ru.nsu.habitgrid.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ru.nsu.habitgrid.data.ReminderSettings
import java.time.Clock
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context, private val clock: Clock) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // ON/OFF функция синхронизации с настройками приложения
    fun sync(settings: ReminderSettings) {
        if (settings.enabled) schedule(settings) else cancel()
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun isScheduled(): Boolean = pendingIntent(PendingIntent.FLAG_NO_CREATE) != null

    // запланировать уведомление точно по времени или в промежуток
    private fun schedule(settings: ReminderSettings) {
        val triggerAt = nextReminderTime(ZonedDateTime.now(clock), settings.time).toInstant().toEpochMilli()
        val intent = requireNotNull(pendingIntent(PendingIntent.FLAG_UPDATE_CURRENT))
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, INEXACT_WINDOW_MILLIS, intent)
        }
    }

    private fun cancel() {
        pendingIntent(PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 1
        val INEXACT_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(10)
    }
}
