package com.mori.app

import android.app.Application
import coil3.SingletonImageLoader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class MoriApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Coil only discovers the app-wide ImageLoader (with our ComicPageFetcher)
        // through an explicit registration; the Hilt binding alone is invisible to it.
        // Without this, reader pages fall back to the default loader and never decode.
        val factory = EntryPointAccessors.fromApplication(
            this,
            ImageLoaderEntryPoint::class.java,
        ).imageLoaderFactory()
        SingletonImageLoader.setSafe(factory)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImageLoaderEntryPoint {
    fun imageLoaderFactory(): SingletonImageLoader.Factory
}
