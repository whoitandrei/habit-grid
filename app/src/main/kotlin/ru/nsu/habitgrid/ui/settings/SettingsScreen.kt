package ru.nsu.habitgrid.ui.settings

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.nsu.habitgrid.R
import ru.nsu.habitgrid.data.ReminderSettings
import ru.nsu.habitgrid.notifications.ReminderNotifications
import ru.nsu.habitgrid.ui.theme.HabitGridTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(ReminderNotifications.canPost(context)) }
    var exactAlarmsAllowed by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        notificationsAllowed = ReminderNotifications.canPost(context)
        exactAlarmsAllowed = viewModel.canScheduleExactAlarms()
        viewModel.onResume()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = ReminderNotifications.canPost(context)
        permissionDenied = !granted
        if (granted) viewModel.onReminderEnabledChange(true)
    }

    SettingsContent(
        state = state,
        showNotificationsBlocked = !notificationsAllowed && (state.reminder.enabled || permissionDenied),
        showInexactAlarmHint = state.reminder.enabled && !exactAlarmsAllowed,
        onBack = onBack,
        onReminderEnabledChange = { enabled ->
            val needsPermission = enabled &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !ReminderNotifications.canPost(context)
            if (needsPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onReminderEnabledChange(enabled)
            }
        },
        onReminderTimeChange = viewModel::onReminderTimeChange,
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
        onAllowExactAlarms = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    showNotificationsBlocked: Boolean,
    showInexactAlarmHint: Boolean,
    onBack: () -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onAllowExactAlarms: () -> Unit,
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val reminder = state.reminder
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings)) },
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reminder)) },
                supportingContent = { Text(stringResource(R.string.settings_reminder_description)) },
                trailingContent = { Switch(checked = reminder.enabled, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    value = reminder.enabled,
                    enabled = !state.isLoading,
                    role = Role.Switch,
                    onValueChange = onReminderEnabledChange,
                ),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_time)) },
                supportingContent = { Text(reminder.time.format(TimeFormatter)) },
                modifier = Modifier
                    .clickable(enabled = reminder.enabled, role = Role.Button) { showTimePicker = true }
                    .alpha(if (reminder.enabled) 1f else 0.38f),
            )
            if (showNotificationsBlocked) {
                HintCard(
                    text = stringResource(R.string.settings_notifications_denied),
                    action = stringResource(R.string.settings_open_system_settings),
                    onAction = onOpenNotificationSettings,
                )
            }
            if (showInexactAlarmHint) {
                HintCard(
                    text = stringResource(R.string.settings_inexact_alarm),
                    action = stringResource(R.string.settings_allow_exact_alarm),
                    onAction = onAllowExactAlarms,
                )
            }
        }
    }
    if (showTimePicker) {
        ReminderTimeDialog(
            initialTime = reminder.time,
            onConfirm = { time ->
                showTimePicker = false
                onReminderTimeChange(time)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun HintCard(text: String, action: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) { Text(action) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_time_pick)) },
        text = {
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                TimeInput(state = pickerState)
            } else {
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    HabitGridTheme {
        SettingsContent(
            state = SettingsUiState(reminder = ReminderSettings(enabled = true), isLoading = false),
            showNotificationsBlocked = false,
            showInexactAlarmHint = true,
            onBack = {},
            onReminderEnabledChange = {},
            onReminderTimeChange = {},
            onOpenNotificationSettings = {},
            onAllowExactAlarms = {},
        )
    }
}
