package com.mori.comic.testutil

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream

/**
 * Test utilities for building CBZ archives, image files, and folder trees in a temporary
 * directory. These are helpers for tests and are NOT part of the library's public API.
 */
object Archives {
    /** A tiny, valid 1x1 PNG. */
    val PNG_1X1: ByteArray = png1x1()

    fun newTempDir(prefix: String = "mori-test"): File =
        java.nio.file.Files.createTempDirectory(prefix).toFile().also { it.deleteOnExit() }

    /** Writes a zipped [entries] map (entry-name -> content) as a `.cbz` file under [dir]. */
    fun writeCbz(dir: File, name: String, entries: Map<String, ByteArray>): File {
        val file = File(dir, name)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zip ->
            entries.forEach { (entryName, content) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return file
    }

    /** Writes an image file with the given content bytes and [fileName]. */
    fun writeFile(dir: File, fileName: String, content: ByteArray): File {
        val file = File(dir, fileName)
        file.parentFile?.mkdirs()
        file.writeBytes(content)
        return file
    }

    /**
     * Writes [entries] as a `.cb7` file under [dir]. Entries stage through
     * temp files because 7-Zip entry creation is file-backed.
     */
    fun writeCb7(
        dir: File,
        name: String,
        entries: Map<String, ByteArray>,
        password: CharArray? = null,
    ): File {
        val file = File(dir, name)
        val out = if (password != null) SevenZOutputFile(file, password) else SevenZOutputFile(file)
        out.use {
            entries.forEach { (entryName, content) ->
                val staged = writeFile(File(dir, "stage-${entryName.hashCode()}"), "page.bin", content)
                it.putArchiveEntry(it.createArchiveEntry(staged, entryName))
                it.write(content)
                it.closeArchiveEntry()
            }
            it.finish()
        }
        return file
    }

    /** Writes [entries] as a `.cbt` (TAR) file under [dir]. */
    fun writeCbt(dir: File, name: String, entries: Map<String, ByteArray>): File {
        val file = File(dir, name)
        TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(file))).use { tar ->
            entries.forEach { (entryName, content) ->
                val staged = writeFile(File(dir, "stage-${entryName.hashCode()}"), "page.bin", content)
                val entry = TarArchiveEntry(staged, entryName)
                tar.putArchiveEntry(entry)
                tar.write(content)
                tar.closeArchiveEntry()
            }
            tar.finish()
        }
        return file
    }

    /** Creates a directory (and parents) under [dir]. */
    fun writeDir(dir: File, path: String): File {
        val file = File(dir, path)
        file.mkdirs()
        return file
    }

    val sampleComicInfo: ByteArray = """
        <?xml version="1.0"?>
        <ComicInfo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns:xsd="http://www.w3.org/2001/XMLSchema">
          <Title>Test Comic</Title>
          <Series>Sample Series</Series>
          <Number>42</Number>
          <Volume>1999</Volume>
          <Summary>An edge-case comic.</Summary>
          <Publisher>Mori Press</Publisher>
          <PageCount>3</PageCount>
          <Year>2001</Year>
          <Writer>Writer McWrite</Writer>
          <LanguageISO>en</LanguageISO>
          <Manga>No</Manga>
        </ComicInfo>
    """.trimIndent().toByteArray(Charsets.UTF_8)

    private fun png1x1(): ByteArray {
        // 1x1 gray pixel PNG (valid IHDR/IDAT/IEND), verified against zlib.
        return byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00,
            0x90.toByte(), 0x77, 0x53, 0xDE.toByte(),
            0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
            0x78, 0x9C.toByte(), 0x63, 0x60, 0x60, 0x60, 0x00, 0x00,
            0x00, 0x04, 0x00, 0x01,
            0xF6.toByte(), 0x17, 0x38.toByte(), 0x55,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            0xAE.toByte(), 0x42, 0x60, 0x82.toByte(),
        )
    }
}
