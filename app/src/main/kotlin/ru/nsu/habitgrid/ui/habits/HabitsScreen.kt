package ru.nsu.habitgrid.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import ru.nsu.habitgrid.ui.components.SlotIndicator
import ru.nsu.habitgrid.ui.theme.HabitGridTheme
import java.time.LocalDate

@Composable
fun HabitsScreen(viewModel: HabitsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HabitsContent(
        state = state,
        onBack = onBack,
        onAddClick = viewModel::onAddClick,
        onRenameClick = viewModel::onRenameClick,
        onDeleteClick = viewModel::onDeleteClick,
    )
    when (val dialog = state.dialog) {
        is HabitDialog.Edit -> HabitNameDialog(
            dialog = dialog,
            onNameChange = viewModel::onNameChange,
            onConfirm = viewModel::onConfirmEdit,
            onDismiss = viewModel::onDismissDialog,
        )
        is HabitDialog.ConfirmDelete -> DeleteHabitDialog(
            name = dialog.name,
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismissDialog,
        )
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsContent(
    state: HabitsUiState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onRenameClick: (Habit) -> Unit,
    onDeleteClick: (Habit) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_habits)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (!state.isLoading && state.habits.isEmpty()) {
                    Text(
                        text = stringResource(R.string.habits_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                } else {
                    LazyColumn {
                        items(state.habits, key = { it.id }) { habit ->
                            HabitRow(
                                habit = habit,
                                onRenameClick = { onRenameClick(habit) },
                                onDeleteClick = { onDeleteClick(habit) },
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (state.canAdd || state.isLoading) {
                        stringResource(R.string.habits_counter, state.habits.size, Habit.MAX_COUNT)
                    } else {
                        stringResource(R.string.habits_limit_reached, Habit.MAX_COUNT)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onAddClick,
                    enabled = state.canAdd,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.habits_add))
                }
            }
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, onRenameClick: () -> Unit, onDeleteClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(habit.name) },
        leadingContent = { SlotIndicator(habit.slot) },
        trailingContent = {
            Row {
                IconButton(onClick = onRenameClick) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.habit_rename_action, habit.name),
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.habit_delete_action, habit.name),
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun HabitsContentPreview() {
    val today = LocalDate.of(2026, 10, 15)
    HabitGridTheme {
        HabitsContent(
            state = HabitsUiState(
                habits = listOf(
                    Habit(1, "Чтение", 0, today),
                    Habit(2, "Спорт", 1, today),
                    Habit(3, "Вода", 3, today),
                ),
                isLoading = false,
            ),
            onBack = {},
            onAddClick = {},
            onRenameClick = {},
            onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HabitsContentEmptyPreview() {
    HabitGridTheme {
        HabitsContent(
            state = HabitsUiState(isLoading = false),
            onBack = {},
            onAddClick = {},
            onRenameClick = {},
            onDeleteClick = {},
        )
    }
}
