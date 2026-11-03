package com.revsystems.countdowntoevent.data.local

import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.testutil.TOKYO
import com.revsystems.countdowntoevent.testutil.event
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class EventMapperTest {
    @Test
    fun roundTripsEveryField() {
        val backgrounds =
            listOf(
                EventBackground.Solid(0xFF1A9AA6.toInt()),
                EventBackground.Solid(0x00000000),
                EventBackground.Preset("beach"),
                EventBackground.LocalImage("file:///data/user/0/app/files/bg/1.jpg"),
            )
        backgrounds.forEach { bg ->
            val original =
                event("2026-11-20", time = "09:30:15", end = "2026-11-22", zone = TOKYO, repeat = RepeatRule.MONTHLY, id = 7)
                    .copy(
                        emoji = "✈️",
                        background = bg,
                        createdAt = Instant.ofEpochMilli(1_000),
                        updatedAt = Instant.ofEpochMilli(2_000),
                        remoteId = "r1",
                        ownerId = "o1",
                    )
            assertEquals(original, original.toEntity().toDomain())
        }
    }

    @Test
    fun allDayEventHasNoTime() {
        val entity = event("2026-11-20").toEntity()
        assertEquals(null, entity.startSecondOfDay)
        assertEquals(null, entity.toDomain().startTime)
    }

    @Test
    fun unknownValuesFallBackToDefaults() {
        val entity =
            event("2026-11-20").toEntity().copy(
                repeat = "HOURLY",
                timeZone = "Mars/Olympus",
                backgroundType = "video",
            )
        val domain = entity.toDomain()
        assertEquals(RepeatRule.NONE, domain.repeat)
        assertEquals(ZoneOffset.UTC, domain.timeZone)
        assertEquals(EventBackground.Default, domain.background)
    }
}
