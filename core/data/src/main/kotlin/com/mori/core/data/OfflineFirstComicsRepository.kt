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
import com.mori.core.model.IndexReport
import com.mori.core.model.LibraryQuery
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context

@Singleton
internal class OfflineFirstComicsRepository @Inject constructor(
    private val dao: ComicDao,
    private val backend: ComicBackendDataSource,
    private val covers: CoverGenerator,
    @ApplicationContext private val context: Context,
) : ComicsRepository {

    override fun observeLibrary(query: LibraryQuery): Flow<List<Comic>> =
        dao.observeAll().map { entities -> entities.map { it.toModel() }.applyQuery(query) }

    override fun observeComic(id: String): Flow<Comic?> =
        dao.observeById(id).map { it?.toModel() }

    override suspend fun getComic(id: String): Comic? =
        dao.getById(id)?.toModel()

    override suspend fun refreshLibrary(): IndexReport = withContext(Dispatchers.IO) {
        val libraryDir = File(context.filesDir, LIBRARY_DIR).apply { mkdirs() }
        val files = libraryDir.walkTopDown()
            .filter { it.isFile && isSupportedArchive(it.name) }
            .toList()
        var indexed = 0
        var failed = 0
        files.forEach { file ->
            val row = indexFile(file)
            if (row.error == null) indexed += 1 else failed += 1
        }
        val liveIds = files.map { it.name }.toSet()
        val removed = (dao.getIds().toSet() - liveIds).size
        dao.deleteMissing(liveIds.toList())
        cleanupOrphanCovers(liveIds)
        IndexReport(indexed, failed, removed)
    }

    override suspend fun refreshComic(id: String): Comic? = withContext(Dispatchers.IO) {
        val row = dao.getById(id) ?: return@withContext null
        val file = File(row.sourcePath)
        if (!file.isFile) {
            dao.deleteById(id)
            deleteCover(row.coverPath)
            return@withContext null
        }
        val updated = indexFile(file, existing = row)
        updated.toModel()
    }

    override suspend fun removeComic(id: String) = withContext(Dispatchers.IO) {
        val row = dao.getById(id)
        if (row != null) {
            runCatching { File(row.sourcePath).delete() }
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

    private suspend fun indexFile(file: File, existing: ComicEntity? = null): ComicEntity {
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
                ?: file.nameWithoutExtension
            val coverPath = covers.generateCover(file, file.name, inspected.pages)
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
            dao.upsert(row)
            row
        } catch (e: Exception) {
            val error = mapError(e)
            val row = (known ?: emptyRow(file, now)).copy(
                pageCount = 0,
                coverPath = known?.coverPath,
                sourceModified = file.lastModified(),
                error = error.name,
                updatedAt = now,
            )
            dao.upsert(row)
            row
        }
    }

    private fun emptyRow(file: File, now: Long): ComicEntity = ComicEntity(
        id = file.name,
        title = file.nameWithoutExtension,
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
            val id = cover.nameWithoutExtension
            if (id !in liveIds) {
                runCatching { cover.delete() }
            }
        }
    }

    private fun isSupportedArchive(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension == "cbz" || extension == "zip" ||
            extension == "cbr" || extension == "rar"
    }

    companion object {
        const val LIBRARY_DIR = "comics"
    }
}
