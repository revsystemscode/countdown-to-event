package com.revsystems.countdowntoevent.domain.repository

import com.revsystems.countdowntoevent.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>

    fun observeEvent(id: Long): Flow<Event?>

    suspend fun getEvent(id: Long): Event?

    suspend fun getEvents(): List<Event>

    /**
     * Inserts when [Event.id] is 0, otherwise updates. Trims the title and sets timestamps.
     * @return the event id.
     * @throws com.revsystems.countdowntoevent.domain.InvalidEventException when validation fails.
     */
    suspend fun save(event: Event): Long

    suspend fun delete(id: Long)

    /** Re-inserts a just-deleted event with its original id and timestamps (undo). */
    suspend fun restore(event: Event)
}
