package com.mori.comic.archive

import com.mori.comic.ArchiveClosedException
import com.mori.comic.ArchiveFormat
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
import com.mori.comic.util.readCapped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarFile
import java.io.File
import java.io.IOException

/**
 * A [ComicArchive] backed by a TAR container (`.cbt` or `.tar`).
 *
 * Uses [TarFile] for random access: page listing and single-page reads never
 * decompress the whole archive. Pages are discovered from image entries
 * (including those in subdirectories) and ordered naturally. Plain TAR has
 * no encryption concept, so no password path exists.
 */
internal class CbtArchive(
    file: File,
) : ComicArchive {

    override val source: ComicSource.File = ComicSource.File(file)

    private val tarFile: TarFile = openTar(file)
    private val imageEntries: List<TarArchiveEntry> = loadImageEntries()
    private var closed = false

    override val metadata: ComicMetadata = parseComicInfo()
    override val pages: List<ComicPage> = buildPages()

    init {
        if (imageEntries.isEmpty()) {
            tarFile.close()
            throw EmptyArchiveException("No image entries found in '${source.file.name}'")
        }
    }

    override suspend fun readPage(page: ComicPage): ByteArray = withContext(Dispatchers.IO) {
        ensureOpen()
        val entry = imageEntries.getOrNull(page.index)?.takeIf { it.name == page.name }
            ?: throw PageNotFoundException("Page '${page.name}' not found in '${source.file.name}'")
        try {
            tarFile.getInputStream(entry).use { it.readCapped(what = "Page '${entry.name}'") }
        } catch (e: IOException) {
            throw CorruptArchiveException("Failed to read page '${entry.name}' from '${source.file.name}'", e)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { tarFile.close() }
    }

    private fun openTar(file: File): TarFile {
        // TarFile tolerates garbage as an empty listing; reject non-tar
        // bytes up front so corrupt files read corrupt, not empty.
        // (read(), not readNBytes(): the latter needs API 33+.)
        val header = ByteArray(TAR_HEADER_SIZE)
        val read = runCatching {
            file.inputStream().use { it.read(header) }
        }.getOrDefault(-1)
        if (read < TAR_HEADER_SIZE || !ArchiveFormat.looksLikeTar(header)) {
            throw CorruptArchiveException("Not a TAR archive: '${file.name}'")
        }
        return try {
            TarFile(file)
        } catch (e: IOException) {
            throw CorruptArchiveException("Failed to open TAR archive '${file.name}'", e)
        }
    }

    private companion object {
        const val TAR_HEADER_SIZE = 512
    }

    private fun buildPages(): List<ComicPage> = imageEntries.mapIndexed { index, entry ->
        ComicPage(
            index = index,
            name = entry.name,
            mediaType = MediaType.fromName(entry.name),
            sizeBytes = entry.size.coerceAtLeast(0L),
        )
    }

    private fun loadImageEntries(): List<TarArchiveEntry> {
        return tarFile.entries
            .filter { !it.isDirectory }
            .filter { PageEntryNames.isPage(it.name) }
            .sortedWith(compareBy(NaturalSort.comparator) { it.name })
    }

    private fun parseComicInfo(): ComicMetadata {
        val entry = tarFile.entries
            .firstOrNull { PageEntryNames.basename(it.name).equals("ComicInfo.xml", ignoreCase = true) }
            ?: return ComicMetadata()
        return runCatching { tarFile.getInputStream(entry).use(ComicInfoParser::parse) }
            .getOrElse { ComicMetadata() }
    }

    private fun ensureOpen() {
        if (closed) throw ArchiveClosedException("Archive '${source.file.name}' is closed")
    }
}
