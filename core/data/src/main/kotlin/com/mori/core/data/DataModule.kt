package com.mori.core.data

import com.mori.comic.decode.PageDecoder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    abstract fun bindLinkedTreeLister(
        impl: DocumentLinkedTreeLister,
    ): LinkedTreeLister
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataProviders {

    @Provides
    @Singleton
    fun providePageDecoder(): PageDecoder = PageDecoder()
}
