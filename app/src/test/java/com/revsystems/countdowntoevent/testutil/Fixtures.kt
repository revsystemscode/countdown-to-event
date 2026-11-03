package com.revsystems.countdowntoevent.testutil

import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

val MADRID: ZoneId = ZoneId.of("Europe/Madrid")
val PARIS: ZoneId = ZoneId.of("Europe/Paris")
val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")

fun date(iso: String): LocalDate = LocalDate.parse(iso)

fun time(iso: String): LocalTime = LocalTime.parse(iso)

/** Instant for a local wall-clock time, e.g. at("2026-11-02T10:00", MADRID). */
fun at(
    isoLocalDateTime: String,
    zone: ZoneId,
): Instant = LocalDateTime.parse(isoLocalDateTime).atZone(zone).toInstant()

fun event(
    start: String,
    time: String? = null,
    end: String? = null,
    zone: ZoneId = MADRID,
    repeat: RepeatRule = RepeatRule.NONE,
    title: String = "Event",
    id: Long = 0,
) = Event(
    id = id,
    title = title,
    startDate = date(start),
    startTime = time?.let(::time),
    endDate = end?.let(::date),
    timeZone = zone,
    repeat = repeat,
)
