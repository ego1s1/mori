package com.mori.comic.archive

import com.github.junrar.Archive
import com.github.junrar.exception.RarException
import com.github.junrar.exception.UnsupportedRarEncryptedException
import com.github.junrar.rarfile.FileHeader
import com.mori.comic.ArchiveClosedException
import com.mori.comic.ComicArchive
import com.mori.comic.ComicException
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
import java.io.File
import java.io.IOException

/**
 * A [ComicArchive] backed by a RAR container (`.cbr` or `.rar`).
 *
 * Backed by [junrar](https://github.com/junrar/junrar). Header enumeration is random access;
 * page reads stream from the archive. Solid archives may require junrar to re-decompress from
 * the start of the solid block for an out-of-order page, which is handled by the library and
 * simply costs extra time.
 */
internal class CbrArchive(
    file: File,
) : ComicArchive {

    override val source: ComicSource.File = ComicSource.File(file)

    private val archive: Archive = openArchive(file)
    private val imageHeaders: List<FileHeader> = loadImageHeaders()
    private val lock = Any()
    private var closed = false

    override val metadata: ComicMetadata = parseComicInfo()
    override val pages: List<ComicPage> = buildPages()

    init {
        if (imageHeaders.isEmpty()) {
            close()
            throw EmptyArchiveException("No image entries found in '${file.name}'")
        }
    }

    override suspend fun readPage(page: ComicPage): ByteArray = withContext(Dispatchers.IO) {
        ensureOpen()
        val header = imageHeaders.getOrNull(page.index)
            ?.takeIf { it.fileName == page.name }
            ?: throw PageNotFoundException("Page '${page.name}' not found in '${source.file.name}'")
        synchronized(lock) {
            try {
                archive.getInputStream(header).use { it.readBytes() }
            } catch (e: IOException) {
                throw CorruptArchiveException("Failed to read page '${page.name}' from '${source.file.name}'", e)
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            archive.close()
        } catch (_: IOException) {
            // Ignored: the archive is being disposed regardless.
        }
    }

    private fun openArchive(file: File): Archive {
        val archive = createArchive(file)
        if (archive.isPasswordProtected() || archive.isEncrypted()) {
            archive.close()
            throw PasswordRequiredException("Archive '${file.name}' is encrypted and requires a password")
        }
        return archive
    }

    private fun createArchive(file: File): Archive {
        return try {
            Archive(file)
        } catch (e: RarException) {
            throw mapRarException(e, file.name)
        } catch (e: IOException) {
            throw CorruptArchiveException("Failed to open '${file.name}'", e)
        }
    }
    private fun buildPages(): List<ComicPage> = imageHeaders.mapIndexed { index, header ->
        ComicPage(
            index = index,
            name = header.fileName,
            mediaType = MediaType.fromName(header.fileName),
            sizeBytes = header.fullUnpackSize,
        )
    }

    private fun loadImageHeaders(): List<FileHeader> {
        val headers = archive.fileHeaders
            .filterNot { it.isDirectory }
            .filter { PageEntryNames.isPage(it.fileName) }
        return headers.sortedWith(compareBy(NaturalSort.comparator) { it.fileName })
    }

    private fun parseComicInfo(): ComicMetadata {
        val header = archive.fileHeaders
            .firstOrNull { PageEntryNames.basename(it.fileName).equals("ComicInfo.xml", ignoreCase = true) }
            ?: return ComicMetadata()
        return synchronized(lock) {
            runCatching { archive.getInputStream(header).use(ComicInfoParser::parse) }
                .getOrElse { ComicMetadata() }
        }
    }

    private fun ensureOpen() {
        if (closed) throw ArchiveClosedException("Archive '${source.file.name}' is closed")
    }

    private fun mapRarException(e: RarException, name: String): ComicException {
        return when (e) {
            is UnsupportedRarEncryptedException ->
                PasswordRequiredException("Archive '$name' is encrypted and requires a password", e)
            else -> CorruptArchiveException("Failed to read RAR archive '$name'", e)
        }
    }
}
