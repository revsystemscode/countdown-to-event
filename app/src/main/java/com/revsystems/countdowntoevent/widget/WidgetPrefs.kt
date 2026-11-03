package com.revsystems.countdowntoevent.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

/** Per-widget settings stored in Glance state. */
object WidgetPrefs {
    private val EVENT_ID = longPreferencesKey("event_id")
    private val LIVE = booleanPreferencesKey("live")

    /** Bumped to force a reload; Glance only recomposes when state changes. */
    val TICK = longPreferencesKey("tick")

    fun config(prefs: Preferences) = EventWidgetConfig(eventId = prefs[EVENT_ID], live = prefs[LIVE] ?: false)

    fun write(
        prefs: MutablePreferences,
        config: EventWidgetConfig,
    ) {
        if (config.eventId == null) prefs.remove(EVENT_ID) else prefs[EVENT_ID] = config.eventId
        prefs[LIVE] = config.live
    }
}
