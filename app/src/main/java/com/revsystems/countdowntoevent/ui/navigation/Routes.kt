package com.revsystems.countdowntoevent.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

/** Create a new event when [eventId] is null, otherwise edit it. */
@Serializable
data class EventEditorRoute(
    val eventId: Long? = null,
)

@Serializable
data class EventDetailRoute(
    val eventId: Long,
)
