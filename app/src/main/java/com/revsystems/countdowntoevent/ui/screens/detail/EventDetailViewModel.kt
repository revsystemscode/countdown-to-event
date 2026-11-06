package com.revsystems.countdowntoevent.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.revsystems.countdowntoevent.data.images.BackgroundImageStore
import com.revsystems.countdowntoevent.data.settings.LockScreenStore
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import com.revsystems.countdowntoevent.domain.settings.SettingsRepository
import com.revsystems.countdowntoevent.domain.time.Countdown
import com.revsystems.countdowntoevent.domain.time.CountdownCalculator
import com.revsystems.countdowntoevent.notifications.LockScreenController
import com.revsystems.countdowntoevent.ui.navigation.EventDetailRoute
import com.revsystems.countdowntoevent.ui.util.tickerFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

sealed interface DetailUiState {
    data object Loading : DetailUiState

    /** Deleted, or never existed: the screen closes. */
    data object Missing : DetailUiState

    data class Loaded(
        val event: Event,
        val countdown: Countdown,
    ) : DetailUiState
}

@HiltViewModel
class EventDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: EventRepository,
        private val imageStore: BackgroundImageStore,
        private val lockScreenStore: LockScreenStore,
        private val lockScreen: LockScreenController,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val eventId = savedStateHandle.toRoute<EventDetailRoute>().eventId

        val uiState: StateFlow<DetailUiState> =
            combine(repository.observeEvent(eventId), tickerFlow(SECOND_MILLIS, clock)) { event, now ->
                if (event == null) {
                    DetailUiState.Missing
                } else {
                    DetailUiState.Loaded(event, CountdownCalculator.calculate(event, now, clock.zone))
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DetailUiState.Loading)

        val pinnedToLockScreen: StateFlow<Boolean> =
            lockScreenStore.pinnedEventId
                .map { it == eventId }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

        /** Pinning replaces whichever event was on the lock screen before. */
        fun setPinnedToLockScreen(pinned: Boolean) {
            viewModelScope.launch {
                if (pinned) lockScreenStore.pin(eventId) else lockScreenStore.unpin(eventId)
                // The store may not change (already pinned, permission just granted), so redraw anyway.
                lockScreen.refresh()
            }
        }

        suspend fun notificationPermissionAsked(): Boolean = settings.notificationPermissionAsked()

        suspend fun markNotificationPermissionAsked() = settings.markNotificationPermissionAsked()

        fun delete() {
            viewModelScope.launch {
                val event = repository.getEvent(eventId) ?: return@launch
                repository.delete(eventId)
                (event.background as? EventBackground.LocalImage)?.let { imageStore.delete(it.uri) }
            }
        }

        private companion object {
            const val SECOND_MILLIS = 1_000L
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
