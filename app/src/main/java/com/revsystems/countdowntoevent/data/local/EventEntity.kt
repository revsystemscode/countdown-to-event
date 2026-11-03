package com.revsystems.countdowntoevent.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Stored with plain columns (no type converters) so the schema stays explicit and easy to migrate. */
@Entity(
    tableName = "events",
    indices = [Index("start_epoch_day")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String?,
    @ColumnInfo(name = "start_epoch_day") val startEpochDay: Long,
    /** Seconds after midnight; null for all-day events. */
    @ColumnInfo(name = "start_second_of_day") val startSecondOfDay: Int?,
    @ColumnInfo(name = "end_epoch_day") val endEpochDay: Long?,
    @ColumnInfo(name = "time_zone") val timeZone: String,
    val repeat: String,
    @ColumnInfo(name = "background_type") val backgroundType: String,
    @ColumnInfo(name = "background_value") val backgroundValue: String,
    @ColumnInfo(name = "created_at") val createdAtMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtMillis: Long,
    @ColumnInfo(name = "remote_id") val remoteId: String?,
    @ColumnInfo(name = "owner_id") val ownerId: String?,
)
