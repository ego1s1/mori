package com.mori.comic.util

import com.mori.comic.CorruptArchiveException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ArchiveBudgetsTest {

    @Test
    fun readsWithinBudgetNormally() {
        val bytes = ByteArrayInputStream(ByteArray(1024) { it.toByte() })
            .readCapped(capBytes = 2048, what = "Page 'p'")

        assertArrayEquals(ByteArray(1024) { it.toByte() }, bytes)
    }

    @Test
    fun overrunThrowsNamedCorruptError() {
        var failed = false
        try {
            ByteArrayInputStream(ByteArray(1024)).readCapped(capBytes = 64, what = "Page 'p'")
        } catch (e: CorruptArchiveException) {
            failed = true
            assertTrue(e.message!!.contains("Page 'p'"))
        }
        assertTrue(failed)
    }
}
