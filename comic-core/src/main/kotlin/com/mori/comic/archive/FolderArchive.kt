package com.mori.comic.archive

import com.mori.comic.ArchiveClosedException
import com.mori.comic.ComicArchive
import com.mori.comic.ComicSource
import com.mori.comic.CorruptArchiveException
import com.mori.comic.EmptyArchiveException
import com.mori.comic.PageNotFoundException
import com.mori.comic.metadata.ComicInfoParser
import com.mori.comic.model.ComicMetadata
import com.mori.comic.model.ComicPage
import com.mori.comic.model.MediaType
import com.mori.comic.util.NaturalSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * A [ComicArchive] backed by a directory of loose image files.
 *
 * Files are discovered recursively and ordered naturally by their path relative to the
 * directory root. A `ComicInfo.xml` at the directory root is parsed when present.
 */
internal class FolderArchive(
    directory: File,
) : ComicArchive {

    override val source: ComicSource.Directory = ComicSource.Directory(directory)

    private val root: File = directory
    private val imageFiles: List<File> = loadImageFiles()
    private var closed = false

    override val metadata: ComicMetadata = parseComicInfo()
    override val pages: List<ComicPage> = buildPages()

    init {
        if (imageFiles.isEmpty()) {
            throw EmptyArchiveException("No image files found in '${root.name}'")
        }
    }

    override suspend fun readPage(page: ComicPage): ByteArray = withContext(Dispatchers.IO) {
        ensureOpen()
        val file = imageFiles.getOrNull(page.index)
            ?.takeIf { relativePath(it) == page.name }
            ?: throw PageNotFoundException("Page '${page.name}' not found in '${root.name}'")
        try {
            file.readBytes()
        } catch (e: IOException) {
            throw CorruptArchiveException("Failed to read page '${page.name}' from '${root.name}'", e)
        }
    }

    override fun close() {
        closed = true
    }

    private fun buildPages(): List<ComicPage> = imageFiles.mapIndexed { index, file ->
        ComicPage(
            index = index,
            name = relativePath(file),
            mediaType = MediaType.fromName(file.name),
            sizeBytes = file.length(),
        )
    }

    private fun loadImageFiles(): List<File> {
        val files = root.walkTopDown()
            .filter { it.isFile }
            .filter { PageEntryNames.isPage(it.name) }
            .toList()
        return files.sortedWith(compareBy(NaturalSort.comparator) { relativePath(it) })
    }

    private fun parseComicInfo(): ComicMetadata {
        val comicInfo = File(root, "ComicInfo.xml")
        if (!comicInfo.isFile) return ComicMetadata()
        return runCatching { comicInfo.inputStream().use(ComicInfoParser::parse) }
            .getOrElse { ComicMetadata() }
    }

    private fun relativePath(file: File): String = file.relativeTo(root).path

    private fun ensureOpen() {
        if (closed) throw ArchiveClosedException("Archive '${root.name}' is closed")
    }
}
