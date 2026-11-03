package com.revsystems.countdowntoevent.data.backup

import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.testutil.TOKYO
import com.revsystems.countdowntoevent.testutil.event
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class BackupCodecTest {
    private val exportedAt = Instant.parse("2026-11-03T12:00:00Z")

    @Test
    fun roundTripKeepsEventFields() {
        val original =
            event("2026-11-20", time = "09:30", end = "2026-11-22", zone = TOKYO, repeat = RepeatRule.YEARLY, title = "Trip", id = 9)
                .copy(emoji = "✈️", background = EventBackground.Preset("ocean"))
        val decoded = BackupCodec.decode(BackupCodec.encode(listOf(original), exportedAt) { null })

        assertEquals(0, decoded.invalid)
        val event = decoded.events.single().event
        // Ids and timestamps are assigned again on import.
        assertEquals(original.copy(id = 0, createdAt = event.createdAt, updatedAt = event.updatedAt), event)
    }

    @Test
    fun photosTravelInsideTheFile() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val withPhoto = event("2026-11-20", title = "Photo").copy(background = EventBackground.LocalImage("file:///x/bg.jpg"))
        val decoded = BackupCodec.decode(BackupCodec.encode(listOf(withPhoto), exportedAt) { bytes })
        assertArrayEquals(bytes, decoded.events.single().image)
    }

    @Test
    fun missingPhotoFallsBackToDefaultColor() {
        val withPhoto = event("2026-11-20", title = "Photo").copy(background = EventBackground.LocalImage("file:///gone.jpg"))
        val decoded = BackupCodec.decode(BackupCodec.encode(listOf(withPhoto), exportedAt) { null })
        assertEquals(
            EventBackground.Default,
            decoded.events
                .single()
                .event.background,
        )
        assertEquals(null, decoded.events.single().image)
    }

    @Test
    fun badEntriesAreSkippedAndCounted() {
        val json =
            """
            {"format":"countdown-to-event-backup","version":1,"exportedAt":"2026-11-03T12:00:00Z","futureField":true,
             "events":[
               {"title":"Good","startDate":"2026-12-24","timeZone":"Europe/Madrid"},
               {"title":"Bad date","startDate":"2026-13-40","timeZone":"Europe/Madrid"},
               {"title":"   ","startDate":"2026-12-24","timeZone":"Europe/Madrid"},
               {"title":"Bad zone","startDate":"2026-12-24","timeZone":"Mars/Base"},
               {"title":"Bad image","startDate":"2026-12-24","timeZone":"UTC","background":{"type":"image","image":"%%%"}}
             ]}
            """.trimIndent()
        val decoded = BackupCodec.decode(json)
        assertEquals(listOf("Good"), decoded.events.map { it.event.title })
        assertEquals(4, decoded.invalid)
    }

    @Test
    fun rejectsFilesThatAreNotBackups() {
        assertThrows(InvalidBackupException::class.java) { BackupCodec.decode("not json") }
        assertThrows(InvalidBackupException::class.java) { BackupCodec.decode("""{"hello":"world"}""") }
        assertThrows(InvalidBackupException::class.java) {
            BackupCodec.decode("""{"format":"other-app","version":1,"exportedAt":"x","events":[]}""")
        }
    }

    @Test
    fun rejectsBackupsFromNewerVersions() {
        assertThrows(InvalidBackupException::class.java) {
            BackupCodec.decode("""{"format":"countdown-to-event-backup","version":99,"exportedAt":"x","events":[]}""")
        }
    }

    @Test
    fun identityIgnoresCaseAndIds() {
        val a = event("2026-12-24", title = "Christmas", id = 1)
        val b = event("2026-12-24", title = " christmas ", id = 2)
        assertEquals(BackupCodec.identity(a), BackupCodec.identity(b))
    }
}
