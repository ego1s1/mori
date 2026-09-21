package com.mori.comic.archive

import com.mori.comic.ArchiveClosedException
import com.mori.comic.ComicArchive
import com.mori.comic.ComicSource
import com.mori.comic.CorruptArchiveException
import com.mori.comic.EmptyArchiveException
import com.mori.comic.PageNotFoundException
import com.mori.comic.PasswordRequiredException
import com.mori.comic.metadata.ComicInfoParser
import com.mori.comic.model.ComicMetadata
import com.mori.comic.model.ComicPage
import com.mori.comic.model.MediaType
import com.mori.comic.util.NaturalSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.IOException

/**
 * A [ComicArchive] backed by a 7-Zip container (`.cb7` or `.7z`).
 *
 * Uses [SevenZFile] for random access: page listing and single-page reads
 * never decompress the whole archive. Pages are discovered from image
 * entries (including those in subdirectories) and ordered naturally.
 * Encrypted archives are refused — Mori has no password entry.
 */
internal class Cb7Archive(
    file: File,
) : ComicArchive {

    override val source: ComicSource.File = ComicSource.File(file)

    private val sevenZFile: SevenZFile = openSevenZ(file)
    private val imageEntries: List<SevenZArchiveEntry> = loadImageEntries()
    private var closed = false

    override val metadata: ComicMetadata = parseComicInfo()
    override val pages: List<ComicPage> = buildPages()

    init {
        if (imageEntries.isEmpty()) {
            sevenZFile.close()
            throw EmptyArchiveException("No image entries found in '${source.file.name}'")
        }
    }

    override suspend fun readPage(page: ComicPage): ByteArray = withContext(Dispatchers.IO) {
        ensureOpen()
        val entry = imageEntries.getOrNull(page.index)?.takeIf { it.name == page.name }
            ?: throw PageNotFoundException("Page '${page.name}' not found in '${source.file.name}'")
        try {
            sevenZFile.getInputStream(entry).use { it.readBytes() }
        } catch (e: org.apache.commons.compress.PasswordRequiredException) {
            throw PasswordRequiredException("Archive '${source.file.name}' is encrypted and requires a password", e)
        } catch (e: IOException) {
            throw CorruptArchiveException("Failed to read page '${entry.name}' from '${source.file.name}'", e)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { sevenZFile.close() }
    }

    private fun openSevenZ(file: File): SevenZFile {
        return try {
            SevenZFile(file)
        } catch (e: org.apache.commons.compress.PasswordRequiredException) {
            throw PasswordRequiredException("Archive '${file.name}' is encrypted and requires a password", e)
        } catch (e: IOException) {
            throw CorruptArchiveException("Failed to open 7-Zip archive '${file.name}'", e)
        }
    }

    private fun buildPages(): List<ComicPage> = imageEntries.mapIndexed { index, entry ->
        ComicPage(
            index = index,
            name = entry.name,
            mediaType = MediaType.fromName(entry.name),
            sizeBytes = entry.size.coerceAtLeast(0L),
        )
    }

    private fun loadImageEntries(): List<SevenZArchiveEntry> {
        return sevenZFile.entries.toList()
            .filter { !it.isDirectory }
            .filter { PageEntryNames.isPage(it.name) }
            .sortedWith(compareBy(NaturalSort.comparator) { it.name })
    }

    private fun parseComicInfo(): ComicMetadata {
        val entry = sevenZFile.entries.toList()
            .firstOrNull { PageEntryNames.basename(it.name).equals("ComicInfo.xml", ignoreCase = true) }
            ?: return ComicMetadata()
        return runCatching { sevenZFile.getInputStream(entry).use(ComicInfoParser::parse) }
            .getOrElse { ComicMetadata() }
    }

    private fun ensureOpen() {
        if (closed) throw ArchiveClosedException("Archive '${source.file.name}' is closed")
    }
}
