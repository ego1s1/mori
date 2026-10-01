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
        // Symlink defense without NIO (File#toPath is API 26+, min is 24).
        // FileTreeWalk follows links, so: (1) a link cycle would walk
        // forever — prune canonical dirs already visited; (2) a link
        // pointing outside the root would leak external files in — only
        // descend while the canonical path stays inside the canonical root
        // (itself resolved, so a root picked through a symlink mount works).
        // IO errors fail closed (skip descent).
        val rootCanonical = runCatching { root.canonicalPath }.getOrNull() ?: return emptyList()
        val seenCanonical = mutableSetOf<String>()
        val files = root.walkTopDown()
            .onEnter { dir ->
                val canonical = runCatching { dir.canonicalPath }.getOrNull()
                    ?: return@onEnter false
                if (canonical != rootCanonical && !canonical.startsWith(rootCanonical + File.separatorChar)) {
                    return@onEnter false
                }
                // False on revisit: a symlink cycle maps distinct absolute
                // paths onto one canonical dir, so prune the second visit.
                return@onEnter seenCanonical.add(canonical)
            }
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
