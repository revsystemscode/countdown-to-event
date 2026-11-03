package com.revsystems.countdowntoevent.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.revsystems.countdowntoevent.di.appEntryPoint
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val scheduler: WidgetRefreshScheduler,
        private val clock: Clock,
    ) {
        /** Redraws every widget with fresh data and schedules the next automatic refresh. */
        suspend fun refreshAll() {
            refreshAllOf(EventWidget())
            refreshAllOf(UpcomingWidget())
            scheduler.reschedule()
        }

        suspend fun refreshEventWidget(id: GlanceId) {
            refresh(EventWidget(), id)
            scheduler.reschedule()
        }

        private suspend fun refreshAllOf(widget: GlanceAppWidget) {
            GlanceAppWidgetManager(context).getGlanceIds(widget.javaClass).forEach { refresh(widget, it) }
        }

        private suspend fun refresh(
            widget: GlanceAppWidget,
            id: GlanceId,
        ) {
            updateAppWidgetState(context, id) { it[WidgetPrefs.TICK] = clock.millis() }
            widget.update(context, id)
        }
    }

/** Wakes the widgets only when their content actually changes (see [EventWidgetModels.nextRefresh]). */
@Singleton
class WidgetRefreshScheduler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val repository: EventRepository,
        private val clock: Clock,
    ) {
        private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

        suspend fun reschedule() {
            val manager = GlanceAppWidgetManager(context)
            val eventWidgets = manager.getGlanceIds(EventWidget::class.java)
            val listWidgets = manager.getGlanceIds(UpcomingWidget::class.java)
            if (eventWidgets.isEmpty() && listWidgets.isEmpty()) {
                alarmManager.cancel(refreshIntent())
                return
            }

            val events = repository.getEvents()
            val now = clock.instant()
            val liveEventIds =
                eventWidgets
                    .mapNotNull { id ->
                        val config = WidgetPrefs.config(getAppWidgetState(context, PreferencesGlanceStateDefinition, id))
                        (EventWidgetModels.build(events, config, now, clock.zone) as? EventWidgetModel.Showing)
                            ?.takeIf { it.live }
                            ?.item
                            ?.event
                            ?.id
                    }.toSet()
            val next = EventWidgetModels.nextRefresh(events, liveEventIds, now, clock.zone).plusSeconds(1)
            schedule(maxOf(next, now.plusSeconds(MIN_DELAY_SECONDS)))
        }

        private fun schedule(at: Instant) {
            val millis = at.toEpochMilli()
            // RTC (not _WAKEUP): widgets can't be seen with the screen off, so don't wake the device.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC, millis, refreshIntent())
            } else {
                // Without exact-alarm access Android may deliver this up to ~10 minutes late.
                alarmManager.setWindow(AlarmManager.RTC, millis, WINDOW_MILLIS, refreshIntent())
            }
        }

        private fun refreshIntent(): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, WidgetRefreshReceiver::class.java).setAction(WidgetRefreshReceiver.ACTION_REFRESH),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private companion object {
            const val MIN_DELAY_SECONDS = 30L
            const val WINDOW_MILLIS = 60_000L
        }
    }

/** Fired by [WidgetRefreshScheduler]'s alarm. System events are handled by SystemEventsReceiver. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_REFRESH) return
        val pending = goAsync()
        val entryPoint = context.appEntryPoint()
        entryPoint.applicationScope().launch {
            try {
                entryPoint.widgetUpdater().refreshAll()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.revsystems.countdowntoevent.REFRESH_WIDGETS"
    }
}
