package com.mori.comic

import com.mori.comic.archive.CbzArchive
import com.mori.comic.archive.CbrArchive
import com.mori.comic.archive.FolderArchive
import java.io.File

/**
 * Entry point for opening a [ComicArchive] from a [ComicSource].
 *
 * The factory performs format detection and returns the matching implementation. Archives
 * must be [closed][ComicArchive.close] by the caller.
 */
object ComicFactory {

    /**
     * Opens [source] as a [ComicArchive].
     *
     * @throws UnsupportedFormatException when the source's format is not recognized.
     * @throws CorruptArchiveException when the source cannot be read.
     * @throws EmptyArchiveException when the source contains no pages.
     * @throws PasswordRequiredException when a CBR is encrypted.
     */
    fun open(source: ComicSource): ComicArchive {
        return when (source) {
            is ComicSource.Directory -> openDirectory(source.directory)
            is ComicSource.File -> openFile(source.file)
        }
    }

    private fun openDirectory(directory: File): ComicArchive {
        if (!directory.exists()) {
            throw CorruptArchiveException("Directory does not exist: '${directory.path}'")
        }
        if (!directory.isDirectory) {
            throw CorruptArchiveException("Not a directory: '${directory.path}'")
        }
        return FolderArchive(directory)
    }

    private fun openFile(file: File): ComicArchive {
        if (!file.exists()) {
            throw CorruptArchiveException("File does not exist: '${file.path}'")
        }
        if (!file.isFile) {
            throw CorruptArchiveException("Not a regular file: '${file.path}'")
        }
        return when (ArchiveFormat.detect(file)) {
            ArchiveFormat.CBZ -> CbzArchive(file)
            ArchiveFormat.CBR -> CbrArchive(file)
        }
    }
}
