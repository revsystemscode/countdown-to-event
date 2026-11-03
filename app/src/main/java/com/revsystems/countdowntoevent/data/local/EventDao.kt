package com.revsystems.countdowntoevent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY start_epoch_day, id")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeById(id: Long): Flow<EventEntity?>

    @Query("SELECT * FROM events ORDER BY start_epoch_day, id")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Insert
    suspend fun insert(event: EventEntity): Long

    /** @return number of rows updated (0 if the event no longer exists). */
    @Update
    suspend fun update(event: EventEntity): Int

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
