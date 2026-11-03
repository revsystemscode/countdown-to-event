package com.revsystems.countdowntoevent.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.revsystems.countdowntoevent.data.images.BackgroundImageStore
import com.revsystems.countdowntoevent.domain.EventCountdown
import com.revsystems.countdowntoevent.domain.EventValidationError
import com.revsystems.countdowntoevent.domain.EventValidator
import com.revsystems.countdowntoevent.domain.InvalidEventException
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import com.revsystems.countdowntoevent.domain.time.CountdownCalculator
import com.revsystems.countdowntoevent.ui.navigation.EventEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class EditorUiState(
    val isLoading: Boolean,
    val isNew: Boolean,
    val draft: EventDraft,
    val preview: EventCountdown,
    /** Shown after the first save attempt, then live-updated. */
    val errors: Set<EventValidationError> = emptySet(),
    val isImportingImage: Boolean = false,
    val imageError: Boolean = false,
    val isSaving: Boolean = false,
    /** Set when the screen should close (saved, or the event no longer exists). */
    val isDone: Boolean = false,
)

@HiltViewModel
class EventEditorViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: EventRepository,
        private val imageStore: BackgroundImageStore,
        private val clock: Clock,
    ) : ViewModel() {
        private val eventId: Long? = savedStateHandle.toRoute<EventEditorRoute>().eventId
        private var original: Event? = null

        private val _uiState = MutableStateFlow(initialState())
        val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

        init {
            if (eventId != null) {
                viewModelScope.launch {
                    val event = repository.getEvent(eventId)
                    original = event
                    if (event == null) {
                        _uiState.update { it.copy(isDone = true) }
                    } else {
                        val draft = EventDraft.from(event)
                        _uiState.update { it.copy(isLoading = false, draft = draft, preview = previewOf(draft)) }
                    }
                }
            }
        }

        fun onTitleChange(title: String) = updateDraft { it.copy(title = title) }

        fun onEmojiChange(emoji: String?) = updateDraft { it.copy(emoji = emoji) }

        fun onStartDateChange(date: LocalDate) = updateDraft { it.withStartDate(date) }

        fun onAllDayChange(allDay: Boolean) =
            updateDraft { it.copy(startTime = if (allDay) null else it.startTime ?: DEFAULT_TIME) }

        fun onTimeChange(time: LocalTime) = updateDraft { it.copy(startTime = time) }

        fun onMultiDayChange(multiDay: Boolean) =
            updateDraft { it.copy(endDate = if (multiDay) it.endDate ?: it.startDate.plusDays(1) else null) }

        fun onEndDateChange(date: LocalDate) = updateDraft { it.copy(endDate = date) }

        fun onRepeatChange(repeat: RepeatRule) = updateDraft { it.copy(repeat = repeat) }

        fun onBackgroundChange(background: EventBackground) = updateDraft { it.copy(background = background) }

        fun onImagePicked(source: String) {
            _uiState.update { it.copy(isImportingImage = true, imageError = false) }
            viewModelScope.launch {
                try {
                    val uri = imageStore.import(source)
                    _uiState.update { it.copy(isImportingImage = false) }
                    onBackgroundChange(EventBackground.LocalImage(uri))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Corrupt or unsupported files must not crash the editor.
                    _uiState.update { it.copy(isImportingImage = false, imageError = true) }
                }
            }
        }

        fun save() {
            val state = _uiState.value
            if (state.isSaving || state.isLoading) return
            val event = state.draft.toEvent(original, clock.zone)
            val errors = EventValidator.validate(event.copy(title = event.title.trim()))
            if (errors.isNotEmpty()) {
                _uiState.update { it.copy(errors = errors) }
                return
            }
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                try {
                    repository.save(event)
                    _uiState.update { it.copy(isSaving = false, isDone = true) }
                } catch (e: InvalidEventException) {
                    _uiState.update { it.copy(isSaving = false, errors = e.errors) }
                }
            }
        }

        private fun updateDraft(transform: (EventDraft) -> EventDraft) {
            _uiState.update { state ->
                val draft = transform(state.draft)
                val event = draft.toEvent(original, clock.zone)
                state.copy(
                    draft = draft,
                    preview = previewOf(draft),
                    errors = if (state.errors.isEmpty()) state.errors else EventValidator.validate(event.copy(title = event.title.trim())),
                )
            }
        }

        private fun initialState(): EditorUiState {
            val draft = EventDraft(startDate = LocalDate.now(clock).plusDays(DEFAULT_DAYS_AHEAD))
            return EditorUiState(
                isLoading = eventId != null,
                isNew = eventId == null,
                draft = draft,
                preview = previewOf(draft),
            )
        }

        private fun previewOf(draft: EventDraft): EventCountdown {
            val event = draft.toEvent(original, clock.zone)
            return EventCountdown(event, CountdownCalculator.calculate(event, clock.instant(), clock.zone))
        }

        private companion object {
            val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
            const val DEFAULT_DAYS_AHEAD = 7L
        }
    }
