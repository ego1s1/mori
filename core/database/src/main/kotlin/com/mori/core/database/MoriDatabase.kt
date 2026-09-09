package com.mori.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ComicEntity::class], version = 1, exportSchema = false)
abstract class MoriDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
}
