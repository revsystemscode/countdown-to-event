package com.revsystems.countdowntoevent.notifications

import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.domain.settings.AppSettings
import com.revsystems.countdowntoevent.domain.settings.ReminderOffset
import com.revsystems.countdowntoevent.domain.time.Occurrence
import com.revsystems.countdowntoevent.domain.time.RecurrenceResolver
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

enum class NotificationKind {
    /** "Tomorrow: Lise's birthday" */
    REMINDER,

    /** "Today is the day!" */
    ARRIVED,
}

data class PlannedNotification(
    val event: Event,
    val kind: NotificationKind,
    val at: Instant,
    val occurrence: Occurrence,
    /** Set for reminders: how far ahead of the event it fires. */
    val offset: ReminderOffset? = null,
)

/**
 * Decides when notifications fire. Pure logic: the scheduler sets a single alarm for [next]
 * and, when it fires, shows everything [due] since the last check.
 */
object ReminderPlanner {
    /** All-day events have no time, so their notifications go out in the morning. */
    val ALL_DAY_NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)

    private val ONE_DAY: Duration = Duration.ofDays(1)

    fun forOccurrence(
        event: Event,
        occurrence: Occurrence,
        settings: AppSettings,
    ): List<PlannedNotification> {
        val reference =
            if (event.isAllDay) {
                occurrence.start
                    .toLocalDate()
                    .atTime(ALL_DAY_NOTIFY_TIME)
                    .atZone(occurrence.start.zone)
            } else {
                occurrence.start
            }
        return buildList {
            val offset = settings.reminderOffset
            offset.duration?.let { duration ->
                // "15 minutes before" an all-day event isn't meaningful: the day notification covers it.
                if (!event.isAllDay || duration >= ONE_DAY) {
                    add(PlannedNotification(event, NotificationKind.REMINDER, reference.minus(duration).toInstant(), occurrence, offset))
                }
            }
            if (settings.notifyOnEventDay) {
                add(PlannedNotification(event, NotificationKind.ARRIVED, reference.toInstant(), occurrence))
            }
        }
    }

    /** Earliest notification strictly after [after], or null if nothing is planned. */
    fun next(
        events: List<Event>,
        settings: AppSettings,
        after: Instant,
        zone: ZoneId,
    ): Instant? =
        events
            .flatMap { event -> candidates(event, after, settings, zone).filter { it.at > after && isAfterLastEdit(it) } }
            .minOfOrNull { it.at }

    /** Notifications in the window (from, to], at most one per event and kind. */
    fun due(
        events: List<Event>,
        settings: AppSettings,
        from: Instant,
        to: Instant,
        zone: ZoneId,
    ): List<PlannedNotification> =
        events.flatMap { event ->
            // Looking from both ends catches an occurrence that ended inside the window.
            (candidates(event, from, settings, zone) + candidates(event, to, settings, zone))
                .filter { it.at > from && it.at <= to && isAfterLastEdit(it) }
                .groupBy { it.kind }
                .map { (_, items) -> items.maxBy { it.at } }
        }

    private fun candidates(
        event: Event,
        around: Instant,
        settings: AppSettings,
        zone: ZoneId,
    ): List<PlannedNotification> {
        val current = RecurrenceResolver.current(event, around, zone)
        val occurrences =
            if (event.repeat == RepeatRule.NONE) {
                listOf(current)
            } else {
                // The current occurrence's notifications may all be past; the next one's aren't.
                listOf(current, RecurrenceResolver.occurrence(event, current.index + 1, zone))
            }
        return occurrences.flatMap { forOccurrence(event, it, settings) }
    }

    /** A reminder whose time had already passed when the event was created or edited would be noise. */
    private fun isAfterLastEdit(item: PlannedNotification) = item.at >= item.event.updatedAt
}
