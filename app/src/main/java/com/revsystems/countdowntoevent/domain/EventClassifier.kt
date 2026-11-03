package com.revsystems.countdowntoevent.domain

import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.domain.time.CountdownCalculator
import com.revsystems.countdowntoevent.domain.time.CountdownStatus
import java.time.Instant
import java.time.ZoneId

data class EventCountdown(
    val event: Event,
    val countdown: Countdown,
)

data class ClassifiedEvents(
    /** Not started or in progress, soonest first. Repeating events always land here. */
    val upcoming: List<EventCountdown>,
    /** Finished, most recent first. */
    val reached: List<EventCountdown>,
)

object EventClassifier {
    fun classify(
        events: List<Event>,
        now: Instant,
        viewerZone: ZoneId,
    ): ClassifiedEvents {
        val (reached, upcoming) =
            events
                .map { EventCountdown(it, CountdownCalculator.calculate(it, now, viewerZone)) }
                .partition { it.countdown.status == CountdownStatus.REACHED }

        return ClassifiedEvents(
            upcoming =
                upcoming.sortedWith(
                    compareBy<EventCountdown> {
                        it.countdown.occurrence.start
                            .toInstant()
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.event.title },
                ),
            reached =
                reached.sortedWith(
                    compareByDescending<EventCountdown> {
                        it.countdown.occurrence.end
                            .toInstant()
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.event.title },
                ),
        )
    }
}
