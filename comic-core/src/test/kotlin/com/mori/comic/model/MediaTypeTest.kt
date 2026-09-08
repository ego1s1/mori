package com.mori.comic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaTypeTest {

    @Test
    fun resolvesKnownTypes() {
        assertEquals(MediaType.JPEG, MediaType.fromName("page.jpg"))
        assertEquals(MediaType.JPEG, MediaType.fromName("page.jpeg"))
        assertEquals(MediaType.JPEG, MediaType.fromName("page.JPEG"))
        assertEquals(MediaType.PNG, MediaType.fromName("page.png"))
        assertEquals(MediaType.WEBP, MediaType.fromName("page.webp"))
        assertEquals(MediaType.GIF, MediaType.fromName("page.gif"))
        assertEquals(MediaType.BMP, MediaType.fromName("page.bmp"))
    }

    @Test
    fun resolvesTypesIgnoringDirectoryPrefix() {
        assertEquals(MediaType.JPEG, MediaType.fromName("nested/folder/page.jpg"))
    }

    @Test
    fun unknownExtensionsResolveToUnknown() {
        assertEquals(MediaType.UNKNOWN, MediaType.fromName("page.txt"))
        assertEquals(MediaType.UNKNOWN, MediaType.fromName("page"))
        assertEquals(MediaType.UNKNOWN, MediaType.fromName("page.jpg.bak"))
    }

    @Test
    fun isImageNameMatchesKnownOnly() {
        assertTrue(MediaType.isImageName("a.jpg"))
        assertTrue(MediaType.isImageName("a.PNG"))
        assertFalse(MediaType.isImageName("a.txt"))
        assertFalse(MediaType.isImageName("ComicInfo.xml"))
    }
}
