package ru.nsu.habitgrid

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import ru.nsu.habitgrid.notifications.ReminderNotifications

// наследний Application - android может вызывать методы класса на протяжении его жизни
class HabitGridApplication : Application() {
    // значение будет присвоено позже, читать можно везде - изменять только внутри HabitGridApplication
    lateinit var container: AppContainer
        private set

    // получатель системных уведомлений
    private val timeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            container.todayProvider.refresh()
        }
    }

    // переопределяет Application.onCreate() - то что андроид вызовет при инициализации процесса
    override fun onCreate() {
        // делегирующий - вызывает метод родителя
        super.onCreate()
        container = AppContainer(this)
        // канал для уведомлений
        ReminderNotifications.createChannel(this)
        // фильтр системных событий - нужно для отправиления напоминаний
        val timeChanges = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        // получатель для процесса
        // этот класс, timeChangeReceiver = получатель, получать изменения времени
        ContextCompat.registerReceiver(this, timeChangeReceiver, timeChanges, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}
