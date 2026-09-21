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
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * A [ComicArchive] backed by a ZIP container (`.cbz` or `.zip`).
 *
 * Uses [ZipFile] for random access: page listing and single-page reads never decompress
 * the whole archive. Pages are discovered from image entries (including those in
 * subdirectories) and ordered naturally.
 */
internal class CbzArchive(
    file: File,
) : ComicArchive {

    override val source: ComicSource.File = ComicSource.File(file)

    private val zipFile: ZipFile = openZip(file)
    private val imageEntries: List<ZipEntry> = loadImageEntries()
    private var closed = false

    override val metadata: ComicMetadata = parseComicInfo()
    override val pages: List<ComicPage> = buildPages()

    init {
        if (imageEntries.isEmpty()) {
            zipFile.close()
            throw EmptyArchiveException("No image entries found in '${source.file.name}'")
        }
    }

    override suspend fun readPage(page: ComicPage): ByteArray = withContext(Dispatchers.IO) {
        ensureOpen()
        val entry = zipFile.getEntry(page.name)
            ?: throw PageNotFoundException("Page '${page.name}' not found in '${source.file.name}'")
        try {
            zipFile.getInputStream(entry).use { it.readBytes() }
        } catch (e: IOException) {
            throw CorruptArchiveException("Failed to read page '${entry.name}' from '${source.file.name}'", e)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        zipFile.close()
    }

    private fun openZip(file: File): ZipFile {
        return try {
            ZipFile(file)
        } catch (e: IOException) {
            throw CorruptArchiveException("Failed to open ZIP archive '${file.name}'", e)
        }
    }

    private fun buildPages(): List<ComicPage> = imageEntries.mapIndexed { index, entry ->
        ComicPage(
            index = index,
            name = entry.name,
            mediaType = MediaType.fromName(entry.name),
            sizeBytes = entry.size,
        )
    }

    private fun loadImageEntries(): List<ZipEntry> {
        val entries = zipEntries()
            .filter { !it.isDirectory }
            .filter { PageEntryNames.isPage(it.name) }
        return entries.sortedWith(compareBy(NaturalSort.comparator) { it.name })
    }

    private fun parseComicInfo(): ComicMetadata {
        val entry = zipEntries()
            .firstOrNull { PageEntryNames.basename(it.name).equals("ComicInfo.xml", ignoreCase = true) }
            ?: return ComicMetadata()
        return runCatching { zipFile.getInputStream(entry).use(ComicInfoParser::parse) }
            .getOrElse { ComicMetadata() }
    }

    private fun zipEntries(): List<ZipEntry> {
        val list = ArrayList<ZipEntry>()
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            list.add(entries.nextElement())
        }
        return list
    }

    private fun ensureOpen() {
        if (closed) throw ArchiveClosedException("Archive '${source.file.name}' is closed")
    }
}
