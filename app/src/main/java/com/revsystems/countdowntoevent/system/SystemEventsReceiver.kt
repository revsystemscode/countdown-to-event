package com.revsystems.countdowntoevent.system

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.revsystems.countdowntoevent.di.appEntryPoint
import kotlinx.coroutines.launch

/**
 * System events that invalidate countdowns or alarms: reboot (alarms are lost), clock or zone
 * change, language change, app update, and exact-alarm access being granted.
 * Exported for the system; any other sender can at most trigger a harmless recalculation.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        val entryPoint = context.appEntryPoint()
        entryPoint.applicationScope().launch {
            try {
                val reminders = entryPoint.reminderScheduler()
                // After a manual clock change the "checked until" mark can't be trusted.
                if (action in CLOCK_ACTIONS) reminders.restartFromNow() else reminders.reschedule()
                entryPoint.widgetUpdater().refreshAll()
                entryPoint.lockScreenController().refresh()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val CLOCK_ACTIONS = setOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)
        val HANDLED_ACTIONS =
            CLOCK_ACTIONS +
                setOf(
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_LOCALE_CHANGED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
                )
    }
}
