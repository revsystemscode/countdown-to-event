package com.revsystems.countdowntoevent.ui.format

import android.content.res.Resources
import androidx.annotation.StringRes
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.domain.time.CountdownStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** What the big number on a card shows. */
sealed interface CountdownBadge {
    data class Days(
        val count: Long,
    ) : CountdownBadge

    data class Hours(
        val count: Long,
    ) : CountdownBadge

    data class Minutes(
        val count: Long,
    ) : CountdownBadge

    data class DaysAgo(
        val count: Long,
    ) : CountdownBadge

    data object Today : CountdownBadge

    /** A multi-day event under way. */
    data object Ongoing : CountdownBadge
}

fun badgeFor(
    event: Event,
    countdown: Countdown,
): CountdownBadge =
    when (countdown.status) {
        CountdownStatus.UPCOMING ->
            when {
                countdown.daysUntil >= 1 -> CountdownBadge.Days(countdown.daysUntil)
                countdown.remaining.toHours() >= 1 -> CountdownBadge.Hours(countdown.remaining.toHours())
                // Round up so "0 minutes" never shows before the event starts.
                else -> CountdownBadge.Minutes((countdown.remaining.seconds + 59) / 60)
            }
        CountdownStatus.IN_PROGRESS -> if (event.endDate != null) CountdownBadge.Ongoing else CountdownBadge.Today
        CountdownStatus.REACHED -> if (countdown.daysSince == 0L) CountdownBadge.Today else CountdownBadge.DaysAgo(countdown.daysSince)
    }

fun titleWithEmoji(event: Event): String = event.emoji?.let { "${event.title} $it" } ?: event.title

/** e.g. "20 nov 2026", "20 nov 2026, 9:00", "1 nov 2026 → 5 nov 2026" for the current occurrence. */
fun formatEventDates(
    event: Event,
    countdown: Countdown,
    locale: Locale,
    viewerZone: ZoneId,
): String {
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val start = countdown.occurrence.start
    val startText =
        if (event.isAllDay) {
            dateFormat.format(start.toLocalDate())
        } else {
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale)
                .format(start.withZoneSameInstant(viewerZone).toLocalDateTime())
        }
    val endDate = event.endDate ?: return startText
    val end = start.toLocalDate().plusDays(ChronoUnit.DAYS.between(event.startDate, endDate))
    return "$startText → ${dateFormat.format(end)}"
}

@StringRes
fun RepeatRule.labelRes(): Int =
    when (this) {
        RepeatRule.NONE -> R.string.repeat_none
        RepeatRule.DAILY -> R.string.repeat_daily
        RepeatRule.WEEKLY -> R.string.repeat_weekly
        RepeatRule.BIWEEKLY -> R.string.repeat_biweekly
        RepeatRule.MONTHLY -> R.string.repeat_monthly
        RepeatRule.YEARLY -> R.string.repeat_yearly
    }

fun shareText(
    resources: Resources,
    event: Event,
    countdown: Countdown,
    locale: Locale,
    viewerZone: ZoneId,
): String {
    val title = titleWithEmoji(event)
    val line =
        when (val badge = badgeFor(event, countdown)) {
            is CountdownBadge.Days -> resources.getQuantityString(R.plurals.share_days_left, badge.count.toInt(), badge.count, title)
            is CountdownBadge.Hours -> resources.getQuantityString(R.plurals.share_hours_left, badge.count.toInt(), badge.count, title)
            is CountdownBadge.Minutes -> resources.getQuantityString(R.plurals.share_minutes_left, badge.count.toInt(), badge.count, title)
            is CountdownBadge.DaysAgo -> resources.getQuantityString(R.plurals.share_days_ago, badge.count.toInt(), badge.count, title)
            CountdownBadge.Today, CountdownBadge.Ongoing -> resources.getString(R.string.share_today, title)
        }
    val dates = formatEventDates(event, countdown, locale, viewerZone)
    return "$line\n$dates\n\n${resources.getString(R.string.share_footer)}"
}
