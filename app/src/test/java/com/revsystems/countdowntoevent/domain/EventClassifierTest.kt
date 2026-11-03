package com.revsystems.countdowntoevent.domain

import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.testutil.MADRID
import com.revsystems.countdowntoevent.testutil.at
import com.revsystems.countdowntoevent.testutil.event
import org.junit.Assert.assertEquals
import org.junit.Test

class EventClassifierTest {
    private val now = at("2026-11-02T10:00", MADRID)

    @Test
    fun splitsAndSortsEvents() {
        val events =
            listOf(
                event("2026-12-24", title = "Christmas eve", id = 1),
                event("2026-10-01", title = "Old concert", id = 2),
                event("2026-11-02", title = "Today", id = 3),
                event("2025-11-10", repeat = RepeatRule.YEARLY, title = "Anniversary", id = 4),
                event("2026-10-20", title = "Recent trip", id = 5),
                event("2026-11-12", title = "beach", id = 6),
                event("2026-11-12", title = "Airport", id = 7),
            )

        val result = EventClassifier.classify(events, now, MADRID)

        assertEquals(listOf(3L, 4L, 7L, 6L, 1L), result.upcoming.map { it.event.id })
        assertEquals(listOf(5L, 2L), result.reached.map { it.event.id })
    }

    @Test
    fun noEvents() {
        val result = EventClassifier.classify(emptyList(), now, MADRID)
        assertEquals(0, result.upcoming.size + result.reached.size)
    }
}
