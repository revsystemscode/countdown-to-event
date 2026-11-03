package com.revsystems.countdowntoevent.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.revsystems.countdowntoevent.domain.settings.AppSettings
import com.revsystems.countdowntoevent.domain.settings.ReminderOffset
import com.revsystems.countdowntoevent.domain.settings.SettingsRepository
import com.revsystems.countdowntoevent.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val REMINDER_OFFSET = stringPreferencesKey("reminder_offset")
private val NOTIFY_ON_DAY = booleanPreferencesKey("notify_on_event_day")
private val THEME_MODE = stringPreferencesKey("theme_mode")
private val PROMPT_DISMISSED = booleanPreferencesKey("notification_prompt_dismissed")
private val PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
private val REMINDERS_CHECKED_UNTIL = longPreferencesKey("reminders_checked_until")

@Singleton
class DataStoreSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override val settings: Flow<AppSettings> =
            dataStore.data
                .map { prefs ->
                    AppSettings(
                        reminderOffset =
                            ReminderOffset.entries.firstOrNull { it.name == prefs[REMINDER_OFFSET] } ?: AppSettings().reminderOffset,
                        notifyOnEventDay = prefs[NOTIFY_ON_DAY] ?: AppSettings().notifyOnEventDay,
                        themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE] } ?: ThemeMode.SYSTEM,
                    )
                }.distinctUntilChanged()

        override suspend fun current(): AppSettings = settings.first()

        override suspend fun setReminderOffset(offset: ReminderOffset) {
            dataStore.edit { it[REMINDER_OFFSET] = offset.name }
        }

        override suspend fun setNotifyOnEventDay(enabled: Boolean) {
            dataStore.edit { it[NOTIFY_ON_DAY] = enabled }
        }

        override suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { it[THEME_MODE] = mode.name }
        }

        override val notificationPromptDismissed: Flow<Boolean> =
            dataStore.data
                .map { it[PROMPT_DISMISSED] ?: false }
                .distinctUntilChanged()

        override suspend fun dismissNotificationPrompt() {
            dataStore.edit { it[PROMPT_DISMISSED] = true }
        }

        override suspend fun notificationPermissionAsked(): Boolean = dataStore.data.first()[PERMISSION_ASKED] ?: false

        override suspend fun markNotificationPermissionAsked() {
            dataStore.edit { it[PERMISSION_ASKED] = true }
        }
    }

/** Internal bookkeeping for the reminder alarm: everything up to this instant has been handled. */
@Singleton
class ReminderStateStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        suspend fun checkedUntil(): Instant? = dataStore.data.first()[REMINDERS_CHECKED_UNTIL]?.let(Instant::ofEpochMilli)

        suspend fun setCheckedUntil(instant: Instant) {
            dataStore.edit { it[REMINDERS_CHECKED_UNTIL] = instant.toEpochMilli() }
        }
    }
