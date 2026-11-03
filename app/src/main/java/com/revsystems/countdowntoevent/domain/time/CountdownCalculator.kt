package com.revsystems.countdowntoevent.domain.time

import com.revsystems.countdowntoevent.domain.model.Event
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class CountdownStatus {
    UPCOMING,

    /** Started but not finished: an all-day event today, or a multi-day trip under way. */
    IN_PROGRESS,
    REACHED,
}

data class Countdown(
    val occurrence: Occurrence,
    val status: CountdownStatus,
    /** Calendar days from the viewer's today to the start date. 0 once started. Shown on cards ("10 days"). */
    val daysUntil: Long,
    /** Calendar days since the start date for reached events, else 0 ("3 days ago"). */
    val daysSince: Long,
    /** Exact time left until start, ZERO once started. Drives the live countdown. */
    val remaining: Duration,
)

/** [days] here are 24-hour blocks, for the live "D:HH:MM:SS" display. */
data class CountdownParts(
    val days: Long,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
)

object CountdownCalculator {
    fun calculate(
        event: Event,
        now: Instant,
        viewerZone: ZoneId,
    ): Countdown {
        val occurrence = RecurrenceResolver.current(event, now, viewerZone)
        val start = occurrence.start.toInstant()
        val end = occurrence.end.toInstant()

        val status =
            when {
                now < start -> CountdownStatus.UPCOMING
                now < end -> CountdownStatus.IN_PROGRESS
                else -> CountdownStatus.REACHED
            }

        val today = now.atZone(viewerZone).toLocalDate()
        val startDate = occurrence.start.withZoneSameInstant(viewerZone).toLocalDate()
        val dayDelta = ChronoUnit.DAYS.between(today, startDate)

        return Countdown(
            occurrence = occurrence,
            status = status,
            daysUntil = if (status == CountdownStatus.UPCOMING) dayDelta.coerceAtLeast(0) else 0,
            daysSince = if (status == CountdownStatus.REACHED) (-dayDelta).coerceAtLeast(0) else 0,
            remaining = if (status == CountdownStatus.UPCOMING) Duration.between(now, start) else Duration.ZERO,
        )
    }
}

// Computed by hand: Duration.toHoursPart() and friends only exist from API 31.
fun Duration.toCountdownParts(): CountdownParts {
    val total = if (isNegative) 0L else seconds
    return CountdownParts(
        days = total / SECONDS_PER_DAY,
        hours = ((total % SECONDS_PER_DAY) / SECONDS_PER_HOUR).toInt(),
        minutes = ((total % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).toInt(),
        seconds = (total % SECONDS_PER_MINUTE).toInt(),
    )
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_DAY = 86_400L
