package com.revsystems.countdowntoevent.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A countdown target.
 *
 * Time semantics:
 * - All-day events ([startTime] == null) are "floating": Jan 15 is Jan 15 wherever the viewer is.
 * - Timed events are a fixed instant in [timeZone], so a shared event ticks down in sync across time zones.
 *
 * [createdAt] and [updatedAt] are set by the repository on save.
 * [remoteId] and [ownerId] are reserved for sync in v2.
 */
data class Event(
    val id: Long = 0,
    val title: String,
    val emoji: String? = null,
    val startDate: LocalDate,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val timeZone: ZoneId = ZoneId.systemDefault(),
    val repeat: RepeatRule = RepeatRule.NONE,
    val background: EventBackground = EventBackground.Default,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val remoteId: String? = null,
    val ownerId: String? = null,
) {
    val isAllDay: Boolean get() = startTime == null
}

enum class RepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    YEARLY,
}

sealed interface EventBackground {
    /** ARGB color, e.g. 0xFF1A9AA6.toInt(). */
    data class Solid(
        val argb: Int,
    ) : EventBackground

    /** One of the images bundled with the app, referenced by a stable key. */
    data class Preset(
        val key: String,
    ) : EventBackground

    /** A user-picked image copied into app storage. */
    data class LocalImage(
        val uri: String,
    ) : EventBackground

    companion object {
        val Default: EventBackground = Solid(0xFF1A9AA6.toInt())
    }
}
