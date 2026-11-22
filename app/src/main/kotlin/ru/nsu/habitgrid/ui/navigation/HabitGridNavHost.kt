package ru.nsu.habitgrid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.nsu.habitgrid.AppContainer
import ru.nsu.habitgrid.HabitGridApplication
import ru.nsu.habitgrid.ui.calendar.CalendarScreen
import ru.nsu.habitgrid.ui.calendar.CalendarViewModel
import ru.nsu.habitgrid.ui.habits.HabitsScreen
import ru.nsu.habitgrid.ui.habits.HabitsViewModel
import ru.nsu.habitgrid.ui.settings.SettingsScreen
import ru.nsu.habitgrid.ui.settings.SettingsViewModel
import ru.nsu.habitgrid.ui.stats.StatsScreen
import ru.nsu.habitgrid.ui.stats.StatsViewModel

private const val STATS_MONTH_ARG = StatsViewModel.MONTH_KEY

@Composable
fun HabitGridNavHost(
    openTodayRequest: State<Long>,
    navController: NavHostController = rememberNavController(),
) {
    val request = openTodayRequest.value
    LaunchedEffect(request) {
        if (request != 0L) navController.popBackStack(Screen.Calendar.route, inclusive = false)
    }
    NavHost(navController = navController, startDestination = Screen.Calendar.route) {
        composable(Screen.Calendar.route) {
            val container = appContainer()
            CalendarScreen(
                viewModel = viewModel {
                    CalendarViewModel(createSavedStateHandle(), container.habitRepository, container.todayProvider.today)
                },
                onNavigate = { screen ->
                    navController.navigate(screen.route) { launchSingleTop = true }
                },
                openTodayRequest = openTodayRequest.value,
                onOpenStats = { month ->
                    navController.navigate("${Screen.Stats.route}?$STATS_MONTH_ARG=$month") { launchSingleTop = true }
                },
            )
        }
        composable(
            route = "${Screen.Stats.route}?$STATS_MONTH_ARG={$STATS_MONTH_ARG}",
            arguments = listOf(
                navArgument(STATS_MONTH_ARG) {
                    type = NavType.StringType
                    nullable = true
                },
            ),
        ) {
            val container = appContainer()
            StatsScreen(
                viewModel = viewModel {
                    StatsViewModel(createSavedStateHandle(), container.habitRepository, container.todayProvider.today)
                },
                onBack = { navController.navigateUp() },
            )
        }
        composable(Screen.Habits.route) {
            val container = appContainer()
            HabitsScreen(
                viewModel = viewModel { HabitsViewModel(createSavedStateHandle(), container.habitRepository) },
                onBack = { navController.navigateUp() },
            )
        }
        composable(Screen.Settings.route) {
            val container = appContainer()
            SettingsScreen(
                viewModel = viewModel { SettingsViewModel(container.settingsRepository, container.reminderScheduler) },
                onBack = { navController.navigateUp() },
            )
        }
    }
}

@Composable
private fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as HabitGridApplication).container
