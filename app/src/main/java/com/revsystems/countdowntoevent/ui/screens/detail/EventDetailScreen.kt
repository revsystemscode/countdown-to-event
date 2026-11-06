package com.revsystems.countdowntoevent.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.domain.time.CountdownStatus
import com.revsystems.countdowntoevent.domain.time.toCountdownParts
import com.revsystems.countdowntoevent.notifications.LockScreenContent
import com.revsystems.countdowntoevent.notifications.LockScreenPlanner
import com.revsystems.countdowntoevent.ui.components.EventBackgroundView
import com.revsystems.countdowntoevent.ui.components.scrim
import com.revsystems.countdowntoevent.ui.format.formatEventDates
import com.revsystems.countdowntoevent.ui.format.labelRes
import com.revsystems.countdowntoevent.ui.format.titleWithEmoji
import com.revsystems.countdowntoevent.ui.permissions.rememberNotificationAccess
import com.revsystems.countdowntoevent.ui.share.shareEvent
import com.revsystems.countdowntoevent.ui.theme.CountdownNumberStyle
import com.revsystems.countdowntoevent.ui.util.LightStatusBarIcons
import kotlinx.coroutines.launch
import java.time.ZoneId

private val OnImage = Color.White

@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pinned by viewModel.pinnedToLockScreen.collectAsStateWithLifecycle()
    val notificationAccess =
        rememberNotificationAccess(
            wasAskedBefore = viewModel::notificationPermissionAsked,
            onAsked = viewModel::markNotificationPermissionAsked,
        )
    LightStatusBarIcons()

    when (val s = state) {
        DetailUiState.Loading -> Box(Modifier.fillMaxSize().background(Color.Black))
        DetailUiState.Missing -> LaunchedEffect(Unit) { onBack() }
        is DetailUiState.Loaded ->
            EventDetailContent(
                event = s.event,
                countdown = s.countdown,
                onBack = onBack,
                onEdit = { onEdit(s.event.id) },
                onDelete = viewModel::delete,
                // Pinned without permission shows as off: tapping again asks for it.
                pinnedToLockScreen = pinned && notificationAccess.notificationsAllowed,
                onToggleLockScreen = {
                    val show = !(pinned && notificationAccess.notificationsAllowed)
                    viewModel.setPinnedToLockScreen(show)
                    // Pinned first, so it appears as soon as permission is granted (dialog or settings).
                    if (show && !notificationAccess.notificationsAllowed) notificationAccess.requestNotifications()
                },
            )
    }
}

@Composable
fun EventDetailContent(
    event: Event,
    countdown: Countdown,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    pinnedToLockScreen: Boolean = false,
    /** null hides the lock-screen button. */
    onToggleLockScreen: (() -> Unit)? = null,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val iconColors = IconButtonDefaults.iconButtonColors(contentColor = OnImage)

    Box(Modifier.fillMaxSize()) {
        EventBackgroundView(event.background, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(event.background.scrim(strong = true) ?: SolidColor(SolidOverlay)))

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, colors = iconColors) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, colors = iconColors) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                }
                IconButton(onClick = { confirmDelete = true }, colors = iconColors) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = titleWithEmoji(event),
                style = MaterialTheme.typography.headlineLarge,
                color = OnImage,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatEventDates(event, countdown, locale, ZoneId.systemDefault()),
                style = MaterialTheme.typography.titleMedium,
                color = OnImage.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            if (event.repeat != RepeatRule.NONE) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Repeat, contentDescription = null, tint = OnImage)
                    Text(stringResource(event.repeat.labelRes()), style = MaterialTheme.typography.bodyLarge, color = OnImage)
                }
            }

            Spacer(Modifier.height(40.dp))
            CountdownDisplay(event, countdown)

            Spacer(Modifier.weight(1.3f))

            Button(
                onClick = { scope.launch { shareEvent(context, event, countdown) } },
                colors = ButtonDefaults.buttonColors(containerColor = OnImage, contentColor = Color.Black),
                modifier = Modifier.widthIn(min = 200.dp),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Text(stringResource(R.string.action_share), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
            // A one-off event that's over can't be pinned: there's nothing left to count down.
            if (onToggleLockScreen != null && LockScreenPlanner.content(event, countdown) != LockScreenContent.Gone) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onToggleLockScreen,
                    colors = ButtonDefaults.textButtonColors(contentColor = OnImage),
                ) {
                    Icon(if (pinnedToLockScreen) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = null)
                    Text(
                        stringResource(if (pinnedToLockScreen) R.string.lock_screen_hide else R.string.lock_screen_show),
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private val SolidOverlay = Color.Black.copy(alpha = 0.12f)

@Composable
private fun CountdownDisplay(
    event: Event,
    countdown: Countdown,
) {
    when (countdown.status) {
        CountdownStatus.UPCOMING -> {
            val parts = countdown.remaining.toCountdownParts()
            val description =
                listOf(
                    pluralStringResource(R.plurals.a11y_days, parts.days.toInt(), parts.days),
                    pluralStringResource(R.plurals.a11y_hours, parts.hours, parts.hours),
                    pluralStringResource(R.plurals.a11y_minutes, parts.minutes, parts.minutes),
                ).joinToString(", ")
            // Read as one phrase, without announcing every second.
            Row(
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimeUnit(parts.days.toString(), stringResource(R.string.label_days))
                TimeUnit(parts.hours.twoDigits(), stringResource(R.string.label_hours))
                TimeUnit(parts.minutes.twoDigits(), stringResource(R.string.label_minutes))
                TimeUnit(parts.seconds.twoDigits(), stringResource(R.string.label_seconds))
            }
        }
        CountdownStatus.IN_PROGRESS ->
            BigMessage(stringResource(if (event.endDate != null) R.string.detail_ongoing else R.string.detail_today))
        CountdownStatus.REACHED ->
            BigMessage(
                if (countdown.daysSince == 0L) {
                    stringResource(R.string.detail_reached_today)
                } else {
                    pluralStringResource(R.plurals.detail_days_ago, countdown.daysSince.toInt(), countdown.daysSince)
                },
            )
    }
}

private fun Int.twoDigits() = toString().padStart(2, '0')

@Composable
private fun TimeUnit(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 64.dp)) {
        Text(value, style = CountdownNumberStyle.copy(fontSize = 44.sp, lineHeight = 48.sp), color = OnImage)
        Text(label, style = MaterialTheme.typography.labelLarge, color = OnImage.copy(alpha = 0.85f))
    }
}

@Composable
private fun BigMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        color = OnImage,
        textAlign = TextAlign.Center,
    )
}
