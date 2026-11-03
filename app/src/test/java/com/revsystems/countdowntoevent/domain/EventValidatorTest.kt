package com.revsystems.countdowntoevent.domain

import com.revsystems.countdowntoevent.testutil.event
import org.junit.Assert.assertEquals
import org.junit.Test

class EventValidatorTest {
    @Test
    fun validEvent() {
        assertEquals(emptySet<EventValidationError>(), EventValidator.validate(event("2026-11-02", title = "Trip")))
    }

    @Test
    fun blankTitle() {
        assertEquals(setOf(EventValidationError.BLANK_TITLE), EventValidator.validate(event("2026-11-02", title = "   ")))
    }

    @Test
    fun titleLengthCountsEmojiAsOneCharacter() {
        val sixtyEmoji = "🎂".repeat(EventValidator.MAX_TITLE_LENGTH)
        assertEquals(emptySet<EventValidationError>(), EventValidator.validate(event("2026-11-02", title = sixtyEmoji)))
        assertEquals(
            setOf(EventValidationError.TITLE_TOO_LONG),
            EventValidator.validate(event("2026-11-02", title = sixtyEmoji + "a")),
        )
    }

    @Test
    fun endBeforeStart() {
        assertEquals(
            setOf(EventValidationError.END_BEFORE_START),
            EventValidator.validate(event("2026-11-02", end = "2026-11-01", title = "Trip")),
        )
    }
}
