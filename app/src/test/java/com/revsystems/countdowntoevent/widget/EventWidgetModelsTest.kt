package com.revsystems.countdowntoevent.widget

import com.revsystems.countdowntoevent.domain.time.CountdownCalculator
import com.revsystems.countdowntoevent.testutil.MADRID
import com.revsystems.countdowntoevent.testutil.at
import com.revsystems.countdowntoevent.testutil.event
import com.revsystems.countdowntoevent.testutil.time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class EventWidgetModelsTest {
    private val now = at("2026-11-02T10:00", MADRID)
    private val soon = event("2026-11-05", title = "Soon", id = 1)
    private val later = event("2026-12-24", title = "Later", id = 2)
    private val past = event("2026-10-01", title = "Past", id = 3)
    private val events = listOf(later, past, soon)

    private fun shown(config: EventWidgetConfig) = EventWidgetModels.build(events, config, now, MADRID) as EventWidgetModel.Showing

    @Test
    fun nextModePicksSoonestUpcoming() {
        assertEquals(1L, shown(EventWidgetConfig()).item.event.id)
    }

    @Test
    fun pinnedEventIsShownEvenWhenReached() {
        assertEquals(3L, shown(EventWidgetConfig(eventId = 3)).item.event.id)
    }

    @Test
    fun deletedPinnedEventFallsBackToNext() {
        assertEquals(1L, shown(EventWidgetConfig(eventId = 99)).item.event.id)
    }

    @Test
    fun emptyWhenNothingUpcoming() {
        assertEquals(EventWidgetModel.Empty, EventWidgetModels.build(listOf(past), EventWidgetConfig(), now, MADRID))
        assertEquals(EventWidgetModel.Empty, EventWidgetModels.build(emptyList(), EventWidgetConfig(), now, MADRID))
    }

    @Test
    fun liveOnlyWhileUpcoming() {
        assertTrue(shown(EventWidgetConfig(eventId = 1, live = true)).live)
        assertFalse(shown(EventWidgetConfig(eventId = 3, live = true)).live)
    }

    @Test
    fun hoursBecomeStaticTimeToday() {
        val e = event("2026-11-02", time = "20:00")
        val badge = EventWidgetModels.badge(e, CountdownCalculator.calculate(e, now, MADRID), MADRID)
        assertEquals(WidgetBadge.TodayAt(time("20:00")), badge)
    }

    @Test
    fun liveSplitKeepsRemainderUnderADay() {
        val split = EventWidgetModels.liveSplit(Duration.ofDays(3).plusHours(5).plusSeconds(7))
        assertEquals(3L, split.days)
        assertEquals(Duration.ofHours(5).plusSeconds(7), split.remainder)
    }

    @Test
    fun refreshAtMidnightWhenNothingSooner() {
        assertEquals(at("2026-11-03T00:00", MADRID), EventWidgetModels.nextRefresh(listOf(later), emptySet(), now, MADRID))
    }

    @Test
    fun refreshWhenTimedEventStartsToday() {
        val concert = event("2026-11-02", time = "20:00", id = 7)
        assertEquals(at("2026-11-02T20:00", MADRID), EventWidgetModels.nextRefresh(listOf(concert), emptySet(), now, MADRID))
    }

    @Test
    fun liveWidgetRefreshesWhenItsDayCountDrops() {
        // 3 days 2 hours left -> the day count drops in 2 hours.
        val trip = event("2026-11-05", time = "12:00", id = 8)
        assertEquals(at("2026-11-02T12:00", MADRID), EventWidgetModels.nextRefresh(listOf(trip), setOf(8L), now, MADRID))
        assertEquals(at("2026-11-03T00:00", MADRID), EventWidgetModels.nextRefresh(listOf(trip), emptySet(), now, MADRID))
    }
}
