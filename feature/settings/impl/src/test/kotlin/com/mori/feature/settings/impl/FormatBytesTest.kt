package com.mori.feature.settings.impl

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBytesTest {

    @Test
    fun formatsBytes() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("512 B", formatBytes(512))
    }

    @Test
    fun formatsKilobytes() {
        assertEquals("2 KB", formatBytes(2048))
        assertEquals("1.5 KB", formatBytes(1536))
    }

    @Test
    fun formatsMegabytesAndGigabytes() {
        assertEquals("6 MB", formatBytes(6L * 1024 * 1024))
        assertEquals("2 GB", formatBytes(2L * 1024 * 1024 * 1024))
    }
}
