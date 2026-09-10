package com.mori.core.data

import android.content.Context
import androidx.room.Room
import com.mori.comic.decode.PageDecoder
import com.mori.core.database.ComicDao
import com.mori.core.database.MoriDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindComicsRepository(
        impl: OfflineFirstComicsRepository,
    ): ComicsRepository

    @Binds
    abstract fun bindBackendDataSource(
        impl: MoriComicBackendDataSource,
    ): ComicBackendDataSource

    @Binds
    abstract fun bindComicImporter(
        impl: AppComicImporter,
    ): ComicImporter
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataProviders {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoriDatabase =
        Room.databaseBuilder(context, MoriDatabase::class.java, "mori.db").build()

    @Provides
    @Singleton
    fun provideComicDao(database: MoriDatabase): ComicDao = database.comicDao()

    @Provides
    @Singleton
    fun providePageDecoder(): PageDecoder = PageDecoder()

    @Provides
    @Singleton
    fun provideLibraryDir(@ApplicationContext context: Context): File =
        File(context.filesDir, OfflineFirstComicsRepository.LIBRARY_DIR)
}
