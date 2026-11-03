package com.revsystems.countdowntoevent.domain

import com.revsystems.countdowntoevent.domain.model.Event

enum class EventValidationError {
    BLANK_TITLE,
    TITLE_TOO_LONG,
    END_BEFORE_START,
}

object EventValidator {
    const val MAX_TITLE_LENGTH = 60

    fun validate(event: Event): Set<EventValidationError> =
        buildSet {
            val title = event.title.trim()
            if (title.isEmpty()) add(EventValidationError.BLANK_TITLE)
            if (title.codePointCount(0, title.length) > MAX_TITLE_LENGTH) add(EventValidationError.TITLE_TOO_LONG)
            if (event.endDate != null && event.endDate < event.startDate) add(EventValidationError.END_BEFORE_START)
        }
}

class InvalidEventException(
    val errors: Set<EventValidationError>,
) : IllegalArgumentException("Invalid event: $errors")
