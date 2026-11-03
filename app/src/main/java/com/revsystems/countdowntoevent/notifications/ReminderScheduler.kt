package com.revsystems.countdowntoevent.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.revsystems.countdowntoevent.data.settings.ReminderStateStore
import com.revsystems.countdowntoevent.di.appEntryPoint
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import com.revsystems.countdowntoevent.domain.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps exactly one alarm: the next notification. When it fires, everything due since the
 * last check is shown and the following alarm is set. "Checked until" survives process death,
 * so a reminder missed while the app wasn't running is still delivered (if not too late).
 */
@Singleton
class ReminderScheduler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val repository: EventRepository,
        private val settings: SettingsRepository,
        private val state: ReminderStateStore,
        private val notifier: ReminderNotifier,
        private val clock: Clock,
    ) {
        private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
        private val mutex = Mutex()

        /** After events or system state change. */
        suspend fun reschedule() = mutex.withLock { rescheduleLocked() }

        /** After the user changes reminder settings: don't fire reminders that would have been in the past. */
        suspend fun restartFromNow() =
            mutex.withLock {
                state.setCheckedUntil(clock.instant())
                rescheduleLocked()
            }

        /** Called by the alarm. */
        suspend fun deliverDue() =
            mutex.withLock {
                val now = clock.instant()
                val from = state.checkedUntil() ?: now
                if (from < now) {
                    ReminderPlanner
                        .due(repository.getEvents(), settings.current(), from, now, clock.zone)
                        // A reminder hours late (phone was off) is more confusing than useful.
                        .filter { Duration.between(it.at, now) <= MAX_LATENESS }
                        .forEach(notifier::show)
                }
                state.setCheckedUntil(now)
                rescheduleLocked()
            }

        private suspend fun rescheduleLocked() {
            val now = clock.instant()
            val after = state.checkedUntil() ?: now.also { state.setCheckedUntil(it) }
            val next = ReminderPlanner.next(repository.getEvents(), settings.current(), after, clock.zone)
            if (next == null) {
                alarmManager.cancel(alarmIntent())
            } else {
                // A missed one (next <= now) fires right away.
                set(maxOf(next, now.plusSeconds(1)))
            }
        }

        private fun set(at: Instant) {
            val millis = at.toEpochMilli()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarmIntent())
            } else {
                // Without exact-alarm access Android may batch this, usually by a few minutes.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarmIntent())
            }
        }

        private fun alarmIntent(): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_DELIVER),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private companion object {
            val MAX_LATENESS: Duration = Duration.ofHours(1)
        }
    }

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_DELIVER) return
        val pending = goAsync()
        val entryPoint = context.appEntryPoint()
        entryPoint.applicationScope().launch {
            try {
                entryPoint.reminderScheduler().deliverDue()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DELIVER = "com.revsystems.countdowntoevent.DELIVER_REMINDERS"
    }
}
