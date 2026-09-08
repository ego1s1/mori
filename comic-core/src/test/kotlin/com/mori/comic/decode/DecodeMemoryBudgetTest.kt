package com.mori.comic.decode

import com.mori.comic.model.DecodeOptions
import com.mori.comic.model.MediaType
import com.mori.comic.testutil.Fixtures
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DecodeMemoryBudgetTest {

    private val decoder = PageDecoder()

    private fun budget(maxDimension: Int): Long = maxDimension.toLong() * maxDimension * BYTES_PER_ARGB_PIXEL

    @Test
    fun boundedDecodeStaysWithinPixelBudget() {
        val max = 256
        val fixtures = listOf(
            Fixtures.Images.landscapeJpg() to MediaType.JPEG,
            Fixtures.Images.portraitJpg() to MediaType.JPEG,
            Fixtures.Images.progressiveJpg() to MediaType.JPEG,
        )
        fixtures.forEach { (bytes, type) ->
            val page = decoder.decode(bytes, type, DecodeOptions(maxDimension = max))
            assertTrue(
                "decoded ${page.bitmap.width}x${page.bitmap.height} exceeds budget for max=$max",
                page.bitmap.allocationByteCount.toLong() <= budget(max),
            )
            page.bitmap.recycle()
        }
    }

    @Test
    fun unboundedDecodeKnowsItsFullCost() {
        val page = decoder.decode(Fixtures.Images.landscapeJpg(), MediaType.JPEG, DecodeOptions())
        // 1200x923 ARGB_8888 = 4,430,400 bytes; assert it matches expectations exactly so any
        // silent config change (e.g. RGB_565 default) is caught.
        assertTrue(page.bitmap.allocationByteCount == 1200 * 923 * BYTES_PER_ARGB_PIXEL)
        page.bitmap.recycle()
    }

    @Test
    fun regionDecodeCostsAtMostRegionArea() {
        val page = decoder.decodeRegion(
            Fixtures.Images.landscapeJpg(),
            MediaType.JPEG,
            android.graphics.Rect(0, 0, 200, 200),
            DecodeOptions(),
        )
        assertTrue(page.bitmap.allocationByteCount.toLong() <= 200L * 200 * BYTES_PER_ARGB_PIXEL)
        page.bitmap.recycle()
    }

    private companion object {
        const val BYTES_PER_ARGB_PIXEL = 4
    }
}
