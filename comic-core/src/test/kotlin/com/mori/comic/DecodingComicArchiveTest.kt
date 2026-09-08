package com.mori.comic

import android.graphics.Rect
import com.mori.comic.model.DecodeOptions
import com.mori.comic.model.MediaType
import com.mori.comic.testutil.Archives
import com.mori.comic.testutil.Fixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DecodingComicArchiveTest {

    private fun newArchive(): DecodingComicArchive {
        val dir = Archives.newTempDir()
        val file = Archives.writeCbz(
            dir,
            "comic.cbz",
            mapOf(
                "001.jpg" to Fixtures.Images.portraitJpg(),
                "002.jpg" to Fixtures.Images.landscapeJpg(),
                "003.jpg" to Fixtures.Images.progressiveJpg(),
            ),
        )
        return ComicFactory.open(ComicSource.File(file)).withDecode()
    }

    @Test
    fun decodesPageFromArchive() = runTest {
        newArchive().use { archive ->
            assertEquals(3, archive.pageCount)
            val page = archive.decode(archive.pages[0], DecodeOptions(maxDimension = 256))
            assertEquals(MediaType.JPEG, page.mediaType)
            assertTrue(page.bitmap.width in 1..256)
            assertEquals(447, page.sourceWidth)
            page.bitmap.recycle()
        }
    }

    @Test
    fun readsPageDimensionsWithoutDecoding() = runTest {
        newArchive().use { archive ->
            val dims = archive.pageDimensions(archive.pages[1])
            assertEquals(1200, dims.width)
            assertEquals(923, dims.height)
        }
    }

    @Test
    fun decodesRegionFromArchive() = runTest {
        newArchive().use { archive ->
            val page = archive.decodeRegion(archive.pages[1], Rect(0, 0, 200, 200), DecodeOptions())
            assertEquals(200, page.bitmap.width)
            assertEquals(200, page.bitmap.height)
            page.bitmap.recycle()
        }
    }

    @Test
    fun progressiveJpegDecodesThroughArchive() = runTest {
        newArchive().use { archive ->
            val page = archive.decode(archive.pages[2], DecodeOptions(maxDimension = 300))
            assertTrue(page.bitmap.width in 1..300)
            page.bitmap.recycle()
        }
    }
}
