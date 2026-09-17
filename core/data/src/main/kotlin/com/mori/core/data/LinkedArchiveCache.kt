package com.mori.core.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
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

    /**
     * Per-entry locks: concurrent page-fetch plus cover-gen for the same
     * document serialize instead of double-copying, and different books
     * still materialize in parallel.
     */
    private val entryLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun materialize(uri: Uri, displayName: String): File = withContext(Dispatchers.IO) {
        // Only SAF documents are materialized: anything restored from backup
        // (or otherwise unexpected) that is not a content URI reads as a
        // vanished document instead of being queried blindly. Revoked grants
        // surface as IOException so Coil shows its error placeholder rather
        // than crashing on the unchecked SecurityException.
        if (uri.scheme != "content") throw IOException("Not a document: $displayName")
        val doc = try {
            DocumentFile.fromSingleUri(context, uri)
        } catch (e: SecurityException) {
            throw IOException("Grant revoked: $displayName", e)
        } ?: throw IOException("Unable to open $displayName")
        val size = doc.length()
        val modified = doc.lastModified()
        val dest = File(cacheDir, cacheName(uri, displayName, size, modified))
        val lock = entryLocks.getOrPut(dest.name) { Mutex() }
        try {
            lock.withLock {
                if (!dest.isFile) {
                    evictToFit(size)
                    val stream = try {
                        context.contentResolver.openInputStream(uri)
                    } catch (e: SecurityException) {
                        throw IOException("Grant revoked: $displayName", e)
                    } ?: throw IOException("Unable to open $displayName")
                    // Copy to a temp sibling and rename: a concurrent reader
                    // never observes a partial file, and a crash never leaves
                    // a torn entry that looks complete. The copy is capped:
                    // a hostile provider must not stream unbounded bytes to
                    // disk, and eviction accounts actual bytes written.
                    val tmp = File(cacheDir, dest.name + ".part")
                    var written = 0L
                    try {
                        stream.use { input ->
                            FileOutputStream(tmp).use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    written += read
                                    if (written > MATERIALIZE_BYTE_CAP) {
                                        throw IOException("$displayName exceeds size cap")
                                    }
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                        tmp.setLastModified(System.currentTimeMillis())
                        if (!tmp.renameTo(dest) && !dest.isFile) {
                            throw IOException("Unable to cache $displayName")
                        }
                        evictToFit(written)
                    } finally {
                        if (tmp.isFile && !dest.isFile) runCatching { tmp.delete() }
                    }
                    dest.setLastModified(System.currentTimeMillis())
                } else {
                    // LRU touch: recently read archives survive eviction.
                    dest.setLastModified(System.currentTimeMillis())
                }
            }
        } finally {
            // Mutexes accumulate to library size otherwise; drop the idle
            // one (remove only if it is still ours).
            entryLocks.remove(dest.name, lock)
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

        /**
         * Hard ceiling per materialized document: a hostile or broken
         * provider streaming past this aborts instead of filling disk.
         * Far above any plausible book (2 GiB).
         */
        const val MATERIALIZE_BYTE_CAP = 2L * 1024 * 1024 * 1024
    }
}

/** Linked rows address documents by URI; app rows by absolute file path. */
internal fun isLinkedSourcePath(sourcePath: String): Boolean =
    sourcePath.startsWith("content://")
