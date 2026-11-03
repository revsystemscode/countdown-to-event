package com.revsystems.countdowntoevent.domain.time

import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** One concrete instance of an event. [end] is exclusive; it equals [start] for timed single-moment events. */
data class Occurrence(
    val index: Long,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
)

object RecurrenceResolver {
    /**
     * Occurrence number [index] (0 = the original date).
     *
     * Monthly/yearly dates are always computed from the original date, so Jan 31 gives
     * Feb 28 and then Mar 31 (no drift). Feb 29 yearly falls on Feb 28 in non-leap years.
     */
    fun occurrence(
        event: Event,
        index: Long,
        viewerZone: ZoneId,
    ): Occurrence {
        val date = shift(event.startDate, event.repeat, index)
        val zone = if (event.isAllDay) viewerZone else event.timeZone
        val start =
            event.startTime
                ?.let { ZonedDateTime.of(date, it, zone) }
                ?: date.atStartOfDay(zone)
        val end =
            if (event.isAllDay || event.endDate != null) {
                date.plusDays(spanDays(event) + 1).atStartOfDay(zone)
            } else {
                start
            }
        return Occurrence(index, start, end)
    }

    /**
     * The occurrence that matters at [now]: the first one that hasn't ended yet.
     * Non-repeating events always return their only occurrence, even when it's in the past.
     */
    fun current(
        event: Event,
        now: Instant,
        viewerZone: ZoneId,
    ): Occurrence {
        val first = occurrence(event, 0, viewerZone)
        if (event.repeat == RepeatRule.NONE || first.end.toInstant() > now) return first

        // Jump close to the answer instead of iterating from the original date, then walk forward.
        val zone = first.start.zone
        val referenceDate = now.atZone(zone).toLocalDate().minusDays(spanDays(event))
        var index = (estimateIndex(event.startDate, referenceDate, event.repeat) - 1).coerceAtLeast(0)
        var candidate = occurrence(event, index, viewerZone)
        while (candidate.end.toInstant() <= now) {
            index++
            candidate = occurrence(event, index, viewerZone)
        }
        return candidate
    }

    private fun spanDays(event: Event): Long = event.endDate?.let { ChronoUnit.DAYS.between(event.startDate, it) } ?: 0

    private fun shift(
        date: LocalDate,
        rule: RepeatRule,
        index: Long,
    ): LocalDate =
        when (rule) {
            RepeatRule.NONE -> date
            RepeatRule.DAILY -> date.plusDays(index)
            RepeatRule.WEEKLY -> date.plusWeeks(index)
            RepeatRule.BIWEEKLY -> date.plusWeeks(index * 2)
            RepeatRule.MONTHLY -> date.plusMonths(index)
            RepeatRule.YEARLY -> date.plusYears(index)
        }

    private fun estimateIndex(
        start: LocalDate,
        reference: LocalDate,
        rule: RepeatRule,
    ): Long =
        when (rule) {
            RepeatRule.NONE -> 0
            RepeatRule.DAILY -> ChronoUnit.DAYS.between(start, reference)
            RepeatRule.WEEKLY -> ChronoUnit.WEEKS.between(start, reference)
            RepeatRule.BIWEEKLY -> ChronoUnit.WEEKS.between(start, reference) / 2
            RepeatRule.MONTHLY -> ChronoUnit.MONTHS.between(start, reference)
            RepeatRule.YEARLY -> ChronoUnit.YEARS.between(start, reference)
        }
}
