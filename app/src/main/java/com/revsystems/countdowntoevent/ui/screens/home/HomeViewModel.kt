package com.revsystems.countdowntoevent.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revsystems.countdowntoevent.data.images.BackgroundImageStore
import com.revsystems.countdowntoevent.domain.EventClassifier
import com.revsystems.countdowntoevent.domain.EventCountdown
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import com.revsystems.countdowntoevent.domain.settings.SettingsRepository
import com.revsystems.countdowntoevent.ui.util.tickerFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    /** True when the user has no events at all (first run). */
    val isEmpty: Boolean = false,
    val upcoming: List<EventCountdown> = emptyList(),
    val reached: List<EventCountdown> = emptyList(),
    /** Reminders are on in settings and the user hasn't said "not now" to the prompt. */
    val mayPromptForNotifications: Boolean = false,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val repository: EventRepository,
        private val imageStore: BackgroundImageStore,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) : ViewModel() {
        // Re-classify every minute so cards roll over at midnight and "x hours" stays fresh.
        val uiState: StateFlow<HomeUiState> =
            combine(
                repository.observeEvents(),
                tickerFlow(MINUTE_MILLIS, clock),
                settings.settings,
                settings.notificationPromptDismissed,
            ) { events, now, appSettings, promptDismissed ->
                val classified = EventClassifier.classify(events, now, clock.zone)
                HomeUiState(
                    isLoading = false,
                    isEmpty = events.isEmpty(),
                    upcoming = classified.upcoming,
                    reached = classified.reached,
                    mayPromptForNotifications = appSettings.anyNotificationEnabled && !promptDismissed,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

        init {
            // Photos left behind by cancelled edits or deletes from a previous session.
            viewModelScope.launch {
                imageStore.deleteUnused(repository.getEvents().mapNotNull { it.imageUri() }.toSet())
            }
        }

        fun delete(event: Event) {
            viewModelScope.launch { repository.delete(event.id) }
        }

        fun restore(event: Event) {
            viewModelScope.launch { repository.restore(event) }
        }

        fun dismissNotificationPrompt() {
            viewModelScope.launch { settings.dismissNotificationPrompt() }
        }

        suspend fun notificationPermissionAsked() = settings.notificationPermissionAsked()

        suspend fun markNotificationPermissionAsked() = settings.markNotificationPermissionAsked()

        /** Called once the undo window has passed. */
        fun discardDeleted(event: Event) {
            val uri = event.imageUri() ?: return
            viewModelScope.launch { imageStore.delete(uri) }
        }

        private fun Event.imageUri() = (background as? EventBackground.LocalImage)?.uri

        private companion object {
            const val MINUTE_MILLIS = 60_000L
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
