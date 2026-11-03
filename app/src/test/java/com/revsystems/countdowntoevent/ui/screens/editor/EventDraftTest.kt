package com.revsystems.countdowntoevent.ui.screens.editor

import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.testutil.MADRID
import com.revsystems.countdowntoevent.testutil.TOKYO
import com.revsystems.countdowntoevent.testutil.date
import com.revsystems.countdowntoevent.testutil.event
import com.revsystems.countdowntoevent.testutil.time
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class EventDraftTest {
    @Test
    fun movingStartKeepsTripLength() {
        val draft = EventDraft(startDate = date("2026-11-20"), endDate = date("2026-11-23"))
        val moved = draft.withStartDate(date("2026-12-01"))
        assertEquals(date("2026-12-04"), moved.endDate)
        assertEquals(null, EventDraft(startDate = date("2026-11-20")).withStartDate(date("2026-12-01")).endDate)
    }

    @Test
    fun newEventUsesDeviceZone() {
        val event = EventDraft(title = "Trip", startDate = date("2026-11-20"), startTime = time("09:00")).toEvent(null, TOKYO)
        assertEquals(TOKYO, event.timeZone)
        assertEquals(0L, event.id)
    }

    @Test
    fun editingOtherFieldsKeepsOriginalZoneAndMetadata() {
        val original =
            event("2026-11-20", time = "09:00", zone = MADRID, id = 5)
                .copy(createdAt = Instant.ofEpochSecond(100), remoteId = "r")
        val edited = EventDraft.from(original).copy(title = "Renamed", repeat = RepeatRule.YEARLY).toEvent(original, TOKYO)
        assertEquals(MADRID, edited.timeZone)
        assertEquals(5L, edited.id)
        assertEquals(Instant.ofEpochSecond(100), edited.createdAt)
        assertEquals("r", edited.remoteId)
        assertEquals("Renamed", edited.title)
    }

    @Test
    fun changingTimeReanchorsToDeviceZone() {
        val original = event("2026-11-20", time = "09:00", zone = MADRID, id = 5)
        val edited = EventDraft.from(original).copy(startTime = time("10:00")).toEvent(original, TOKYO)
        assertEquals(TOKYO, edited.timeZone)
    }
}
