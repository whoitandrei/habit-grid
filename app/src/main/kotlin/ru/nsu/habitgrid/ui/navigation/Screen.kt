package ru.nsu.habitgrid.ui.navigation

import androidx.annotation.StringRes
import ru.nsu.habitgrid.R

enum class Screen(val route: String, @param:StringRes val titleRes: Int) {
    Calendar("calendar", R.string.screen_calendar),
    Habits("habits", R.string.screen_habits),
    Stats("stats", R.string.screen_stats),
    Settings("settings", R.string.screen_settings),
}
