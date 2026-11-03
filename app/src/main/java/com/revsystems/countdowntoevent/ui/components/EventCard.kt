package com.revsystems.countdowntoevent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.domain.time.CountdownCalculator
import com.revsystems.countdowntoevent.ui.format.CountdownBadge
import com.revsystems.countdowntoevent.ui.format.badgeFor
import com.revsystems.countdowntoevent.ui.format.formatEventDates
import com.revsystems.countdowntoevent.ui.format.labelRes
import com.revsystems.countdowntoevent.ui.format.titleWithEmoji
import com.revsystems.countdowntoevent.ui.theme.CountdownNumberStyle
import com.revsystems.countdowntoevent.ui.theme.CountdownTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val OnCard = Color.White
private val CARD_MIN_HEIGHT = 184.dp

/** The big number is already huge; past 1.5x it would squeeze the title to nothing. */
private const val MAX_NUMBER_FONT_SCALE = 1.5f

@Composable
fun EventCard(
    event: Event,
    countdown: Countdown,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                // Grows with large font sizes instead of clipping.
                .heightIn(min = CARD_MIN_HEIGHT)
                .clip(MaterialTheme.shapes.large)
                .then(
                    if (onClick != null) {
                        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    } else {
                        Modifier
                    },
                ).semantics { if (accessibilityActions.isNotEmpty()) customActions = accessibilityActions },
    ) {
        EventBackgroundView(event.background, Modifier.matchParentSize())
        event.background.scrim()?.let { Box(Modifier.matchParentSize().background(it)) }

        // This Column sizes the card (the layers above only match it).
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = CARD_MIN_HEIGHT).padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = titleWithEmoji(event),
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnCard,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatEventDates(event, countdown, locale, zone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnCard.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                CountdownBadgeView(badgeFor(event, countdown))
            }

            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (event.repeat != RepeatRule.NONE) RepeatPill(event.repeat)
                Spacer(Modifier.weight(1f))
                if (onShare != null) {
                    IconButton(
                        onClick = onShare,
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = OnCard,
                            ),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share))
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownBadgeView(badge: CountdownBadge) {
    Column(horizontalAlignment = Alignment.End) {
        when (badge) {
            is CountdownBadge.Days -> NumberWithUnit(badge.count, R.plurals.unit_days)
            is CountdownBadge.Hours -> NumberWithUnit(badge.count, R.plurals.unit_hours)
            is CountdownBadge.Minutes -> NumberWithUnit(badge.count, R.plurals.unit_minutes)
            is CountdownBadge.DaysAgo -> NumberWithUnit(badge.count, R.plurals.unit_days_ago)
            CountdownBadge.Today -> BadgeLabel(stringResource(R.string.badge_today))
            CountdownBadge.Ongoing -> BadgeLabel(stringResource(R.string.badge_ongoing))
        }
    }
}

@Composable
private fun NumberWithUnit(
    count: Long,
    unitPlural: Int,
) {
    val fontScale = LocalDensity.current.fontScale
    val clamp = minOf(fontScale, MAX_NUMBER_FONT_SCALE) / fontScale
    Text(
        text = count.toString(),
        style =
            CountdownNumberStyle.copy(
                fontSize = CountdownNumberStyle.fontSize * clamp,
                lineHeight =
                    CountdownNumberStyle.lineHeight * clamp,
            ),
        color = OnCard,
    )
    Text(
        text = pluralStringResource(unitPlural, count.toInt()),
        style = MaterialTheme.typography.titleMedium,
        color = OnCard,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun BadgeLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineMedium, color = OnCard)
}

@Composable
private fun RepeatPill(repeat: RepeatRule) {
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Outlined.Repeat, contentDescription = null, tint = OnCard, modifier = Modifier.size(16.dp))
        Text(stringResource(repeat.labelRes()), style = MaterialTheme.typography.labelMedium, color = OnCard)
    }
}

@Preview
@Composable
private fun EventCardPreview() {
    val event =
        Event(
            title = "Lise's birthday",
            emoji = "🎂",
            startDate = LocalDate.now().plusDays(66),
            repeat = RepeatRule.YEARLY,
            background = EventBackground.Preset("sunset"),
        )
    CountdownTheme {
        EventCard(
            event = event,
            countdown = CountdownCalculator.calculate(event, Instant.now(), ZoneId.systemDefault()),
            onShare = {},
        )
    }
}
