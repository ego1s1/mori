package com.mori.comic.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrEnginesConcurrencyTest {

    @Test
    fun concurrentRegisterAndLookupIsConsistent() = runTest {
        OcrEngines.clear()
        val jobs = (1..32).map { i ->
            async(Dispatchers.Default) {
                OcrEngines.register(FakeOcrEngine(id = "engine-$i"))
                OcrEngines.byId("engine-$i")
            }
        }
        val results = jobs.awaitAll()
        results.forEachIndexed { index, engine ->
            assertNotNull("engine-${index + 1} missing after concurrent register", engine)
        }
        assertEquals(32, OcrEngines.available().size)
        OcrEngines.clear()
        assertTrue(OcrEngines.available().isEmpty())
    }

    @Test
    fun defaultIsStableUnderConcurrentReads() = runTest {
        OcrEngines.clear()
        OcrEngines.register(FakeOcrEngine(id = "first"))
        val defaults = (1..32).map {
            async(Dispatchers.Default) { OcrEngines.default()?.id }
        }.awaitAll()
        assertTrue(defaults.all { it == "first" })
        OcrEngines.clear()
    }
}
