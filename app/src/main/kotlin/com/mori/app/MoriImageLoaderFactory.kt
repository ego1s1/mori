package com.mori.app

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.mori.core.data.ComicPageFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toOkioPath
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide Coil image loader with the comic page fetcher registered.
 *
 * Covers load from generated thumbnail files through Coil's default fetchers; reader
 * pages load through [ComicPageFetcher] with resolution-bounded backend decoding.
 *
 * Decode/fetch parallelism is capped: Coil otherwise spawns a thread per load,
 * and a fast grid fling would churn threads and the GC into visible stutter.
 */
class MoriImageLoaderFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pageFetcherFactory: ComicPageFetcher.Factory,
) : SingletonImageLoader.Factory {

    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(pageFetcherFactory) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, IMAGE_MEMORY_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "coil").toOkioPath())
                    .maxSizeBytes(IMAGE_DISK_BYTES)
                    .build()
            }
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(IMAGE_FETCH_PARALLELISM))
            .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(IMAGE_DECODE_PARALLELISM))
            .crossfade(true)
            .build()

    private companion object {
        const val IMAGE_MEMORY_PERCENT = 0.25
        const val IMAGE_DISK_BYTES = 256L * 1024 * 1024
        const val IMAGE_FETCH_PARALLELISM = 8
        const val IMAGE_DECODE_PARALLELISM = 3
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CoilModule {

    @Binds
    @Singleton
    abstract fun bindImageLoaderFactory(
        impl: MoriImageLoaderFactory,
    ): SingletonImageLoader.Factory
}
