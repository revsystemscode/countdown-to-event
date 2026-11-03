package com.revsystems.countdowntoevent.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.revsystems.countdowntoevent.R
import com.revsystems.countdowntoevent.domain.settings.ReminderOffset
import com.revsystems.countdowntoevent.ui.format.titleWithEmoji
import com.revsystems.countdowntoevent.ui.navigation.LaunchRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val manager = NotificationManagerCompat.from(context)

        /** Channels let users silence reminders and keep the event-day alert (or vice versa). */
        fun createChannels() {
            val system = context.getSystemService(NotificationManager::class.java)
            system.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        CHANNEL_REMINDERS,
                        context.getString(R.string.channel_reminders),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply { description = context.getString(R.string.channel_reminders_desc) },
                    NotificationChannel(
                        CHANNEL_EVENT_DAY,
                        context.getString(R.string.channel_event_day),
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply { description = context.getString(R.string.channel_event_day_desc) },
                ),
            )
        }

        fun canPost(): Boolean {
            val permitted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            return permitted && manager.areNotificationsEnabled()
        }

        fun show(item: PlannedNotification) {
            if (!canPost()) return
            val event = item.event
            val id = notificationId(event.id, item.kind)
            val tap =
                PendingIntent.getActivity(
                    context,
                    id,
                    LaunchRequest.openEventIntent(context, event.id),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            val (channel, title, text) =
                when (item.kind) {
                    NotificationKind.REMINDER -> Triple(CHANNEL_REMINDERS, titleWithEmoji(event), reminderText(item))
                    NotificationKind.ARRIVED ->
                        Triple(
                            CHANNEL_EVENT_DAY,
                            context.getString(if (event.isAllDay) R.string.notif_today else R.string.notif_now),
                            titleWithEmoji(event),
                        )
                }
            val notification =
                NotificationCompat
                    .Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(context.getColor(R.color.launcher_background))
                    .setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(tap)
                    .setAutoCancel(true)
                    .setCategory(
                        if (item.kind ==
                            NotificationKind.ARRIVED
                        ) {
                            NotificationCompat.CATEGORY_EVENT
                        } else {
                            NotificationCompat.CATEGORY_REMINDER
                        },
                    ).setWhen(item.at.toEpochMilli())
                    .setShowWhen(true)
                    .build()
            try {
                manager.notify(id, notification)
            } catch (_: SecurityException) {
                // Permission revoked between the check and the call: nothing to show.
            }
        }

        private fun reminderText(item: PlannedNotification): String {
            val locale = context.resources.configuration.locales[0]
            val start = item.occurrence.start.withZoneSameInstant(ZoneId.systemDefault())
            val whenText =
                if (item.event.isAllDay) {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(start)
                } else {
                    DateTimeFormatter
                        .ofLocalizedDateTime(
                            FormatStyle.MEDIUM,
                            FormatStyle.SHORT,
                        ).withLocale(locale)
                        .format(start.toLocalDateTime())
                }
            val lead =
                when (item.offset) {
                    ReminderOffset.MINUTES_15 -> R.string.notif_in_15_min
                    ReminderOffset.HOUR_1 -> R.string.notif_in_1_hour
                    ReminderOffset.DAY_1 -> R.string.notif_tomorrow
                    ReminderOffset.DAY_2 -> R.string.notif_in_2_days
                    ReminderOffset.NONE, null -> null
                }
            return lead?.let { context.getString(it, whenText) } ?: whenText
        }

        private fun notificationId(
            eventId: Long,
            kind: NotificationKind,
        ): Int = (eventId * NotificationKind.entries.size + kind.ordinal).toInt()

        companion object {
            const val CHANNEL_REMINDERS = "reminders"
            const val CHANNEL_EVENT_DAY = "event_day"
        }
    }
