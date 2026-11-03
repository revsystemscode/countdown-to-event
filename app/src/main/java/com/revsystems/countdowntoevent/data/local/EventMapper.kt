package com.revsystems.countdowntoevent.data.local

import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private const val BG_SOLID = "solid"
private const val BG_PRESET = "preset"
private const val BG_IMAGE = "image"

fun Event.toEntity(): EventEntity {
    val (bgType, bgValue) =
        when (val bg = background) {
            is EventBackground.Solid -> BG_SOLID to "%08X".format(bg.argb)
            is EventBackground.Preset -> BG_PRESET to bg.key
            is EventBackground.LocalImage -> BG_IMAGE to bg.uri
        }
    return EventEntity(
        id = id,
        title = title,
        emoji = emoji,
        startEpochDay = startDate.toEpochDay(),
        startSecondOfDay = startTime?.toSecondOfDay(),
        endEpochDay = endDate?.toEpochDay(),
        timeZone = timeZone.id,
        repeat = repeat.name,
        backgroundType = bgType,
        backgroundValue = bgValue,
        createdAtMillis = createdAt.toEpochMilli(),
        updatedAtMillis = updatedAt.toEpochMilli(),
        remoteId = remoteId,
        ownerId = ownerId,
    )
}

/** Unknown values (e.g. written by a newer app version) fall back to safe defaults instead of crashing. */
fun EventEntity.toDomain(): Event =
    Event(
        id = id,
        title = title,
        emoji = emoji,
        startDate = LocalDate.ofEpochDay(startEpochDay),
        startTime = startSecondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) },
        endDate = endEpochDay?.let(LocalDate::ofEpochDay),
        timeZone = parseZone(timeZone),
        repeat = RepeatRule.entries.firstOrNull { it.name == repeat } ?: RepeatRule.NONE,
        background = parseBackground(backgroundType, backgroundValue),
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtMillis),
        remoteId = remoteId,
        ownerId = ownerId,
    )

private fun parseZone(id: String): ZoneId =
    try {
        ZoneId.of(id)
    } catch (_: DateTimeException) {
        ZoneOffset.UTC
    }

private fun parseBackground(
    type: String,
    value: String,
): EventBackground =
    when (type) {
        BG_SOLID -> value.toLongOrNull(16)?.let { EventBackground.Solid(it.toInt()) }
        BG_PRESET -> EventBackground.Preset(value)
        BG_IMAGE -> EventBackground.LocalImage(value)
        else -> null
    } ?: EventBackground.Default
