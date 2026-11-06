package com.revsystems.countdowntoevent.notifications

import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.domain.time.CountdownCalculator
import com.revsystems.countdowntoevent.testutil.MADRID
import com.revsystems.countdowntoevent.testutil.at
import com.revsystems.countdowntoevent.testutil.event
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class LockScreenPlannerTest {
    private fun content(
        event: Event,
        now: Instant,
    ) = LockScreenPlanner.content(event, CountdownCalculator.calculate(event, now, MADRID))

    private fun nextRefresh(
        event: Event,
        now: Instant,
    ) = LockScreenPlanner.nextRefresh(event, CountdownCalculator.calculate(event, now, MADRID), now, MADRID)

    @Test
    fun daysWhileMoreThanADayAway() {
        val trip = event("2026-11-12", time = "20:00")
        assertEquals(LockScreenContent.Days(10), content(trip, at("2026-11-02T10:00", MADRID)))
    }

    @Test
    fun liveCountdownInTheLastDay() {
        val concert = event("2026-11-03", time = "09:00")
        assertEquals(
            LockScreenContent.Live(at("2026-11-03T09:00", MADRID)),
            content(concert, at("2026-11-02T10:00", MADRID)),
        )
    }

    @Test
    fun allDayEventTomorrowShowsOneDayNotLive() {
        val birthday = event("2026-11-03")
        assertEquals(LockScreenContent.Days(1), content(birthday, at("2026-11-02T22:00", MADRID)))
    }

    @Test
    fun todayForAllDayAndOngoingEvents() {
        val now = at("2026-11-02T10:00", MADRID)
        assertEquals(LockScreenContent.Today(ongoing = false), content(event("2026-11-02"), now))
        assertEquals(LockScreenContent.Today(ongoing = true), content(event("2026-11-01", end = "2026-11-04"), now))
    }

    @Test
    fun timedEventStaysUntilMidnightThenGoes() {
        val meeting = event("2026-11-02", time = "09:00")
        assertEquals(LockScreenContent.Today(ongoing = false), content(meeting, at("2026-11-02T18:00", MADRID)))
        assertEquals(LockScreenContent.Gone, content(meeting, at("2026-11-03T00:01", MADRID)))
    }

    @Test
    fun repeatingEventMovesToNextOccurrence() {
        val weekly = event("2026-10-26", repeat = RepeatRule.WEEKLY)
        assertEquals(LockScreenContent.Days(6), content(weekly, at("2026-11-03T10:00", MADRID)))
    }

    @Test
    fun refreshesWhenLiveModeStarts() {
        val concert = event("2026-11-05", time = "21:00")
        assertEquals(at("2026-11-04T21:00", MADRID), nextRefresh(concert, at("2026-11-04T10:00", MADRID)))
    }

    @Test
    fun refreshesAtMidnightWhenNothingElseChanges() {
        val trip = event("2026-11-20")
        assertEquals(at("2026-11-03T00:00", MADRID), nextRefresh(trip, at("2026-11-02T10:00", MADRID)))
    }

    @Test
    fun refreshesWhenTheEventStarts() {
        val concert = event("2026-11-02", time = "21:00")
        assertEquals(at("2026-11-02T21:00", MADRID), nextRefresh(concert, at("2026-11-02T10:00", MADRID)))
    }
}
