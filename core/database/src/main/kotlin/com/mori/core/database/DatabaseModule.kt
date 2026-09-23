package com.mori.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database graph: the Room instance and its DAOs. Lives in `core:database`
 * (not `core:data`) so storage wiring stays with the schema it builds.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoriDatabase =
        Room.databaseBuilder(context, MoriDatabase::class.java, "mori.db").build()

    @Provides
    @Singleton
    fun provideComicDao(database: MoriDatabase): ComicDao = database.comicDao()

    @Provides
    @Singleton
    fun provideDisplayFilterDao(database: MoriDatabase): DisplayFilterDao =
        database.displayFilterDao()
}
