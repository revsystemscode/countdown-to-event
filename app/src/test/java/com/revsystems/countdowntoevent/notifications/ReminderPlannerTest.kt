package com.revsystems.countdowntoevent.notifications

import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.domain.settings.AppSettings
import com.revsystems.countdowntoevent.domain.settings.ReminderOffset
import com.revsystems.countdowntoevent.testutil.MADRID
import com.revsystems.countdowntoevent.testutil.at
import com.revsystems.countdowntoevent.testutil.event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReminderPlannerTest {
    private val now = at("2026-11-02T10:00", MADRID)
    private val dayBefore = AppSettings(reminderOffset = ReminderOffset.DAY_1, notifyOnEventDay = true)

    private fun next(
        vararg events: Event,
        settings: AppSettings = dayBefore,
        after: Instant = now,
    ) = ReminderPlanner.next(events.toList(), settings, after, MADRID)

    @Test
    fun allDayEventRemindsAtNineTheDayBeforeThenOnTheDay() {
        val birthday = event("2026-11-10")
        assertEquals(at("2026-11-09T09:00", MADRID), next(birthday))
        assertEquals(at("2026-11-10T09:00", MADRID), next(birthday, after = at("2026-11-09T09:00", MADRID)))
        assertNull(next(birthday, after = at("2026-11-10T09:00", MADRID)))
    }

    @Test
    fun timedEventRemindsBeforeStartAndAtStart() {
        val concert = event("2026-11-02", time = "20:00")
        val oneHour = AppSettings(reminderOffset = ReminderOffset.HOUR_1)
        assertEquals(at("2026-11-02T19:00", MADRID), next(concert, settings = oneHour))
        assertEquals(at("2026-11-02T20:00", MADRID), next(concert, settings = oneHour, after = at("2026-11-02T19:00", MADRID)))
    }

    @Test
    fun shortOffsetsAreSkippedForAllDayEvents() {
        val birthday = event("2026-11-10")
        val fifteen = AppSettings(reminderOffset = ReminderOffset.MINUTES_15, notifyOnEventDay = true)
        assertEquals(at("2026-11-10T09:00", MADRID), next(birthday, settings = fifteen))
        val due = ReminderPlanner.due(listOf(birthday), fifteen, now, at("2026-11-10T12:00", MADRID), MADRID)
        assertEquals(listOf(NotificationKind.ARRIVED), due.map { it.kind })
    }

    @Test
    fun nothingWhenAllNotificationsOff() {
        val off = AppSettings(reminderOffset = ReminderOffset.NONE, notifyOnEventDay = false)
        assertNull(next(event("2026-11-10"), settings = off))
    }

    @Test
    fun repeatingEventPlansNextOccurrence() {
        // Yearly birthday already happened this year (Nov 1) -> next year's reminder.
        val birthday = event("1990-11-01", repeat = RepeatRule.YEARLY)
        assertEquals(at("2027-10-31T09:00", MADRID), next(birthday))
    }

    @Test
    fun repeatingDailyTimedEventGoesToTomorrowAfterToday() {
        val standup = event("2026-01-01", time = "09:30", repeat = RepeatRule.DAILY)
        val onDayOnly = AppSettings(reminderOffset = ReminderOffset.NONE, notifyOnEventDay = true)
        assertEquals(at("2026-11-03T09:30", MADRID), next(standup, settings = onDayOnly))
    }

    @Test
    fun dueIncludesArrivalOfTimedEventThatJustStarted() {
        // At 20:00:05 the timed concert's occurrence has ended, but its "it's time" notification is due.
        val concert = event("2026-11-02", time = "20:00", repeat = RepeatRule.WEEKLY)
        val due =
            ReminderPlanner.due(
                listOf(concert),
                AppSettings(reminderOffset = ReminderOffset.NONE),
                at("2026-11-02T19:59", MADRID),
                at("2026-11-02T20:00:05", MADRID),
                MADRID,
            )
        assertEquals(listOf(NotificationKind.ARRIVED), due.map { it.kind })
        assertEquals(at("2026-11-02T20:00", MADRID), due.single().at)
    }

    @Test
    fun dueReturnsBothKindsInWindowOncePerKind() {
        val trip = event("2026-11-03")
        val due = ReminderPlanner.due(listOf(trip), dayBefore, at("2026-11-02T08:00", MADRID), at("2026-11-03T10:00", MADRID), MADRID)
        assertEquals(setOf(NotificationKind.REMINDER, NotificationKind.ARRIVED), due.map { it.kind }.toSet())
        assertEquals(ReminderOffset.DAY_1, due.first { it.kind == NotificationKind.REMINDER }.offset)
    }

    @Test
    fun remindersFromBeforeTheLastEditAreDropped() {
        // Created at 10:00 for tomorrow 09:00 with "2 days before": that reminder time already passed.
        val trip = event("2026-11-03").copy(updatedAt = now)
        val twoDays = AppSettings(reminderOffset = ReminderOffset.DAY_2)
        assertEquals(at("2026-11-03T09:00", MADRID), next(trip, settings = twoDays, after = at("2026-10-01T00:00", MADRID)))
        val due = ReminderPlanner.due(listOf(trip), twoDays, at("2026-10-01T00:00", MADRID), at("2026-11-03T09:30", MADRID), MADRID)
        assertTrue(due.none { it.kind == NotificationKind.REMINDER })
    }

    @Test
    fun picksEarliestAcrossEvents() {
        assertEquals(at("2026-11-04T09:00", MADRID), next(event("2026-12-24"), event("2026-11-05")))
    }
}
