package com.revsystems.countdowntoevent.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.settings.AppSettings
import com.revsystems.countdowntoevent.domain.settings.ReminderOffset
import com.revsystems.countdowntoevent.domain.settings.ThemeMode
import com.revsystems.countdowntoevent.ui.permissions.NotificationAccess
import com.revsystems.countdowntoevent.ui.permissions.rememberNotificationAccess
import com.revsystems.countdowntoevent.widget.EventWidgetReceiver
import com.revsystems.countdowntoevent.widget.UpcomingWidgetReceiver
import java.time.LocalDate

@StringRes
fun ReminderOffset.labelRes(): Int =
    when (this) {
        ReminderOffset.NONE -> R.string.reminder_none
        ReminderOffset.MINUTES_15 -> R.string.reminder_15_min
        ReminderOffset.HOUR_1 -> R.string.reminder_1_hour
        ReminderOffset.DAY_1 -> R.string.reminder_1_day
        ReminderOffset.DAY_2 -> R.string.reminder_2_days
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val access =
        rememberNotificationAccess(
            wasAskedBefore = viewModel::notificationPermissionAsked,
            onAsked = viewModel::markNotificationPermissionAsked,
        )

    val messageText = backupMessage?.let { backupMessageText(it) }
    LaunchedEffect(backupMessage) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText, duration = SnackbarDuration.Long)
            viewModel.backupMessageShown()
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            settings?.let { current ->
                RemindersSection(
                    settings = current,
                    access = access,
                    onOffsetChange = viewModel::setReminderOffset,
                    onNotifyOnDayChange = viewModel::setNotifyOnEventDay,
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AppearanceSection(current.themeMode, onThemeChange = viewModel::setThemeMode)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            BackupSection(busy = backupBusy, onExport = viewModel::export, onImport = viewModel::import)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            WidgetsSection()
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            AboutSection()
        }
    }
}

@Composable
private fun backupMessageText(message: BackupMessage): String =
    when (message) {
        is BackupMessage.Exported -> pluralStringResource(R.plurals.backup_exported, message.count, message.count)
        is BackupMessage.Imported ->
            stringResource(R.string.backup_imported, message.result.imported, message.result.duplicates, message.result.invalid)
        BackupMessage.NotABackup -> stringResource(R.string.backup_not_a_backup)
        BackupMessage.Failed -> stringResource(R.string.backup_failed)
    }

@StringRes
private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val context = LocalContext.current
    var pickingTheme by rememberSaveable { mutableStateOf(false) }

    SectionTitle(stringResource(R.string.settings_appearance))
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_theme)) },
        supportingContent = { Text(stringResource(themeMode.labelRes())) },
        modifier = Modifier.clickable { pickingTheme = true },
    )
    // Android 13+ has a per-app language screen (fed by the generated locale config).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val current = LocalConfiguration.current.locales[0]
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_language)) },
            supportingContent = { Text(current.getDisplayLanguage(current).replaceFirstChar { it.titlecase(current) }) },
            modifier =
                Modifier.clickable {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, "package:${context.packageName}".toUri()),
                    )
                },
        )
    }

    if (pickingTheme) {
        ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = themeMode,
            label = { stringResource(it.labelRes()) },
            onSelect = {
                onThemeChange(it)
                pickingTheme = false
            },
            onDismiss = { pickingTheme = false },
        )
    }
}

@Composable
private fun BackupSection(
    busy: Boolean,
    onExport: (String) -> Unit,
    onImport: (String) -> Unit,
) {
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { onExport(it.toString()) }
        }
    // Some file managers label .json files as text or binary.
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onImport(it.toString()) }
        }
    val fileName = "countdown-to-event-${LocalDate.now()}.json"

    SectionTitle(stringResource(R.string.settings_backup))
    ListItem(
        headlineContent = { Text(stringResource(R.string.backup_export)) },
        supportingContent = { Text(stringResource(R.string.backup_export_desc)) },
        trailingContent = { if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) },
        modifier = Modifier.clickable(enabled = !busy) { exportLauncher.launch(fileName) },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.backup_import)) },
        supportingContent = { Text(stringResource(R.string.backup_import_desc)) },
        modifier =
            Modifier.clickable(enabled = !busy) {
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            },
    )
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val version =
        remember {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                .orEmpty()
        }
    SectionTitle(stringResource(R.string.settings_about))
    ListItem(
        headlineContent = { Text(stringResource(R.string.app_name)) },
        supportingContent = { Text(stringResource(R.string.settings_version, version)) },
    )
}

@Composable
private fun RemindersSection(
    settings: AppSettings,
    access: NotificationAccess,
    onOffsetChange: (ReminderOffset) -> Unit,
    onNotifyOnDayChange: (Boolean) -> Unit,
) {
    var pickingOffset by rememberSaveable { mutableStateOf(false) }

    SectionTitle(stringResource(R.string.settings_reminders))
    if (!access.notificationsAllowed) {
        WarningRow(
            title = stringResource(R.string.settings_notifications_off),
            body = stringResource(R.string.settings_notifications_off_desc),
            action = stringResource(R.string.action_enable),
            onAction = access.requestNotifications,
        )
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_reminder)) },
        supportingContent = { Text(stringResource(settings.reminderOffset.labelRes())) },
        modifier = Modifier.clickable { pickingOffset = true },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_notify_on_day)) },
        supportingContent = { Text(stringResource(R.string.settings_notify_on_day_desc)) },
        trailingContent = { Switch(checked = settings.notifyOnEventDay, onCheckedChange = null) },
        modifier =
            Modifier.toggleable(value = settings.notifyOnEventDay, role = Role.Switch, onValueChange = onNotifyOnDayChange),
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !access.exactAlarmsAllowed && settings.anyNotificationEnabled) {
        WarningRow(
            title = stringResource(R.string.settings_exact_alarms),
            body = stringResource(R.string.settings_exact_alarms_desc),
            action = stringResource(R.string.action_allow),
            onAction = access.openExactAlarmSettings,
        )
    }

    if (pickingOffset) {
        ChoiceDialog(
            title = stringResource(R.string.settings_reminder),
            options = ReminderOffset.entries,
            selected = settings.reminderOffset,
            label = { stringResource(it.labelRes()) },
            onSelect = {
                onOffsetChange(it)
                pickingOffset = false
            },
            onDismiss = { pickingOffset = false },
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = option == selected, role = Role.RadioButton) { onSelect(option) }
                                .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun WarningRow(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(body) },
        trailingContent = {
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            ) { Text(action, fontWeight = FontWeight.Bold) }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun WidgetsSection() {
    val context = LocalContext.current
    val widgetManager = remember { AppWidgetManager.getInstance(context) }
    val canPin = remember { widgetManager.isRequestPinAppWidgetSupported }

    fun pin(receiver: Class<*>) {
        widgetManager.requestPinAppWidget(ComponentName(context, receiver), null, null)
    }

    SectionTitle(stringResource(R.string.settings_widgets))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(if (canPin) R.string.settings_widgets_hint else R.string.settings_widgets_manual),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (canPin) {
            PinButton(Icons.Outlined.Widgets, stringResource(R.string.settings_add_event_widget)) {
                pin(EventWidgetReceiver::class.java)
            }
            PinButton(Icons.AutoMirrored.Outlined.ViewList, stringResource(R.string.settings_add_list_widget)) {
                pin(UpcomingWidgetReceiver::class.java)
            }
        }
    }
}

@Composable
private fun PinButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null)
        Text(label, modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
    }
}
