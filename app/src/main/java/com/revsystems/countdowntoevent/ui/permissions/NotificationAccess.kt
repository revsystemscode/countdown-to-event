package com.revsystems.countdowntoevent.ui.permissions

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch

/** Live state of the notification and exact-alarm permissions, refreshed when the screen resumes. */
class NotificationAccess(
    val notificationsAllowed: Boolean,
    /** Always true below Android 12, where exact alarms need no permission. */
    val exactAlarmsAllowed: Boolean,
    val requestNotifications: () -> Unit,
    val openExactAlarmSettings: () -> Unit,
)

/**
 * @param wasAskedBefore whether the system dialog was already shown once. Android won't show it
 *   again after a second refusal, so in that case we send the user to the app's notification settings.
 */
@Composable
fun rememberNotificationAccess(
    wasAskedBefore: suspend () -> Boolean,
    onAsked: suspend () -> Unit,
    onResult: (Boolean) -> Unit = {},
): NotificationAccess {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var notificationsAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
    var exactAllowed by remember { mutableStateOf(exactAlarmsAllowed(context)) }
    var askedBefore by remember { mutableStateOf<Boolean?>(null) }

    LifecycleResumeEffect(Unit) {
        notificationsAllowed = notificationsAllowed(context)
        exactAllowed = exactAlarmsAllowed(context)
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { askedBefore = wasAskedBefore() }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsAllowed = granted && notificationsAllowed(context)
            onResult(granted)
        }
    val scope = rememberCoroutineScope()

    return NotificationAccess(
        notificationsAllowed = notificationsAllowed,
        exactAlarmsAllowed = exactAllowed,
        requestNotifications = {
            val canShowDialog =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !permissionGranted(context) &&
                    (askedBefore == false || activity?.let(::shouldShowRationale) == true)
            if (canShowDialog) {
                scope.launch {
                    onAsked()
                    askedBefore = true
                }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        },
        openExactAlarmSettings = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
                )
            }
        },
    )
}

private fun permissionGranted(context: Context) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun notificationsAllowed(context: Context) =
    permissionGranted(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun exactAlarmsAllowed(context: Context) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

private fun shouldShowRationale(activity: Activity) =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
