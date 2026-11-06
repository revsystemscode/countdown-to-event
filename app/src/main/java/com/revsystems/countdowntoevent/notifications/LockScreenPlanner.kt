package com.revsystems.countdowntoevent.notifications

import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.domain.time.CountdownStatus
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** What the pinned lock-screen notification shows. */
sealed interface LockScreenContent {
    /** "12 days to go". */
    data class Days(
        val count: Long,
    ) : LockScreenContent

    /** A timed event less than a day away: the system ticks a countdown to [start]. */
    data class Live(
        val start: Instant,
    ) : LockScreenContent

    /** An all-day event today, a multi-day event under way, or a moment that passed earlier today. */
    data class Today(
        val ongoing: Boolean,
    ) : LockScreenContent

    /** A one-off event that is over: the notification goes away and the event is unpinned. */
    data object Gone : LockScreenContent
}

/** Pure logic for the lock-screen notification: what to show and when it next changes. */
object LockScreenPlanner {
    private val LIVE_WINDOW: Duration = Duration.ofDays(1)

    fun content(
        event: Event,
        countdown: Countdown,
    ): LockScreenContent =
        when (countdown.status) {
            CountdownStatus.UPCOMING ->
                // All-day events start at midnight, so a ticking clock adds nothing over "1 day".
                if (!event.isAllDay && countdown.remaining < LIVE_WINDOW) {
                    LockScreenContent.Live(countdown.occurrence.start.toInstant())
                } else {
                    LockScreenContent.Days(countdown.daysUntil.coerceAtLeast(1))
                }
            CountdownStatus.IN_PROGRESS -> LockScreenContent.Today(ongoing = event.endDate != null)
            CountdownStatus.REACHED ->
                if (countdown.daysSince == 0L) LockScreenContent.Today(ongoing = false) else LockScreenContent.Gone
        }

    /** Next moment the content changes: local midnight, the switch to live mode, or the event starting or ending. */
    fun nextRefresh(
        event: Event,
        countdown: Countdown,
        now: Instant,
        zone: ZoneId,
    ): Instant {
        val start = countdown.occurrence.start.toInstant()
        val midnight =
            now
                .atZone(zone)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
        val candidates = mutableListOf(midnight, start, countdown.occurrence.end.toInstant())
        if (!event.isAllDay) candidates += start.minus(LIVE_WINDOW)
        return candidates.filter { it > now }.minOrNull() ?: midnight
    }
}
