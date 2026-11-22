package ru.nsu.habitgrid.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.nsu.habitgrid.R
import ru.nsu.habitgrid.domain.Habit
import ru.nsu.habitgrid.domain.HabitStats
import ru.nsu.habitgrid.domain.MonthStats
import ru.nsu.habitgrid.ui.components.MonthHeader
import ru.nsu.habitgrid.ui.components.SlotIndicator
import ru.nsu.habitgrid.ui.components.isRussianGenitiveSingular
import ru.nsu.habitgrid.ui.theme.HabitGridTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsContent(
        state = state,
        onBack = onBack,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onTodayClick = viewModel::onTodayClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsContent(
    state: StatsUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_stats)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MonthHeader(
                    month = state.month,
                    isCurrentMonth = state.isCurrentMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onTodayClick = onTodayClick,
                )
                if (!state.isLoading && state.stats.habits.isEmpty()) {
                    Text(
                        text = stringResource(R.string.stats_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    )
                } else if (!state.isLoading) {
                    TotalCard(state.stats)
                    state.stats.habits.forEach { HabitStatsCard(it) }
                }
            }
        }
    }
}

@Composable
private fun TotalCard(stats: MonthStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.stats_total), style = MaterialTheme.typography.titleMedium)
            Text(
                text = percentText(stats.percent),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = daysText(stats.done, stats.tracked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HabitStatsCard(stats: HabitStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SlotIndicator(stats.habit.slot, cellSize = 10.dp)
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stats.habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(text = percentText(stats.percent), style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = { if (stats.tracked == 0) 0f else stats.done.toFloat() / stats.tracked },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = daysText(stats.done, stats.tracked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun percentText(percent: Int?): String =
    if (percent == null) stringResource(R.string.stats_no_data) else stringResource(R.string.stats_percent, percent)

@Composable
private fun daysText(done: Int, tracked: Int): String =
    if (tracked == 0) {
        stringResource(R.string.stats_no_tracked_days)
    } else {
        stringResource(
            if (isRussianGenitiveSingular(tracked)) R.string.stats_days_one else R.string.stats_days_many,
            done,
            tracked,
        )
    }

@Preview(showBackground = true)
@Composable
private fun StatsContentPreview() {
    val today = LocalDate.of(2026, 10, 15)
    val reading = Habit(1, "Чтение", 0, LocalDate.of(2026, 9, 1))
    val sport = Habit(2, "Спорт", 1, LocalDate.of(2026, 10, 10))
    HabitGridTheme {
        StatsContent(
            state = StatsUiState(
                month = YearMonth.from(today),
                today = today,
                stats = MonthStats(listOf(HabitStats(reading, 10, 14), HabitStats(sport, 2, 5))),
                isLoading = false,
            ),
            onBack = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onTodayClick = {},
        )
    }
}
