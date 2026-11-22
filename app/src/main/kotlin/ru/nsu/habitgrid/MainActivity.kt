package ru.nsu.habitgrid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableLongStateOf
import ru.nsu.habitgrid.ui.navigation.HabitGridNavHost
import ru.nsu.habitgrid.ui.theme.HabitGridTheme

class MainActivity : ComponentActivity() {

    // на каком дне открыть
    private val openTodayRequest = mutableLongStateOf(0L)

    // что делать при создании активности
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // если активность восстанавливается после пересоздания то эту проверку надо опустить так как есть состояния
        if (savedInstanceState == null) handleOpenToday(intent)
        setContent {
            HabitGridTheme {
                HabitGridNavHost(openTodayRequest = openTodayRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenToday(intent)
    }

    override fun onResume() {
        super.onResume()
        container().todayProvider.refresh()
    }

    private fun handleOpenToday(intent: Intent) {
        if (!intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) return
        intent.removeExtra(EXTRA_OPEN_TODAY)
        container().todayProvider.refresh()
        openTodayRequest.longValue = SystemClock.elapsedRealtimeNanos()
    }

    private fun container() = (application as HabitGridApplication).container

    companion object {
        const val EXTRA_OPEN_TODAY = "ru.nsu.habitgrid.extra.OPEN_TODAY"

        fun openTodayIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_TODAY, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
