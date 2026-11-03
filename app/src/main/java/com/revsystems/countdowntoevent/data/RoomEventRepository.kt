package com.revsystems.countdowntoevent.data

import com.revsystems.countdowntoevent.data.local.EventDao
import com.revsystems.countdowntoevent.data.local.toDomain
import com.revsystems.countdowntoevent.data.local.toEntity
import com.revsystems.countdowntoevent.domain.EventValidator
import com.revsystems.countdowntoevent.domain.InvalidEventException
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEventRepository
    @Inject
    constructor(
        private val dao: EventDao,
        private val clock: Clock,
    ) : EventRepository {
        override fun observeEvents(): Flow<List<Event>> = dao.observeAll().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

        override fun observeEvent(id: Long): Flow<Event?> = dao.observeById(id).map { it?.toDomain() }.distinctUntilChanged()

        override suspend fun getEvent(id: Long): Event? = dao.getById(id)?.toDomain()

        override suspend fun getEvents(): List<Event> = dao.getAll().map { it.toDomain() }

        override suspend fun save(event: Event): Long {
            val cleaned = event.copy(title = event.title.trim(), emoji = event.emoji?.trim()?.ifEmpty { null })
            val errors = EventValidator.validate(cleaned)
            if (errors.isNotEmpty()) throw InvalidEventException(errors)

            val now = clock.instant()
            if (cleaned.id == 0L) {
                return dao.insert(cleaned.copy(createdAt = now, updatedAt = now).toEntity())
            }
            val existing = dao.getById(cleaned.id) ?: error("Event ${cleaned.id} does not exist")
            dao.update(cleaned.copy(createdAt = existing.toDomain().createdAt, updatedAt = now).toEntity())
            return cleaned.id
        }

        override suspend fun delete(id: Long) = dao.deleteById(id)

        override suspend fun restore(event: Event) {
            require(event.id != 0L) { "Only saved events can be restored" }
            dao.insert(event.toEntity())
        }
    }
