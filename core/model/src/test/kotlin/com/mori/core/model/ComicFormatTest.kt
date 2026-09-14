package com.mori.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComicFormatTest {

    @Test
    fun detectsByExtension() {
        assertEquals(ComicFormat.CBZ, ComicFormat.fromFileName("book.cbz"))
        assertEquals(ComicFormat.CBZ, ComicFormat.fromFileName("book.ZIP"))
        assertEquals(ComicFormat.CBR, ComicFormat.fromFileName("book.cbr"))
        assertEquals(ComicFormat.CBR, ComicFormat.fromFileName("book.rar"))
        assertEquals(ComicFormat.CB7, ComicFormat.fromFileName("book.cb7"))
        assertEquals(ComicFormat.CB7, ComicFormat.fromFileName("book.7z"))
        assertEquals(ComicFormat.CBT, ComicFormat.fromFileName("book.cbt"))
        assertEquals(ComicFormat.CBT, ComicFormat.fromFileName("book.tar"))
    }

    @Test
    fun unknownExtensionsYieldNull() {
        assertNull(ComicFormat.fromFileName("book.pdf"))
        assertNull(ComicFormat.fromFileName("book"))
    }

    @Test
    fun mimeTypesCoverShareTargets() {
        assertEquals("application/zip", ComicFormat.CBZ.mimeType())
        assertEquals("application/vnd.rar", ComicFormat.CBR.mimeType())
        assertEquals("application/x-7z-compressed", ComicFormat.CB7.mimeType())
        assertEquals("application/x-tar", ComicFormat.CBT.mimeType())
    }
}
