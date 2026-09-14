package com.mori.core.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Materializes SAF documents into a bounded read cache.
 *
 * Linked library rows reference user folders by document URI, which the
 * File-based backend cannot open directly. This store copies a document into
 * `cacheDir/linked` on demand (keyed by identity + size + modified, so an
 * unchanged file is never recopied) and evicts least-recently-used entries
 * past [CACHE_BOUND_BYTES]. The cache holds working copies only — the library
 * itself never duplicates linked files — and anything cached may vanish at
 * any time without data loss.
 */
@Singleton
class LinkedArchiveCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cacheDir = File(context.cacheDir, LINKED_DIR).apply { mkdirs() }

    suspend fun materialize(uri: Uri, displayName: String): File = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromSingleUri(context, uri)
            ?: throw IOException("Unable to open $displayName")
        val size = doc.length()
        val modified = doc.lastModified()
        val dest = File(cacheDir, cacheName(uri, displayName, size, modified))
        if (!dest.isFile) {
            evictToFit(size)
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open $displayName")
            stream.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            dest.setLastModified(System.currentTimeMillis())
        } else {
            // LRU touch: recently read archives survive eviction.
            dest.setLastModified(System.currentTimeMillis())
        }
        dest
    }

    private fun cacheName(uri: Uri, displayName: String, size: Long, modified: Long): String {
        val safe = displayName.replace('/', '_').replace('\\', '_').trim()
            .ifBlank { "comic" }.take(80)
        // Truncated SHA-256, not String.hashCode: 32-bit hashes collide by
        // accident at library scale, and a collision serves the wrong book.
        return "${sha256Hex(uri.toString()).take(16)}-$size-$modified-$safe"
    }

    private fun evictToFit(incoming: Long) {
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total + incoming.coerceAtLeast(0) <= CACHE_BOUND_BYTES) break
            val freed = file.length()
            if (runCatching { file.delete() }.getOrDefault(false)) {
                total -= freed
            }
        }
    }

    internal companion object {
        const val LINKED_DIR = "linked"
        const val CACHE_BOUND_BYTES = 256L * 1024 * 1024
    }
}

/** Linked rows address documents by URI; app rows by absolute file path. */
internal fun isLinkedSourcePath(sourcePath: String): Boolean =
    sourcePath.startsWith("content://")
