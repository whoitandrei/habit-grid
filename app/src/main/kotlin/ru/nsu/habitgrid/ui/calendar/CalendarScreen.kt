package ru.nsu.habitgrid.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.nsu.habitgrid.R
import ru.nsu.habitgrid.domain.CellState
import ru.nsu.habitgrid.domain.Habit
import ru.nsu.habitgrid.ui.components.MonthHeader
import ru.nsu.habitgrid.ui.components.RussianLocale
import ru.nsu.habitgrid.ui.components.SlotIndicator
import ru.nsu.habitgrid.ui.navigation.Screen
import ru.nsu.habitgrid.ui.theme.HabitDone
import ru.nsu.habitgrid.ui.theme.HabitGridTheme
import ru.nsu.habitgrid.ui.theme.HabitMissed
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val DayOfMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", RussianLocale)

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigate: (Screen) -> Unit,
    onOpenStats: (YearMonth) -> Unit,
    openTodayRequest: Long = 0L,
) {
    LaunchedEffect(openTodayRequest) { viewModel.onOpenTodayRequest(openTodayRequest) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarContent(
        state = state,
        onNavigate = onNavigate,
        onOpenStats = onOpenStats,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onTodayClick = viewModel::onTodayClick,
        onDayClick = viewModel::onDayClick,
    )
    state.selectedDay?.let { day ->
        DaySheet(
            day = day,
            onToggleHabit = viewModel::onToggleHabit,
            onDismiss = viewModel::onDismissDay,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarContent(
    state: CalendarUiState,
    onNavigate: (Screen) -> Unit,
    onOpenStats: (YearMonth) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Screen.Calendar.titleRes)) },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Habits) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(Screen.Habits.titleRes),
                        )
                    }
                    IconButton(onClick = { onOpenStats(state.month) }) {
                        Icon(
                            painterResource(R.drawable.ic_bar_chart),
                            contentDescription = stringResource(Screen.Stats.titleRes),
                        )
                    }
                    IconButton(onClick = { onNavigate(Screen.Settings) }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(Screen.Settings.titleRes),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
            ) {
                MonthHeader(
                    month = state.month,
                    isCurrentMonth = state.isCurrentMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onTodayClick = onTodayClick,
                )
                if (!state.isLoading && state.habits.isEmpty()) {
                    EmptyState(onAddHabits = { onNavigate(Screen.Habits) })
                } else {
                    WeekdayHeader()
                    state.weeks.forEach { week -> WeekRow(week, state.habits, onDayClick) }
                    Legend(state.habits)
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT_STANDALONE, RussianLocale)
                    .replaceFirstChar { it.uppercase(RussianLocale) },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun WeekRow(week: List<CalendarDay?>, habits: List<Habit>, onDayClick: (LocalDate) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        week.forEach { day ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.8f)
                    .padding(2.dp),
            ) {
                if (day != null) DayCell(day, habits, onClick = { onDayClick(day.date) })
            }
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, habits: List<Habit>, onClick: () -> Unit) {
    val description = dayDescription(day, habits)
    val openLabel = stringResource(R.string.a11y_open_day)
    val shape = RoundedCornerShape(6.dp)
    val border = if (day.isToday) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (day.isFuture) disabled() else onClick(label = openLabel) { onClick(); true }
            }
            .clickable(enabled = !day.isFuture, onClick = onClick)
            .then(border)
            .padding(3.dp),
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(2) { row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    repeat(2) { column ->
                        HabitCell(
                            state = day.cells[row * 2 + column],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun dayDescription(day: CalendarDay, habits: List<Habit>): String {
    val date = day.date.format(DayOfMonthFormatter)
    val title = if (day.isToday) "$date, ${stringResource(R.string.a11y_today)}" else date
    val states = habits.map { habit ->
        stringResource(R.string.a11y_habit_state, habit.name, stringResource(cellStateText(day.cells[habit.slot])))
    }
    return (listOf(title) + states).joinToString(". ")
}

private fun cellStateText(state: CellState): Int = when (state) {
    CellState.DONE -> R.string.a11y_state_done
    CellState.MISSED -> R.string.a11y_state_missed
    CellState.PENDING -> R.string.a11y_state_pending
    CellState.FUTURE -> R.string.a11y_state_future
    CellState.NOT_TRACKED, CellState.EMPTY_SLOT -> R.string.a11y_state_not_tracked
}

@Composable
private fun HabitCell(state: CellState, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(2.dp)
    val colors = MaterialTheme.colorScheme
    val cellModifier = when (state) {
        CellState.DONE -> modifier.background(HabitDone, shape)
        CellState.MISSED -> modifier.background(HabitMissed, shape)
        CellState.PENDING -> modifier.border(1.dp, colors.outline, shape)
        CellState.FUTURE -> modifier.border(1.dp, colors.outlineVariant, shape)
        CellState.NOT_TRACKED -> modifier.background(colors.surfaceVariant, shape)
        CellState.EMPTY_SLOT -> modifier
    }
    Box(cellModifier)
}

@Composable
private fun Legend(habits: List<Habit>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        habits.forEach { habit ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SlotIndicator(habit.slot, cellSize = 10.dp)
                Spacer(Modifier.size(12.dp))
                Text(habit.name, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LegendColor(HabitDone, stringResource(R.string.legend_done))
            LegendColor(HabitMissed, stringResource(R.string.legend_missed))
            LegendColor(null, stringResource(R.string.legend_pending))
        }
    }
}

@Composable
private fun LegendColor(color: Color?, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val shape = RoundedCornerShape(2.dp)
        val square = Modifier.size(12.dp)
        Box(
            if (color != null) {
                square.background(color, shape)
            } else {
                square.border(1.dp, MaterialTheme.colorScheme.outline, shape)
            },
        )
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EmptyState(onAddHabits: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.calendar_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAddHabits) {
            Text(stringResource(Screen.Habits.titleRes))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarContentPreview() {
    val today = LocalDate.of(2026, 10, 15)
    val month = YearMonth.from(today)
    val habits = listOf(
        Habit(1, "Чтение", 0, LocalDate.of(2026, 9, 1)),
        Habit(2, "Спорт", 1, LocalDate.of(2026, 10, 5)),
        Habit(3, "Вода", 2, LocalDate.of(2026, 9, 1)),
    )
    val completions = mapOf(
        1L to (1..14 step 2).map(month::atDay).toSet(),
        2L to (5..12).map(month::atDay).toSet(),
        3L to (1..15).map(month::atDay).toSet(),
    )
    HabitGridTheme {
        CalendarContent(
            state = CalendarUiState(
                month = month,
                today = today,
                habits = habits,
                weeks = calendarWeeks(month, today, habits, completions),
                isLoading = false,
            ),
            onNavigate = {},
            onOpenStats = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onTodayClick = {},
            onDayClick = {},
        )
    }
}
