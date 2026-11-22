package ru.nsu.habitgrid.ui.habits

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import ru.nsu.habitgrid.R
import ru.nsu.habitgrid.domain.HabitNameValidation
import ru.nsu.habitgrid.domain.HabitNameValidator

@Composable
fun HabitNameDialog(
    dialog: HabitDialog.Edit,
    onNameChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var name by rememberSaveable { mutableStateOf(dialog.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (dialog.habitId == null) R.string.habit_add_title else R.string.habit_rename_title,
                ),
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onNameChange(it)
                },
                label = { Text(stringResource(R.string.habit_name_label)) },
                singleLine = true,
                isError = dialog.error != null,
                supportingText = {
                    Text(
                        when (dialog.error) {
                            HabitNameValidation.Empty -> stringResource(R.string.habit_name_error_empty)
                            HabitNameValidation.TooLong -> stringResource(
                                R.string.habit_name_error_too_long,
                                HabitNameValidator.MAX_LENGTH,
                            )
                            null -> stringResource(
                                R.string.habit_name_counter,
                                name.trim().length,
                                HabitNameValidator.MAX_LENGTH,
                            )
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm(name) }),
                modifier = Modifier.focusRequester(focusRequester),
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun DeleteHabitDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.habit_delete_title, name)) },
        text = { Text(stringResource(R.string.habit_delete_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
