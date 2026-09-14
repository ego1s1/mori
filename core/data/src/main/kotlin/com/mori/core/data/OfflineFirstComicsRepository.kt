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

@Singleton
internal class OfflineFirstComicsRepository @Inject constructor(
    private val dao: ComicDao,
    private val backend: ComicBackendDataSource,
    private val covers: CoverGenerator,
    private val linkedCache: LinkedArchiveCache,
    private val treeLister: LinkedTreeLister,
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

    override suspend fun indexLinkedTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport = withContext(Dispatchers.IO) {
        val (docs, walkFailed) = treeLister.listArchives(treeUri)
        // Batch like refreshLibrary: one fetch, one upsert, one emission.
        val knownById = dao.getAll().associateBy { it.id }
        val rows = mutableListOf<ComicEntity>()
        val items = mutableListOf<ImportItem>()
        var failed = 0
        docs.forEachIndexed { index, doc ->
            val uri = doc.uri.toString()
            val name = doc.name
            try {
                val known = knownById[uri]
                val row = if (known != null && known.sourceModified == doc.modified &&
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
                            sourceModified = doc.modified,
                        )
                }
                rows += row
                // indexFile converts backend failures into error rows rather
                // than throwing: count them as failed, like refresh indexing.
                if (row.error == null) {
                    items += ImportItem(name, ImportStatus.SUCCEEDED, null)
                } else {
                    failed += 1
                    items += ImportItem(name, ImportStatus.FAILED, row.error)
                }
            } catch (e: Exception) {
                failed += 1
                items += ImportItem(name, ImportStatus.FAILED, e.message)
            }
            onProgress(index + 1, docs.size)
        }
        if (rows.isNotEmpty()) {
            dao.upsertAll(rows)
        }
        // Prune books deleted from the tree out from under us — but never on
        // a failed walk, which would read as an empty folder and wipe rows
        // the user still owns. Pruned covers go with their rows.
        if (!walkFailed) {
            val foundIds = rows.map { it.id }.toSet()
            val pruned = knownById.values.filter { row ->
                isLinkedSourcePath(row.sourcePath) && row.id !in foundIds
            }
            if (rows.isEmpty() && pruned.isNotEmpty()) {
                dao.deleteAllLinked()
            } else if (pruned.isNotEmpty()) {
                dao.deleteMissingLinked(foundIds.toList())
            }
            pruned.forEach { deleteCover(it.coverPath) }
        }
        ImportReport(docs.size, docs.size - failed, failed, items)
    }

    override suspend fun refreshComic(id: String): Comic? = withContext(Dispatchers.IO) {
        val row = dao.getById(id) ?: return@withContext null
        // Rows address user documents by URI; re-materialize transiently. The
        // user original is never touched; progress/bookmarks carry over via
        // `existing`. A vanished document unlinks the row.
        val doc = treeLister.resolve(Uri.parse(row.sourcePath)) ?: return@withContext null
        return@withContext runCatching {
            val temp = linkedCache.materialize(doc.uri, doc.name)
            val updated = indexFile(
                temp,
                existing = row,
                coverId = linkedCoverId(row.id),
                fallbackTitle = doc.name,
            ).copy(
                id = row.id,
                sourcePath = row.sourcePath,
                sourceDisplayName = doc.name,
                sourceModified = doc.modified,
            )
            dao.upsert(updated)
            updated.toModel()
        }.getOrNull()
    }

    override suspend fun removeComic(id: String) = withContext(Dispatchers.IO) {
        // Unlink only: user originals must survive removal.
        val row = dao.getById(id)
        if (row != null) {
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

    /**
     * Wide-page scan for the dual-page split (Mihon's `isWideImage` gate).
     *
     * One inspect plus one bounds decode per page, all on IO. Encrypted or
     * unreadable archives yield empty — the reader then shows whole pages,
     * the same fallback as split-off.
     */
    override suspend fun widePageIndices(id: String): Set<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val comic = dao.getById(id)?.toModel() ?: return@runCatching emptySet()
            val file = linkedCache.fileFor(comic)
            val inspected = backend.inspect(file)
            buildSet {
                inspected.pages.forEachIndexed { index, page ->
                    val bytes = backend.readPageBytes(file, page)
                    val dimensions = backend.readDimensions(bytes, page.mediaType)
                    if (dimensions.width > dimensions.height) add(index)
                }
            }
        }.getOrDefault(emptySet())
    }

    override suspend fun clearThumbnailCache() = withContext(Dispatchers.IO) {
        val coversDir = File(context.filesDir, CoverGenerator.COVERS_DIR)
        runCatching {
            coversDir.listFiles()?.forEach { it.delete() }
        }
        runCatching {
            File(context.cacheDir, LinkedArchiveCache.LINKED_DIR).listFiles()?.forEach { it.delete() }
        }
        dao.clearCovers()
        Unit
    }

    override suspend fun storageUsage(): StorageUsage = withContext(Dispatchers.IO) {
        // No app-private library exists anymore: comic bytes live in the
        // user's folders; only covers and the transient read cache count.
        val coversDir = File(context.filesDir, CoverGenerator.COVERS_DIR)
        val linkedDir = File(context.cacheDir, LinkedArchiveCache.LINKED_DIR)
        fun dirBytes(dir: File): Long =
            if (dir.isDirectory) {
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else {
                0L
            }
        StorageUsage(
            comicCount = dao.getIds().size,
            libraryBytes = 0L,
            coversBytes = dirBytes(coversDir) + dirBytes(linkedDir),
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

    /** Stable cover file key for a linked document URI. */
    private fun linkedCoverId(documentUri: String): String =
        "$LINKED_COVER_PREFIX${sha256Hex(documentUri).take(24)}"

    companion object {
        /** Cover filename prefix for linked documents (see [linkedCoverId]). */
        const val LINKED_COVER_PREFIX = "linked-"
    }
}
