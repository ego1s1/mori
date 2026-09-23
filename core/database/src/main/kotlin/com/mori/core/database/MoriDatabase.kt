package com.mori.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ComicEntity::class, DisplayFilterOverrideEntity::class, ReadingSessionEntity::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
    exportSchema = true,
)
abstract class MoriDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao

    abstract fun displayFilterDao(): DisplayFilterDao

    abstract fun readingSessionDao(): ReadingSessionDao
}
