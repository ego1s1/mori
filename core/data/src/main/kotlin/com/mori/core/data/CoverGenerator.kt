package com.mori.core.data

import android.content.Context
import android.graphics.Bitmap
import com.mori.comic.model.ComicPage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders and caches cover thumbnails under `filesDir/covers/<comicId>.jpg`.
 *
 * Cover selection mirrors reader expectations: a page whose name contains "cover" wins,
 * otherwise the first natural-sorted page is used.
 */
@Singleton
internal class CoverGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backend: ComicBackendDataSource,
) {
    suspend fun generateCover(
        sourceFile: File,
        comicId: String,
        pages: List<ComicPage>,
    ): String? =
        withContext(Dispatchers.IO) {
            val page = pickCover(pages) ?: return@withContext null
            val bytes = runCatching { backend.readPageBytes(sourceFile, page) }.getOrNull()
                ?: return@withContext null
            val bitmap = runCatching {
                backend.decodeCover(bytes, page.mediaType, COVER_MAX_DIMENSION)
            }.getOrNull() ?: return@withContext null
            try {
                val coversDir = File(context.filesDir, COVERS_DIR).apply { mkdirs() }
                val dest = File(coversDir, "$comicId.jpg")
                FileOutputStream(dest).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, out)
                }
                bitmap.recycle()
                dest.absolutePath
            } catch (e: Exception) {
                runCatching { bitmap.recycle() }
                null
            }
        }

    companion object {
        const val COVERS_DIR = "covers"
        const val COVER_MAX_DIMENSION = 512
        const val COVER_QUALITY = 85

        fun pickCover(pages: List<ComicPage>): ComicPage? {
            if (pages.isEmpty()) return null
            return pages.firstOrNull { it.name.contains("cover", ignoreCase = true) }
                ?: pages.first()
        }
    }
}
