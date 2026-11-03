package com.revsystems.countdowntoevent.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * System clock that re-reads the device time zone on every call, so countdowns follow
 * the user when they travel (Clock.systemDefaultZone() would freeze the zone at startup).
 */
class DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
