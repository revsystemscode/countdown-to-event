package com.revsystems.countdowntoevent.widget

import com.revsystems.countdowntoevent.domain.EventClassifier
import com.revsystems.countdowntoevent.domain.EventCountdown
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.domain.time.CountdownCalculator
import com.revsystems.countdowntoevent.domain.time.CountdownStatus
import com.revsystems.countdowntoevent.ui.format.CountdownBadge
import com.revsystems.countdowntoevent.ui.format.badgeFor
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** What a single-event widget is set to show. */
data class EventWidgetConfig(
    /** null = always the next upcoming event. */
    val eventId: Long? = null,
    /** Show a ticking hours/minutes/seconds countdown. */
    val live: Boolean = false,
)

sealed interface EventWidgetModel {
    data object Empty : EventWidgetModel

    data class Showing(
        val item: EventCountdown,
        /** Live mode only applies while the event is still ahead. */
        val live: Boolean,
    ) : EventWidgetModel
}

/** Widgets can't redraw every minute, so "3 hours" becomes a static "Today 20:00". */
sealed interface WidgetBadge {
    data class Days(
        val count: Long,
    ) : WidgetBadge

    data class TodayAt(
        val time: LocalTime,
    ) : WidgetBadge

    data class DaysAgo(
        val count: Long,
    ) : WidgetBadge

    data object Today : WidgetBadge

    data object Ongoing : WidgetBadge
}

/** Whole 24-hour days plus the remainder that a Chronometer ticks down. */
data class LiveSplit(
    val days: Long,
    val remainder: Duration,
)

object EventWidgetModels {
    private val DAY: Duration = Duration.ofDays(1)

    fun build(
        events: List<Event>,
        config: EventWidgetConfig,
        now: Instant,
        zone: ZoneId,
    ): EventWidgetModel {
        val classified = EventClassifier.classify(events, now, zone)
        // A deleted event falls back to the next one instead of leaving a broken widget.
        val chosen =
            config.eventId?.let { id -> (classified.upcoming + classified.reached).firstOrNull { it.event.id == id } }
                ?: classified.upcoming.firstOrNull()
                ?: return EventWidgetModel.Empty
        return EventWidgetModel.Showing(chosen, live = config.live && chosen.countdown.status == CountdownStatus.UPCOMING)
    }

    fun upcoming(
        events: List<Event>,
        now: Instant,
        zone: ZoneId,
        limit: Int,
    ): List<EventCountdown> = EventClassifier.classify(events, now, zone).upcoming.take(limit)

    fun badge(
        event: Event,
        countdown: Countdown,
        zone: ZoneId,
    ): WidgetBadge =
        when (val badge = badgeFor(event, countdown)) {
            is CountdownBadge.Days -> WidgetBadge.Days(badge.count)
            is CountdownBadge.Hours, is CountdownBadge.Minutes ->
                WidgetBadge.TodayAt(
                    countdown.occurrence.start
                        .withZoneSameInstant(zone)
                        .toLocalTime(),
                )
            is CountdownBadge.DaysAgo -> WidgetBadge.DaysAgo(badge.count)
            CountdownBadge.Today -> WidgetBadge.Today
            CountdownBadge.Ongoing -> WidgetBadge.Ongoing
        }

    fun liveSplit(remaining: Duration): LiveSplit {
        val days = remaining.toDays()
        return LiveSplit(days, remaining.minus(DAY.multipliedBy(days)))
    }

    /**
     * The next moment any widget's content changes: local midnight (day counts roll over),
     * an event starting or ending, or a live widget crossing a whole-day boundary.
     */
    fun nextRefresh(
        events: List<Event>,
        liveEventIds: Set<Long>,
        now: Instant,
        zone: ZoneId,
    ): Instant {
        val midnight =
            now
                .atZone(zone)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
        val candidates = mutableListOf(midnight)
        events.forEach { event ->
            val countdown = CountdownCalculator.calculate(event, now, zone)
            candidates += listOf(countdown.occurrence.start.toInstant(), countdown.occurrence.end.toInstant()).filter { it > now }
            if (event.id in liveEventIds && countdown.status == CountdownStatus.UPCOMING) {
                val remainder = liveSplit(countdown.remaining).remainder
                candidates += now.plus(if (remainder.isZero) DAY else remainder)
            }
        }
        return candidates.min()
    }
}
