package com.mori.core.data

import com.mori.comic.CorruptArchiveException
import com.mori.comic.EmptyArchiveException
import com.mori.comic.PasswordRequiredException
import com.mori.comic.UnsupportedFormatException
import com.mori.core.database.ComicDao
import com.mori.core.database.ComicEntity
import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ComicFormat
import com.mori.core.model.ImportItem
import com.mori.core.model.ImportReport
import com.mori.core.model.ImportStatus
import com.mori.core.model.IndexReport
import com.mori.core.model.LibraryQuery
import com.mori.core.model.StorageUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest

@Singleton
internal class OfflineFirstComicsRepository @Inject constructor(
    private val dao: ComicDao,
    private val backend: ComicBackendDataSource,
    private val covers: CoverGenerator,
    private val linkedCache: LinkedArchiveCache,
    @ApplicationContext private val context: Context,
) : ComicsRepository {

    /**
     * Library rows, mapped/filtered/sorted off the main thread. Room re-emits
     * on every table write (each page turn, each indexed file), so the
     * transform rides [Dispatchers.Default] and [distinctUntilChanged] drops
     * equal lists before they can recompose the grid.
     */
    override fun observeLibrary(query: LibraryQuery): Flow<List<Comic>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toModel() }.applyQuery(query) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override fun observeComic(id: String): Flow<Comic?> =
        dao.observeById(id)
            .map { it?.toModel() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override suspend fun getComic(id: String): Comic? =
        dao.getById(id)?.toModel()

    override suspend fun refreshLibrary(): IndexReport = withContext(Dispatchers.IO) {
        val libraryDir = File(context.filesDir, LIBRARY_DIR).apply { mkdirs() }
        val files = libraryDir.walkTopDown()
            .filter { it.isFile && isSupportedArchive(it.name) }
            .toList()
        // Batch the writes: one upsert per refresh, not one per file. Per-file
        // upserts re-emit observeAll N times (each re-sorting the grid); the
        // batched write emits once with the final state.
        val rows = files.map { file -> indexFile(file) }
        if (rows.isNotEmpty()) {
            dao.upsertAll(rows)
        }
        val indexed = rows.count { it.error == null }
        val failed = rows.size - indexed
        val liveIds = files.map { it.name }.toSet()
        val removed = (dao.getLocalIds().toSet() - liveIds).size
        dao.deleteMissing(liveIds.toList())
        cleanupOrphanCovers(liveIds)
        IndexReport(indexed, failed, removed)
    }

    override suspend fun indexLinkedTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext ImportReport(0, 0, 0, emptyList())
        val docs = collectLinkedArchives(root)
        // Batch like refreshLibrary: one upsert, one observer emission.
        val rows = mutableListOf<ComicEntity>()
        val items = mutableListOf<ImportItem>()
        var failed = 0
        docs.forEachIndexed { index, doc ->
            val uri = doc.uri.toString()
            val name = doc.name ?: uri.substringAfterLast('/')
            try {
                val known = dao.getById(uri)
                val modified = doc.lastModified()
                val row = if (known != null && known.sourceModified == modified &&
                    known.coverPath?.let { File(it).isFile } == true
                ) {
                    known
                } else {
                    val temp = linkedCache.materialize(doc.uri, name)
                    // Titles fall back to the document name; ids/paths stay
                    // URI-addressed so rescans refresh rows instead of dupes.
                    indexFile(temp, existing = known, coverId = linkedCoverId(uri), fallbackTitle = name)
                        .copy(
                            id = uri,
                            sourcePath = uri,
                            sourceDisplayName = name,
                            sourceModified = modified,
                        )
                }
                rows += row
                items += ImportItem(name, ImportStatus.SUCCEEDED, null)
            } catch (e: Exception) {
                failed += 1
                items += ImportItem(name, ImportStatus.FAILED, e.message)
            }
            onProgress(index + 1, docs.size)
        }
        if (rows.isNotEmpty()) {
            dao.upsertAll(rows)
        }
        // Prune books deleted from the tree out from under us.
        val foundIds = rows.map { it.id }
        if (foundIds.isEmpty()) {
            dao.deleteAllLinked()
        } else {
            dao.deleteMissingLinked(foundIds)
        }
        ImportReport(docs.size, docs.size - failed, failed, items)
    }

    override suspend fun refreshComic(id: String): Comic? = withContext(Dispatchers.IO) {
        val row = dao.getById(id) ?: return@withContext null
        if (isLinkedSourcePath(row.sourcePath)) {
            // Linked rows re-materialize transiently; the user original is
            // never touched and progress/bookmarks carry over via `existing`.
            return@withContext runCatching {
                val doc = DocumentFile.fromSingleUri(context, Uri.parse(row.sourcePath))
                    ?: return@withContext null
                val name = doc.name ?: row.sourceDisplayName
                val temp = linkedCache.materialize(doc.uri, name)
                val updated = indexFile(
                    temp,
                    existing = row,
                    coverId = linkedCoverId(row.id),
                    fallbackTitle = name,
                ).copy(
                    id = row.id,
                    sourcePath = row.sourcePath,
                    sourceDisplayName = name,
                    sourceModified = doc.lastModified(),
                )
                dao.upsert(updated)
                updated.toModel()
            }.getOrNull()
        }
        val file = File(row.sourcePath)
        if (!file.isFile) {
            dao.deleteById(id)
            deleteCover(row.coverPath)
            return@withContext null
        }
        val updated = indexFile(file, existing = row)
        dao.upsert(updated)
        updated.toModel()
    }

    override suspend fun removeComic(id: String) = withContext(Dispatchers.IO) {
        val row = dao.getById(id)
        if (row != null) {
            // Linked rows only unlink: the user's original must survive.
            if (!isLinkedSourcePath(row.sourcePath)) {
                runCatching { File(row.sourcePath).delete() }
            }
            deleteCover(row.coverPath)
            dao.deleteById(id)
        }
        Unit
    }

    override suspend fun saveProgress(id: String, pageIndex: Int) {
        val row = dao.getById(id) ?: return
        val clamped = pageIndex.coerceIn(0, (row.pageCount - 1).coerceAtLeast(0))
        dao.updateProgress(id, clamped, System.currentTimeMillis())
    }

    override suspend fun toggleBookmark(id: String) {
        val row = dao.getById(id) ?: return
        dao.updateBookmark(id, !row.bookmarked, System.currentTimeMillis())
    }

    override suspend fun clearThumbnailCache() = withContext(Dispatchers.IO) {
        val coversDir = File(context.filesDir, CoverGenerator.COVERS_DIR)
        runCatching {
            coversDir.listFiles()?.forEach { it.delete() }
        }
        dao.clearCovers()
        Unit
    }

    override suspend fun storageUsage(): StorageUsage = withContext(Dispatchers.IO) {
        val libraryDir = File(context.filesDir, LIBRARY_DIR)
        val coversDir = File(context.filesDir, CoverGenerator.COVERS_DIR)
        StorageUsage(
            comicCount = dao.getIds().size,
            libraryBytes = libraryDir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
            coversBytes = coversDir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
        )
    }

    /**
     * Inspects one file and builds its row WITHOUT writing: callers batch
     * the write ([refreshLibrary] collects every row into one [ComicDao.upsertAll],
     * [refreshComic] upserts its single row) so observers emit once per
     * refresh instead of once per file.
     *
     * Linked rows reuse this with the materialized temp file: [coverId] pins
     * the stable cover name and [fallbackTitle] the document name, since temp
     * file names are cache keys, not titles.
     */
    private suspend fun indexFile(
        file: File,
        existing: ComicEntity? = null,
        coverId: String = file.name,
        fallbackTitle: String? = null,
    ): ComicEntity {
        val now = System.currentTimeMillis()
        val known = existing ?: dao.getById(file.name)
        if (known != null && known.sourceModified == file.lastModified() &&
            known.coverPath?.let { File(it).isFile } == true
        ) {
            return known
        }
        return try {
            val inspected = backend.inspect(file)
            val format = ComicFormat.fromFileName(file.name) ?: ComicFormat.CBZ
            val title = inspected.metadata.title?.takeIf { it.isNotBlank() }
                ?: fallbackTitle ?: file.nameWithoutExtension
            val coverPath = covers.generateCover(file, coverId, inspected.pages)
                ?: known?.coverPath?.takeIf { File(it).isFile }
            val row = ComicEntity(
                id = file.name,
                title = title,
                series = inspected.metadata.series,
                number = inspected.metadata.number,
                format = format.name,
                pageCount = inspected.pages.size,
                sourcePath = file.absolutePath,
                coverPath = coverPath,
                lastPageIndex = known?.lastPageIndex ?: 0,
                sourceDisplayName = file.name,
                sourceModified = file.lastModified(),
                error = null,
                createdAt = known?.createdAt ?: now,
                updatedAt = known?.updatedAt ?: now,
            )
            row
        } catch (e: Exception) {
            val error = mapError(e)
            (known ?: emptyRow(file, now, fallbackTitle)).copy(
                pageCount = 0,
                coverPath = known?.coverPath,
                sourceModified = file.lastModified(),
                error = error.name,
                updatedAt = now,
            )
        }
    }

    private fun emptyRow(file: File, now: Long, fallbackTitle: String? = null): ComicEntity = ComicEntity(
        id = file.name,
        title = fallbackTitle ?: file.nameWithoutExtension,
        series = null,
        number = null,
        format = (ComicFormat.fromFileName(file.name) ?: ComicFormat.CBZ).name,
        pageCount = 0,
        sourcePath = file.absolutePath,
        coverPath = null,
        lastPageIndex = 0,
        sourceDisplayName = file.name,
        sourceModified = file.lastModified(),
        error = ComicError.CORRUPT.name,
        createdAt = now,
        updatedAt = now,
    )

    private fun mapError(e: Exception): ComicError = when (e) {
        is PasswordRequiredException -> ComicError.PASSWORD_REQUIRED
        is EmptyArchiveException -> ComicError.EMPTY
        is UnsupportedFormatException -> ComicError.UNSUPPORTED
        is CorruptArchiveException, is IOException -> ComicError.CORRUPT
        else -> ComicError.CORRUPT
    }

    private fun deleteCover(coverPath: String?) {
        if (coverPath != null) {
            runCatching { File(coverPath).delete() }
        }
    }

    private suspend fun cleanupOrphanCovers(liveIds: Set<String>) {
        val coversDir = File(context.filesDir, CoverGenerator.COVERS_DIR)
        if (!coversDir.isDirectory) return
        coversDir.listFiles()?.forEach { cover ->
            // Linked covers are owned by their source tree, not the app dir.
            if (cover.name.startsWith(LINKED_COVER_PREFIX)) return@forEach
            val id = cover.nameWithoutExtension
            if (id !in liveIds) {
                runCatching { cover.delete() }
            }
        }
    }

    /** Archive documents under a linked tree, depth-first. */
    private fun collectLinkedArchives(root: DocumentFile): List<DocumentFile> {
        val out = mutableListOf<DocumentFile>()
        val stack = ArrayDeque<DocumentFile>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            val children = runCatching { current.listFiles().toList() }.getOrDefault(emptyList())
            children.forEach { child ->
                if (child.isDirectory) {
                    stack.add(child)
                } else {
                    val name = child.name ?: return@forEach
                    if (isSupportedArchive(name)) {
                        out += child
                    }
                }
            }
        }
        return out
    }

    /** Stable cover file key for a linked document URI. */
    private fun linkedCoverId(documentUri: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(documentUri.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(24)
        return "$LINKED_COVER_PREFIX$hash"
    }

    private fun isSupportedArchive(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension == "cbz" || extension == "zip" ||
            extension == "cbr" || extension == "rar"
    }

    companion object {
        const val LIBRARY_DIR = "comics"

        /** Cover filename prefix for linked documents (see [linkedCoverId]). */
        const val LINKED_COVER_PREFIX = "linked-"
    }
}
