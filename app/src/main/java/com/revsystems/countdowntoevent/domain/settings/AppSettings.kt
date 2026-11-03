package com.revsystems.countdowntoevent.domain.settings

import kotlinx.coroutines.flow.Flow
import java.time.Duration

/** How long before an event the reminder fires. One global setting for all events. */
enum class ReminderOffset(
    val duration: Duration?,
) {
    NONE(null),
    MINUTES_15(Duration.ofMinutes(15)),
    HOUR_1(Duration.ofHours(1)),
    DAY_1(Duration.ofDays(1)),
    DAY_2(Duration.ofDays(2)),
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val reminderOffset: ReminderOffset = ReminderOffset.DAY_1,
    /** "Today is the day!" notification when the event arrives. */
    val notifyOnEventDay: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val anyNotificationEnabled: Boolean get() = reminderOffset != ReminderOffset.NONE || notifyOnEventDay
}

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun current(): AppSettings

    suspend fun setReminderOffset(offset: ReminderOffset)

    suspend fun setNotifyOnEventDay(enabled: Boolean)

    suspend fun setThemeMode(mode: ThemeMode)

    /** The user said "not now" to the notification prompt on Home. */
    val notificationPromptDismissed: Flow<Boolean>

    suspend fun dismissNotificationPrompt()

    /** Whether the system permission dialog was already shown once (Android can refuse to show it again). */
    suspend fun notificationPermissionAsked(): Boolean

    suspend fun markNotificationPermissionAsked()
}
