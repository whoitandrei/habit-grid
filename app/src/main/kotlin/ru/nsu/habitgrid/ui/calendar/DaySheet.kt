package ru.nsu.habitgrid.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.nsu.habitgrid.R
import ru.nsu.habitgrid.ui.components.RussianLocale
import ru.nsu.habitgrid.ui.components.SlotIndicator
import java.time.format.DateTimeFormatter

private val DayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", RussianLocale)
private val DayOfMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", RussianLocale)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySheet(day: SelectedDay, onToggleHabit: (Long) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = if (day.isToday) {
                    stringResource(R.string.day_sheet_today, day.date.format(DayOfMonthFormatter))
                } else {
                    day.date.format(DayFormatter).replaceFirstChar { it.uppercase(RussianLocale) }
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            day.items.forEach { item ->
                DayHabitRow(item = item, onToggle = { onToggleHabit(item.habit.id) })
            }
        }
    }
}

@Composable
private fun DayHabitRow(item: DayHabitItem, onToggle: () -> Unit) {
    val contentAlpha = if (item.isEnabled) 1f else 0.38f
    ListItem(
        headlineContent = { Text(item.habit.name, modifier = Modifier.alpha(contentAlpha)) },
        supportingContent = if (item.isEnabled) {
            null
        } else {
            { Text(stringResource(R.string.day_sheet_not_tracked)) }
        },
        leadingContent = { SlotIndicator(item.habit.slot, modifier = Modifier.alpha(contentAlpha)) },
        trailingContent = {
            Checkbox(checked = item.isChecked, onCheckedChange = null, enabled = item.isEnabled)
        },
        modifier = Modifier.toggleable(
            value = item.isChecked,
            enabled = item.isEnabled,
            role = Role.Checkbox,
            onValueChange = { onToggle() },
        ),
    )
}
