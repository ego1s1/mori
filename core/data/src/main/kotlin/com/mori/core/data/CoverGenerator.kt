package com.mori.core.data

import android.content.Context
import android.graphics.Bitmap
import com.mori.comic.model.ComicPage
import com.mori.comic.model.MediaType
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
            val decoded = runCatching {
                backend.decodeCover(bytes, page.mediaType, COVER_MAX_DIMENSION)
            }.getOrNull() ?: return@withContext null
            // Opaque JPEG covers don't need alpha: halve the transient bitmap
            // to RGB_565 before JPEG compress. (Decode-time preferredConfig
            // lives in MoriComicBackendDataSource.decodeCover — out of scope here.)
            val bitmap = if (page.mediaType == MediaType.JPEG && decoded.config != Bitmap.Config.RGB_565) {
                val converted = runCatching { decoded.copy(Bitmap.Config.RGB_565, false) }.getOrNull()
                if (converted != null) {
                    decoded.recycle()
                    converted
                } else {
                    decoded
                }
            } else {
                decoded
            }
            try {
                val coversDir = File(context.filesDir, COVERS_DIR).apply { mkdirs() }
                val dest = File(coversDir, "$comicId.jpg")
                // Atomic write: a concurrent reader never observes a partial
                // cover, and a crash never leaves a torn entry that looks done.
                val tmp = File(coversDir, "$comicId.jpg.part")
                try {
                    FileOutputStream(tmp).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, out)
                    }
                    if (!tmp.renameTo(dest) && !dest.isFile) {
                        runCatching { tmp.delete() }
                        return@withContext null
                    }
                    runCatching { if (tmp.isFile) tmp.delete() }
                    dest.absolutePath
                } finally {
                    runCatching { bitmap.recycle() }
                }
            } catch (e: Exception) {
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
