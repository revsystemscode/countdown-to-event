package com.revsystems.countdowntoevent.ui.screens.editor

import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** The editable fields of an event, as the form holds them. */
data class EventDraft(
    val title: String = "",
    val emoji: String? = null,
    val startDate: LocalDate,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val repeat: RepeatRule = RepeatRule.NONE,
    val background: EventBackground = EventBackground.Default,
) {
    /** Moves the start date and keeps a multi-day event's length. */
    fun withStartDate(date: LocalDate): EventDraft {
        val span = endDate?.let { ChronoUnit.DAYS.between(startDate, it).coerceAtLeast(0) }
        return copy(startDate = date, endDate = span?.let { date.plusDays(it) })
    }

    /**
     * A timed event's wall-clock time belongs to the zone where it was entered. When the user
     * changes the date or time, it is re-anchored to their current zone; otherwise the original
     * zone is kept so the event doesn't shift.
     */
    fun toEvent(
        original: Event?,
        deviceZone: ZoneId,
    ): Event {
        val whenUnchanged = original != null && original.startDate == startDate && original.startTime == startTime
        val zone = if (whenUnchanged) original.timeZone else deviceZone
        val base = original ?: Event(title = title, startDate = startDate)
        return base.copy(
            title = title,
            emoji = emoji,
            startDate = startDate,
            startTime = startTime,
            endDate = endDate,
            timeZone = zone,
            repeat = repeat,
            background = background,
        )
    }

    companion object {
        fun from(event: Event) =
            EventDraft(
                title = event.title,
                emoji = event.emoji,
                startDate = event.startDate,
                startTime = event.startTime,
                endDate = event.endDate,
                repeat = event.repeat,
                background = event.background,
            )
    }
}
