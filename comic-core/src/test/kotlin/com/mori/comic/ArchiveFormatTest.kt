package com.mori.comic

import com.mori.comic.testutil.Archives
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ArchiveFormatTest {

    @Test
    fun detectsCbzByExtension() {
        val file = Archives.writeFile(Archives.newTempDir(), "comic.cbz", byteArrayOf(0x50, 0x4B, 0x03, 0x04))
        assertEquals(ArchiveFormat.CBZ, ArchiveFormat.detect(file))
    }

    @Test
    fun detectsCbrByExtension() {
        // Extension wins even without reading contents.
        val file = Archives.writeFile(Archives.newTempDir(), "comic.cbr", byteArrayOf(0, 1, 2, 3))
        assertEquals(ArchiveFormat.CBR, ArchiveFormat.detect(file))
    }

    @Test
    fun detectsZipByMagicWhenExtensionUnknown() {
        val file = Archives.writeFile(Archives.newTempDir(), "comic.unknown", byteArrayOf(0x50, 0x4B, 0x03, 0x04))
        assertEquals(ArchiveFormat.CBZ, ArchiveFormat.detect(file))
    }

    @Test
    fun detectsRarByMagicWhenExtensionUnknown() {
        val rar = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00.toByte(), 0x00)
        val file = Archives.writeFile(Archives.newTempDir(), "comic.bin", rar)
        assertEquals(ArchiveFormat.CBR, ArchiveFormat.detect(file))
    }

    @Test
    fun detectsEmptyZipByMagic() {
        val file = Archives.writeFile(Archives.newTempDir(), "comic.bin", byteArrayOf(0x50, 0x4B, 0x05, 0x06))
        assertEquals(ArchiveFormat.CBZ, ArchiveFormat.detect(file))
    }

    @Test
    fun unsupportedExtensionWithoutMagicThrows() {
        val file = Archives.writeFile(Archives.newTempDir(), "notacomic.txt", "hello".toByteArray())
        assertThrows(UnsupportedFormatException::class.java) { ArchiveFormat.detect(file) }
    }

    @Test
    fun extensionIsCaseInsensitive() {
        val file = Archives.writeFile(Archives.newTempDir(), "comic.CBZ", byteArrayOf(0x50, 0x4B, 0x03, 0x04))
        assertEquals(ArchiveFormat.CBZ, ArchiveFormat.detect(file))
    }

    @Test
    fun unsupportedExceptionCarriesFileName() {
        val file = Archives.writeFile(Archives.newTempDir(), "weird.dat", byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        val e = assertThrows(UnsupportedFormatException::class.java) { ArchiveFormat.detect(file) }
        assert(e.message.orEmpty().contains("weird.dat"))
    }
}
