package com.revsystems.countdowntoevent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema history lives in app/schemas. When changing an entity: bump [version] and add
 * `autoMigrations = [AutoMigration(from = N, to = N + 1)]` (or a manual Migration) — never destructive.
 */
@Database(
    entities = [EventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        const val NAME = "countdown.db"
    }
}
