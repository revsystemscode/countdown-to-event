package com.revsystems.countdowntoevent.ui.format

import com.revsystems.countdowntoevent.domain.time.CountdownCalculator
import com.revsystems.countdowntoevent.testutil.MADRID
import com.revsystems.countdowntoevent.testutil.at
import com.revsystems.countdowntoevent.testutil.event
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.Locale

class CountdownBadgeTest {
    private val now = at("2026-11-02T10:00", MADRID)

    private fun badge(
        start: String,
        time: String? = null,
        end: String? = null,
        at: Instant = now,
    ): CountdownBadge {
        val e = event(start, time = time, end = end)
        return badgeFor(e, CountdownCalculator.calculate(e, at, MADRID))
    }

    @Test
    fun daysAhead() = assertEquals(CountdownBadge.Days(10), badge("2026-11-12"))

    @Test
    fun hoursLeftToday() = assertEquals(CountdownBadge.Hours(3), badge("2026-11-02", time = "13:30"))

    @Test
    fun minutesRoundUp() {
        assertEquals(CountdownBadge.Minutes(1), badge("2026-11-02", time = "10:00:30"))
        assertEquals(CountdownBadge.Minutes(45), badge("2026-11-02", time = "10:45"))
    }

    @Test
    fun todayAndOngoing() {
        assertEquals(CountdownBadge.Today, badge("2026-11-02"))
        assertEquals(CountdownBadge.Ongoing, badge("2026-11-01", end = "2026-11-04"))
    }

    @Test
    fun reached() {
        assertEquals(CountdownBadge.DaysAgo(3), badge("2026-10-30"))
        assertEquals(CountdownBadge.Today, badge("2026-11-02", time = "08:00"))
    }

    @Test
    fun formatsRangeForCurrentOccurrence() {
        val e = event("2026-11-20", end = "2026-11-22")
        val text = formatEventDates(e, CountdownCalculator.calculate(e, now, MADRID), Locale.US, MADRID)
        assertEquals("Nov 20, 2026 → Nov 22, 2026", text)
    }
}
