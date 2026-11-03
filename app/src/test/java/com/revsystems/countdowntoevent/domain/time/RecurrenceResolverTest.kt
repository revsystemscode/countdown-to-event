package com.revsystems.countdowntoevent.domain.time

import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.testutil.MADRID
import com.revsystems.countdowntoevent.testutil.PARIS
import com.revsystems.countdowntoevent.testutil.TOKYO
import com.revsystems.countdowntoevent.testutil.at
import com.revsystems.countdowntoevent.testutil.date
import com.revsystems.countdowntoevent.testutil.event
import com.revsystems.countdowntoevent.testutil.time
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurrenceResolverTest {
    private fun currentDate(
        start: String,
        repeat: RepeatRule,
        now: String,
        end: String? = null,
    ) = RecurrenceResolver
        .current(event(start, end = end, repeat = repeat), at(now, MADRID), MADRID)
        .start
        .toLocalDate()

    @Test
    fun nonRepeatingPastEventKeepsItsOnlyOccurrence() {
        val occ = RecurrenceResolver.current(event("2020-05-01"), at("2026-11-02T10:00", MADRID), MADRID)
        assertEquals(0L, occ.index)
        assertEquals(date("2020-05-01"), occ.start.toLocalDate())
    }

    @Test
    fun futureStartReturnsFirstOccurrence() {
        assertEquals(date("2027-03-10"), currentDate("2027-03-10", RepeatRule.WEEKLY, "2026-11-02T10:00"))
    }

    @Test
    fun yearlyBirthdayRollsToNextYear() {
        assertEquals(date("2027-01-15"), currentDate("2000-01-15", RepeatRule.YEARLY, "2026-11-02T10:00"))
    }

    @Test
    fun yearlyAllDayEventStaysOnItsDayUntilMidnight() {
        assertEquals(date("2026-11-02"), currentDate("1990-11-02", RepeatRule.YEARLY, "2026-11-02T23:59"))
        assertEquals(date("2027-11-02"), currentDate("1990-11-02", RepeatRule.YEARLY, "2026-11-03T00:00"))
    }

    @Test
    fun monthlyFrom31stClampsWithoutDrifting() {
        assertEquals(date("2026-02-28"), currentDate("2026-01-31", RepeatRule.MONTHLY, "2026-02-10T10:00"))
        assertEquals(date("2026-03-31"), currentDate("2026-01-31", RepeatRule.MONTHLY, "2026-03-01T10:00"))
        assertEquals(date("2026-04-30"), currentDate("2026-01-31", RepeatRule.MONTHLY, "2026-04-01T10:00"))
    }

    @Test
    fun leapDayYearlyFallsOnFeb28InCommonYears() {
        assertEquals(date("2027-02-28"), currentDate("2024-02-29", RepeatRule.YEARLY, "2026-06-01T10:00"))
        assertEquals(date("2028-02-29"), currentDate("2024-02-29", RepeatRule.YEARLY, "2027-06-01T10:00"))
    }

    @Test
    fun weeklyAndBiweekly() {
        // 2026-10-05 is a Monday; 2026-11-02 is four weeks later.
        assertEquals(date("2026-11-02"), currentDate("2026-10-05", RepeatRule.WEEKLY, "2026-11-02T10:00"))
        assertEquals(date("2026-11-09"), currentDate("2026-10-05", RepeatRule.WEEKLY, "2026-11-03T10:00"))
        assertEquals(date("2026-11-02"), currentDate("2026-10-05", RepeatRule.BIWEEKLY, "2026-11-02T10:00"))
        assertEquals(date("2026-11-16"), currentDate("2026-10-05", RepeatRule.BIWEEKLY, "2026-11-03T10:00"))
    }

    @Test
    fun dailyTimedEventMovesToTomorrowOnceTimePasses() {
        val e = event("2026-01-01", time = "08:00", repeat = RepeatRule.DAILY)
        val later = RecurrenceResolver.current(e, at("2026-11-02T09:00", MADRID), MADRID)
        assertEquals(at("2026-11-03T08:00", MADRID), later.start.toInstant())
        val before = RecurrenceResolver.current(e, at("2026-11-02T07:59", MADRID), MADRID)
        assertEquals(at("2026-11-02T08:00", MADRID), before.start.toInstant())
    }

    @Test
    fun multiDayRepeatingEventIsCurrentWhileUnderWay() {
        // Weekend trip Fri-Sun every week; on Saturday the current one is still this weekend.
        assertEquals(
            date("2026-10-30"),
            currentDate("2026-10-02", RepeatRule.WEEKLY, "2026-10-31T12:00", end = "2026-10-04"),
        )
        assertEquals(
            date("2026-11-06"),
            currentDate("2026-10-02", RepeatRule.WEEKLY, "2026-11-02T00:00", end = "2026-10-04"),
        )
    }

    @Test
    fun timedEventIsSameInstantForViewersInOtherZones() {
        val reunion = event("2026-11-20", time = "09:00", zone = PARIS)
        val fromTokyo = RecurrenceResolver.current(reunion, at("2026-11-02T10:00", TOKYO), TOKYO)
        val fromParis = RecurrenceResolver.current(reunion, at("2026-11-02T10:00", TOKYO), PARIS)
        assertEquals(fromParis.start.toInstant(), fromTokyo.start.toInstant())
        assertEquals(at("2026-11-20T09:00", PARIS), fromTokyo.start.toInstant())
    }

    @Test
    fun allDayEventFloatsToViewerZone() {
        val birthday = event("2026-12-01", zone = PARIS)
        val occ = RecurrenceResolver.occurrence(birthday, 0, TOKYO)
        assertEquals(at("2026-12-01T00:00", TOKYO), occ.start.toInstant())
        assertEquals(at("2026-12-02T00:00", TOKYO), occ.end.toInstant())
    }

    @Test
    fun timeInDstGapIsShiftedForward() {
        // Spain jumps 02:00 -> 03:00 on 2027-03-28.
        val e = event("2027-03-28", time = "02:30", zone = MADRID)
        val occ = RecurrenceResolver.occurrence(e, 0, MADRID)
        assertEquals(time("03:30"), occ.start.toLocalTime())
    }

    @Test
    fun timedSingleMomentEndsAtStart() {
        val occ = RecurrenceResolver.occurrence(event("2026-11-20", time = "20:00"), 0, MADRID)
        assertEquals(occ.start, occ.end)
    }

    @Test
    fun farFutureRepeatsAreComputedQuickly() {
        // Daily event started 50 years ago: must not iterate ~18k times per call in hot paths.
        val e = event("1976-01-01", repeat = RepeatRule.DAILY)
        val occ = RecurrenceResolver.current(e, at("2026-11-02T10:00", MADRID), MADRID)
        assertEquals(date("2026-11-02"), occ.start.toLocalDate())
    }
}
