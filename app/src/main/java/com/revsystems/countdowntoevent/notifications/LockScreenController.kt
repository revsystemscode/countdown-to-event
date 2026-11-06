package com.revsystems.countdowntoevent.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.revsystems.countdowntoevent.data.settings.LockScreenStore
import com.revsystems.countdowntoevent.di.appEntryPoint
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import com.revsystems.countdowntoevent.domain.time.CountdownCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the lock-screen notification in sync with the pinned event, and sets one alarm for
 * the next time its text changes (midnight, live mode starting, the event starting or ending).
 */
@Singleton
class LockScreenController
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val repository: EventRepository,
        private val store: LockScreenStore,
        private val notifier: LockScreenNotifier,
        private val clock: Clock,
    ) {
        private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
        private val mutex = Mutex()

        suspend fun refresh() =
            mutex.withLock {
                val pinnedId = store.current()
                val event = pinnedId?.let { repository.getEvent(it) }
                if (event == null) {
                    // Deleted events are unpinned rather than left as a stale notification.
                    if (pinnedId != null) store.clear()
                    clear()
                    return@withLock
                }
                if (!notifier.canPost()) {
                    // Stays pinned: it appears as soon as notifications are allowed.
                    clear()
                    return@withLock
                }
                val now = clock.instant()
                val countdown = CountdownCalculator.calculate(event, now, clock.zone)
                val content = LockScreenPlanner.content(event, countdown)
                if (content == LockScreenContent.Gone) {
                    store.unpin(event.id)
                    clear()
                    return@withLock
                }
                notifier.show(event, countdown, content)
                schedule(LockScreenPlanner.nextRefresh(event, countdown, now, clock.zone))
            }

        /** The user swiped the notification away. */
        suspend fun onDismissed() =
            mutex.withLock {
                store.clear()
                alarmManager.cancel(refreshIntent())
            }

        private fun clear() {
            notifier.cancel()
            alarmManager.cancel(refreshIntent())
        }

        private fun schedule(at: Instant) {
            // A second late so the new state is already in effect when the alarm runs.
            val millis = at.plusSeconds(1).toEpochMilli()
            // Wakeup: the lock screen is what people see when they turn the screen on, so it must be current.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, refreshIntent())
            } else {
                // Without exact-alarm access Android may batch this, usually by a few minutes.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, refreshIntent())
            }
        }

        private fun refreshIntent(): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, LockScreenReceiver::class.java).setAction(LockScreenReceiver.ACTION_REFRESH),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }

class LockScreenReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action
        if (action != ACTION_REFRESH && action != ACTION_DISMISSED) return
        val pending = goAsync()
        val entryPoint = context.appEntryPoint()
        entryPoint.applicationScope().launch {
            try {
                val controller = entryPoint.lockScreenController()
                if (action == ACTION_DISMISSED) controller.onDismissed() else controller.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.revsystems.countdowntoevent.REFRESH_LOCK_SCREEN"
        const val ACTION_DISMISSED = "com.revsystems.countdowntoevent.LOCK_SCREEN_DISMISSED"
    }
}
