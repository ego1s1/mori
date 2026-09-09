package com.mori.core.datastore

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesDataSource(
        impl: DataStorePreferencesDataSource,
    ): MoriPreferencesDataSource
}
