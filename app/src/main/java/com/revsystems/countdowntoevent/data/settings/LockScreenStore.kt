package com.revsystems.countdowntoevent.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val LOCK_SCREEN_EVENT_ID = longPreferencesKey("lock_screen_event_id")

/** The one event pinned to the lock screen, if any. */
@Singleton
class LockScreenStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val pinnedEventId: Flow<Long?> =
            dataStore.data
                .map { it[LOCK_SCREEN_EVENT_ID] }
                .distinctUntilChanged()

        suspend fun current(): Long? = pinnedEventId.first()

        /** Replaces any previously pinned event. */
        suspend fun pin(eventId: Long) {
            dataStore.edit { it[LOCK_SCREEN_EVENT_ID] = eventId }
        }

        /** Only unpins if [eventId] is still the pinned one. */
        suspend fun unpin(eventId: Long) {
            dataStore.edit { if (it[LOCK_SCREEN_EVENT_ID] == eventId) it.remove(LOCK_SCREEN_EVENT_ID) }
        }

        suspend fun clear() {
            dataStore.edit { it.remove(LOCK_SCREEN_EVENT_ID) }
        }
    }
