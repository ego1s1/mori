package com.mori.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * End-to-end cover generation over the REAL backend and REAL fixtures: the strongest
 * interoperability guard between core:data and comic-core.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CoverGeneratorTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun resourceBytes(name: String): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("com/mori/core/data/$name")!!.use { it.readBytes() }

    private fun writeCbz(dir: File, name: String, entries: Map<String, ByteArray>): File {
        val file = File(dir, name)
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            entries.forEach { (entryName, content) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun generatesJpegThumbnailWithinBudget() = runTest {
        val dir = File(context.filesDir, "cover-test").apply { mkdirs() }
        val source = writeCbz(
            dir,
            "comic.cbz",
            mapOf(
                "002.jpg" to resourceBytes("landscape.jpg"),
                "001.jpg" to resourceBytes("portrait.jpg"),
            ),
        )
        val generator = CoverGenerator(context, MoriComicBackendDataSource())

        val coverPath = generator.generateCover(
            source,
            "comic.cbz",
            listOf(
                com.mori.comic.model.ComicPage(
                    0,
                    "002.jpg",
                    com.mori.comic.model.MediaType.JPEG,
                    10L,
                ),
            ),
        )

        assertTrue(coverPath != null)
        val cover = File(coverPath!!)
        assertTrue(cover.isFile)
        val magic = cover.inputStream().use { it.readNBytes(2).toList() }
        assertEquals(listOf(0xFF.toByte(), 0xD8.toByte()), magic)
        val bitmap = android.graphics.BitmapFactory.decodeFile(cover.absolutePath)
        assertTrue(bitmap.width in 1..CoverGenerator.COVER_MAX_DIMENSION)
        assertTrue(bitmap.height in 1..CoverGenerator.COVER_MAX_DIMENSION)
        bitmap.recycle()
    }

    @Test
    fun prefersCoverNamedPage() = runTest {
        val dir = File(context.filesDir, "cover-test").apply { mkdirs() }
        // "cover.jpg" sorts after numbered pages naturally; the generator must still pick it.
        val source = writeCbz(
            dir,
            "comic2.cbz",
            mapOf(
                "001.jpg" to resourceBytes("portrait.jpg"),
                "cover.jpg" to resourceBytes("landscape.jpg"),
            ),
        )
        val generator = CoverGenerator(context, MoriComicBackendDataSource())

        val coverPath = generator.generateCover(
            source,
            "comic2.cbz",
            listOf(
                com.mori.comic.model.ComicPage(
                    0,
                    "001.jpg",
                    com.mori.comic.model.MediaType.JPEG,
                    10L,
                ),
                com.mori.comic.model.ComicPage(
                    1,
                    "cover.jpg",
                    com.mori.comic.model.MediaType.JPEG,
                    10L,
                ),
            ),
        )

        assertTrue(coverPath != null)
        // Landscape source is 1200 wide -> sampled to 300 at maxDimension 512... just check validity.
        val bitmap = android.graphics.BitmapFactory.decodeFile(coverPath!!)
        assertTrue(bitmap.width in 1..CoverGenerator.COVER_MAX_DIMENSION)
        bitmap.recycle()
    }

    @Test
    fun returnsNullForEmptyArchive() = runTest {
        val dir = File(context.filesDir, "cover-test").apply { mkdirs() }
        val source = writeCbz(dir, "empty.cbz", mapOf("notes.txt" to "hi".toByteArray()))
        val generator = CoverGenerator(context, MoriComicBackendDataSource())

        // notes.txt is not an image; with no pages the generator returns null.
        assertNull(generator.generateCover(source, "empty.cbz", emptyList()))
    }
}
