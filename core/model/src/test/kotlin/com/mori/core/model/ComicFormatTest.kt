package com.mori.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ComicFormatTest {

    @Test
    fun mimeTypesCoverShareTargets() {
        assertEquals("application/zip", ComicFormat.CBZ.mimeType())
        assertEquals("application/vnd.rar", ComicFormat.CBR.mimeType())
        assertEquals("application/x-7z-compressed", ComicFormat.CB7.mimeType())
        assertEquals("application/x-tar", ComicFormat.CBT.mimeType())
    }
}
