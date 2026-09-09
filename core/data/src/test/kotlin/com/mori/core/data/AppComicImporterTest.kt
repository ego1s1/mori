package com.mori.core.data

import com.mori.core.model.ImportReport
import com.mori.core.model.ImportStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppComicImporterTest {

    private fun importer(dir: File) = AppComicImporter(
        ApplicationProvider.getApplicationContext(),
        dir,
    )

    private fun tempDir(): File =
        Files.createTempDirectory("mori-import-test").toFile().also { it.deleteOnExit() }

    private fun candidate(name: String, bytes: ByteArray) = ImportCandidate(name, bytes.size.toLong()) {
        ByteArrayInputStream(bytes)
    }

    @Test
    fun copiesBytesVerbatim() = runTest {
        val dir = tempDir()
        val content = "comic-bytes".toByteArray()
        val report = importer(dir).importCandidates(listOf(candidate("a.cbz", content)))

        assertEquals(1, report.total)
        assertEquals(1, report.succeeded)
        assertEquals(0, report.failed)
        assertEquals(content.toList(), File(dir, "a.cbz").readBytes().toList())
    }

    @Test
    fun identicalReimportIsIdempotent() = runTest {
        val dir = tempDir()
        val content = "same".toByteArray()
        val engine = importer(dir)
        engine.importCandidates(listOf(candidate("a.cbz", content)))
        val second = engine.importCandidates(listOf(candidate("a.cbz", content)))

        assertEquals(1, second.succeeded)
        assertEquals(listOf("a.cbz"), dir.listFiles()!!.map { it.name })
    }

    @Test
    fun nameCollisionWithDifferentContentRenames() = runTest {
        val dir = tempDir()
        val engine = importer(dir)
        engine.importCandidates(listOf(candidate("a.cbz", "one".toByteArray())))
        engine.importCandidates(listOf(candidate("a.cbz", "two-different".toByteArray())))

        val names = dir.listFiles()!!.map { it.name }.sorted()
        assertEquals(listOf("a (2).cbz", "a.cbz"), names)
        assertEquals("two-different", File(dir, "a (2).cbz").readText())
    }

    @Test
    fun failingEntriesAreReportedNotThrown() = runTest {
        val dir = tempDir()
        val bad = ImportCandidate("bad.cbz", 4) { throw java.io.IOException("nope") }
        val report = importer(dir).importCandidates(
            listOf(candidate("good.cbz", "ok".toByteArray()), bad),
        )

        assertEquals(2, report.total)
        assertEquals(1, report.succeeded)
        assertEquals(1, report.failed)
        val failed = report.items.single { it.displayName == "bad.cbz" }
        assertEquals(ImportStatus.FAILED, failed.status)
        assertTrue(failed.error.orEmpty().contains("nope"))
    }

    @Test
    fun progressCallbackReceivesMonotonicCounts() = runTest {
        val dir = tempDir()
        val seen = mutableListOf<Pair<Int, Int>>()
        importer(dir).importCandidates(
            listOf(
                candidate("a.cbz", "1".toByteArray()),
                candidate("b.cbz", "2".toByteArray()),
                candidate("c.cbz", "3".toByteArray()),
            ),
            onProgress = { done, total -> seen += done to total },
        )

        assertEquals(listOf(1 to 3, 2 to 3, 3 to 3), seen)
    }

    @Test
    fun pathSeparatorsAreSanitized() = runTest {
        val dir = tempDir()
        importer(dir).importCandidates(listOf(candidate("../evil.cbz", "x".toByteArray())))

        val names = dir.listFiles()!!.map { it.name }
        assertEquals(1, names.size)
        assertTrue(!names.single().contains('/'))
    }

    @Test
    fun emptyCandidateListYieldsEmptyReport() = runTest {
        val report: ImportReport = importer(tempDir()).importCandidates(emptyList())
        assertEquals(0, report.total)
        assertTrue(report.isComplete)
    }
}
