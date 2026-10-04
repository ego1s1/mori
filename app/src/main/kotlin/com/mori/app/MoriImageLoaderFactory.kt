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
 * Performance-optimized:
 * - Decode parallelism scales with available hardware CPU cores on [Dispatchers.Default]
 *   (CPU-bound bitmap decompression) instead of bottlenecking on a fixed 2 threads.
 * - Fetch parallelism operates on [Dispatchers.IO] to handle file and archive reads.
 * - Memory cache holds decoded thumbnails across rapid grid flings with strong references.
 */
class MoriImageLoaderFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pageFetcherFactory: ComicPageFetcher.Factory,
) : SingletonImageLoader.Factory {

    override fun newImageLoader(context: Context): ImageLoader {
        val processorCount = Runtime.getRuntime().availableProcessors()
        val decodeParallelism = (processorCount - 1).coerceIn(4, 8)
        val fetchParallelism = (processorCount * 2).coerceIn(8, 16)

        return ImageLoader.Builder(context)
            .components { add(pageFetcherFactory) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, IMAGE_MEMORY_PERCENT)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "coil").toOkioPath())
                    .maxSizeBytes(IMAGE_DISK_BYTES)
                    .build()
            }
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(fetchParallelism))
            .decoderCoroutineContext(Dispatchers.Default.limitedParallelism(decodeParallelism))
            .crossfade(true)
            .build()
    }

    private companion object {
        const val IMAGE_MEMORY_PERCENT = 0.30
        const val IMAGE_DISK_BYTES = 256L * 1024 * 1024
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
