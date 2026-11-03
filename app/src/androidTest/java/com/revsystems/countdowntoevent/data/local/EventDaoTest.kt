package com.revsystems.countdowntoevent.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: EventDao

    private fun entity(
        title: String,
        epochDay: Long,
    ) = EventEntity(
        title = title,
        emoji = null,
        startEpochDay = epochDay,
        startSecondOfDay = null,
        endEpochDay = null,
        timeZone = "Europe/Madrid",
        repeat = "NONE",
        backgroundType = "solid",
        backgroundValue = "FF1A9AA6",
        createdAtMillis = 0,
        updatedAtMillis = 0,
        remoteId = null,
        ownerId = null,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.eventDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertObserveUpdateDelete() =
        runTest {
            val later = dao.insert(entity("Later", 20_000))
            val sooner = dao.insert(entity("Sooner", 19_000))

            assertEquals(listOf("Sooner", "Later"), dao.observeAll().first().map { it.title })

            assertEquals(1, dao.update(dao.getById(later)!!.copy(title = "Renamed")))
            assertEquals("Renamed", dao.observeById(later).first()?.title)

            dao.deleteById(sooner)
            assertNull(dao.getById(sooner))
            assertEquals(1, dao.getAll().size)
        }

    @Test
    fun updateOfMissingRowChangesNothing() =
        runTest {
            assertEquals(0, dao.update(entity("Ghost", 1).copy(id = 999)))
        }
}
