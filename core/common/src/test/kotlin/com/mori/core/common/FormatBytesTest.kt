package com.mori.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBytesTest {

    @Test
    fun bytesUnderKilo() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("512 B", formatBytes(512))
        assertEquals("1023 B", formatBytes(1023))
    }

    @Test
    fun wholeUnitsDropFraction() {
        assertEquals("2 KB", formatBytes(2048))
        assertEquals("512 B", formatBytes(512))
        assertEquals("3 MB", formatBytes(3L * 1024 * 1024))
    }

    @Test
    fun fractionalUnitsKeepOneDecimal() {
        assertEquals("1.5 KB", formatBytes(1536))
        assertEquals("457.7 MB", formatBytes(480_000_000))
    }

    @Test
    fun climbsToGigabytes() {
        assertEquals("2 GB", formatBytes(2L * 1024 * 1024 * 1024))
    }
}
