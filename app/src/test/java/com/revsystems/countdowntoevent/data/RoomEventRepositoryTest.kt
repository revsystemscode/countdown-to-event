package com.revsystems.countdowntoevent.data

import com.revsystems.countdowntoevent.data.local.EventDao
import com.revsystems.countdowntoevent.data.local.EventEntity
import com.revsystems.countdowntoevent.domain.EventValidationError
import com.revsystems.countdowntoevent.domain.InvalidEventException
import com.revsystems.countdowntoevent.testutil.MADRID
import com.revsystems.countdowntoevent.testutil.event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class RoomEventRepositoryTest {
    private class FakeDao : EventDao {
        val rows = MutableStateFlow<Map<Long, EventEntity>>(emptyMap())
        private var nextId = 1L

        override fun observeAll(): Flow<List<EventEntity>> = rows.map { it.values.toList() }

        override fun observeById(id: Long): Flow<EventEntity?> = rows.map { it[id] }

        override suspend fun getAll() = rows.value.values.toList()

        override suspend fun getById(id: Long) = rows.value[id]

        override suspend fun insert(event: EventEntity): Long {
            val id = if (event.id != 0L) event.id else nextId++
            rows.value += id to event.copy(id = id)
            return id
        }

        override suspend fun update(event: EventEntity): Int {
            if (event.id !in rows.value) return 0
            rows.value += event.id to event
            return 1
        }

        override suspend fun deleteById(id: Long) {
            rows.value -= id
        }
    }

    private class MutableClock(
        var now: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = MADRID

        override fun withZone(zone: ZoneId): Clock = fixed(now, zone)

        override fun instant(): Instant = now
    }

    private val clock = MutableClock(Instant.parse("2026-11-02T10:00:00Z"))
    private val dao = FakeDao()
    private val repo = RoomEventRepository(dao, clock)

    @Test
    fun insertTrimsAndStampsThenUpdateKeepsCreatedAt() =
        runTest {
            val id = repo.save(event("2026-12-24", title = "  Christmas  ").copy(emoji = " "))
            val created = repo.getEvent(id)!!
            assertEquals("Christmas", created.title)
            assertNull(created.emoji)
            assertEquals(clock.instant(), created.createdAt)

            clock.now += Duration.ofHours(1)
            repo.save(created.copy(title = "Xmas"))
            val updated = repo.getEvent(id)!!
            assertEquals("Xmas", updated.title)
            assertEquals(created.createdAt, updated.createdAt)
            assertEquals(clock.instant(), updated.updatedAt)
        }

    @Test
    fun rejectsInvalidEvent() =
        runTest {
            val error = runCatching { repo.save(event("2026-12-24", title = "")) }.exceptionOrNull()
            assertEquals(setOf(EventValidationError.BLANK_TITLE), (error as InvalidEventException).errors)
            assertEquals(0, repo.getEvents().size)
        }

    @Test
    fun deleteRemovesEvent() =
        runTest {
            val id = repo.save(event("2026-12-24", title = "A"))
            repo.delete(id)
            assertNull(repo.getEvent(id))
        }

    @Test
    fun restoreBringsBackSameIdAndTimestamps() =
        runTest {
            val id = repo.save(event("2026-12-24", title = "A"))
            val saved = repo.getEvent(id)!!
            repo.delete(id)
            clock.now += Duration.ofDays(1)
            repo.restore(saved)
            assertEquals(saved, repo.getEvent(id))
        }
}
