package com.revsystems.countdowntoevent.domain.time

import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.testutil.MADRID
import com.revsystems.countdowntoevent.testutil.PARIS
import com.revsystems.countdowntoevent.testutil.TOKYO
import com.revsystems.countdowntoevent.testutil.at
import com.revsystems.countdowntoevent.testutil.event
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class CountdownCalculatorTest {
    private val now = at("2026-11-02T10:00", MADRID)

    @Test
    fun allDayEventInTenDays() {
        val c = CountdownCalculator.calculate(event("2026-11-12"), now, MADRID)
        assertEquals(CountdownStatus.UPCOMING, c.status)
        assertEquals(10L, c.daysUntil)
        assertEquals(Duration.ofDays(10).minusHours(10), c.remaining)
    }

    @Test
    fun allDayEventTodayIsInProgress() {
        val c = CountdownCalculator.calculate(event("2026-11-02"), now, MADRID)
        assertEquals(CountdownStatus.IN_PROGRESS, c.status)
        assertEquals(0L, c.daysUntil)
        assertEquals(Duration.ZERO, c.remaining)
    }

    @Test
    fun pastEventIsReachedWithDaysSince() {
        val c = CountdownCalculator.calculate(event("2026-10-30"), now, MADRID)
        assertEquals(CountdownStatus.REACHED, c.status)
        assertEquals(3L, c.daysSince)
        assertEquals(0L, c.daysUntil)
    }

    @Test
    fun timedEventLaterToday() {
        val c = CountdownCalculator.calculate(event("2026-11-02", time = "13:00"), now, MADRID)
        assertEquals(CountdownStatus.UPCOMING, c.status)
        assertEquals(0L, c.daysUntil)
        assertEquals(Duration.ofHours(3), c.remaining)
    }

    @Test
    fun timedEventJustAfterMidnightCountsAsTomorrow() {
        val c = CountdownCalculator.calculate(event("2026-11-03", time = "01:00"), at("2026-11-02T23:00", MADRID), MADRID)
        assertEquals(1L, c.daysUntil)
        assertEquals(Duration.ofHours(2), c.remaining)
    }

    @Test
    fun timedEventIsReachedOnceItsTimePasses() {
        val c = CountdownCalculator.calculate(event("2026-11-02", time = "09:00"), now, MADRID)
        assertEquals(CountdownStatus.REACHED, c.status)
        assertEquals(0L, c.daysSince)
    }

    @Test
    fun multiDayTripUnderWay() {
        val c = CountdownCalculator.calculate(event("2026-11-01", end = "2026-11-05"), now, MADRID)
        assertEquals(CountdownStatus.IN_PROGRESS, c.status)
    }

    @Test
    fun repeatingEventIsNeverReached() {
        val c = CountdownCalculator.calculate(event("2020-10-30", repeat = RepeatRule.YEARLY), now, MADRID)
        assertEquals(CountdownStatus.UPCOMING, c.status)
        assertEquals(362L, c.daysUntil)
    }

    @Test
    fun daysUntilUsesViewerCalendar() {
        // 09:00 in Paris on Nov 20 is 17:00 the same day in Tokyo.
        val reunion = event("2026-11-20", time = "09:00", zone = PARIS)
        val tokyoNow = at("2026-11-02T23:30", TOKYO)
        assertEquals(18L, CountdownCalculator.calculate(reunion, tokyoNow, TOKYO).daysUntil)
        // Same instant seen from Paris is still Nov 2 15:30.
        assertEquals(18L, CountdownCalculator.calculate(reunion, tokyoNow, PARIS).daysUntil)
    }

    @Test
    fun durationSplitsIntoParts() {
        val parts =
            Duration
                .ofDays(1)
                .plusHours(2)
                .plusMinutes(3)
                .plusSeconds(4)
                .toCountdownParts()
        assertEquals(CountdownParts(days = 1, hours = 2, minutes = 3, seconds = 4), parts)
        assertEquals(CountdownParts(0, 0, 0, 0), Duration.ofSeconds(-5).toCountdownParts())
    }
}
