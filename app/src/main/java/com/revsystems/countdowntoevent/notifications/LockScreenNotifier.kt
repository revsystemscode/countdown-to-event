package com.revsystems.countdowntoevent.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.ui.format.formatEventDates
import com.revsystems.countdowntoevent.ui.format.titleWithEmoji
import com.revsystems.countdowntoevent.ui.navigation.LaunchRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Draws the silent, ongoing notification that keeps a pinned event on the lock screen. */
@Singleton
class LockScreenNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val reminders: ReminderNotifier,
    ) {
        private val manager = NotificationManagerCompat.from(context)

        fun createChannel() {
            val channel =
                NotificationChannel(
                    CHANNEL_LOCK_SCREEN,
                    context.getString(R.string.channel_lock_screen),
                    // Not LOW: many phones (Pixel by default) hide "silent" notifications on the lock screen.
                    // Sound and vibration are turned off on the channel instead.
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.channel_lock_screen_desc)
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun canPost(): Boolean = reminders.canPost()

        fun show(
            event: Event,
            countdown: Countdown,
            content: LockScreenContent,
        ) {
            if (!canPost() || content == LockScreenContent.Gone) return
            val locale = context.resources.configuration.locales[0]
            val dates = formatEventDates(event, countdown, locale, ZoneId.systemDefault())
            val builder =
                NotificationCompat
                    .Builder(context, CHANNEL_LOCK_SCREEN)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(context.getColor(R.color.launcher_background))
                    .setContentTitle(titleWithEmoji(event))
                    .setContentIntent(openIntent(event.id))
                    // Android 14+ lets users swipe ongoing notifications away when unlocked: treat it as unpinning.
                    .setDeleteIntent(dismissIntent())
                    .setOngoing(true)
                    .setSilent(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setShowWhen(false)
            when (content) {
                is LockScreenContent.Days -> {
                    val days = context.resources.getQuantityString(R.plurals.lock_days_left, content.count.toInt(), content.count)
                    builder.setContentText("$days · $dates")
                }
                is LockScreenContent.Live ->
                    builder
                        .setContentText(context.getString(R.string.lock_starts_at, dates))
                        // The system ticks this down every second without waking the app.
                        .setUsesChronometer(true)
                        .setChronometerCountDown(true)
                        .setWhen(content.start.toEpochMilli())
                        .setShowWhen(true)
                is LockScreenContent.Today ->
                    builder.setContentText(context.getString(if (content.ongoing) R.string.detail_ongoing else R.string.detail_today))
                LockScreenContent.Gone -> return
            }
            try {
                manager.notify(TAG, ID, builder.build())
            } catch (_: SecurityException) {
                // Permission revoked between the check and the call: nothing to show.
            }
        }

        fun cancel() {
            manager.cancel(TAG, ID)
        }

        private fun openIntent(eventId: Long): PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_OPEN,
                LaunchRequest.openEventIntent(context, eventId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun dismissIntent(): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_DISMISS,
                Intent(context, LockScreenReceiver::class.java).setAction(LockScreenReceiver.ACTION_DISMISSED),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        companion object {
            const val CHANNEL_LOCK_SCREEN = "lock_screen_countdown"

            // A tag keeps this ID apart from reminder IDs, which are derived from event IDs.
            private const val TAG = "lock_screen"
            private const val ID = 1

            // Request codes only need to differ from the reminder tap intents, which use their notification IDs.
            private const val REQUEST_OPEN = -1
            private const val REQUEST_DISMISS = 1
        }
    }
