package com.mori.feature.detail.impl

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DetailShareTest {

    @Test
    fun mimeTypesMapByExtension() {
        assertEquals("application/zip", shareMimeType("book.cbz"))
        assertEquals("application/zip", shareMimeType("book.ZIP"))
        assertEquals("application/vnd.rar", shareMimeType("book.cbr"))
        assertEquals("application/vnd.rar", shareMimeType("book.rar"))
        assertEquals("*/*", shareMimeType("book.pdf"))
        assertEquals("*/*", shareMimeType("book"))
    }

    @Test
    fun shareIntentCarriesStreamGrantAndChooser() {
        val intent = shareComicIntent(
            uri = "content://tree/book.cbz",
            displayName = "book.cbz",
            mimeType = "application/zip",
            chooserTitle = "Share comic",
        )

        assertEquals(Intent.ACTION_CHOOSER, intent.action)
        val inner = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals(Intent.ACTION_SEND, inner?.action)
        assertEquals("application/zip", inner?.type)
        assertEquals(Uri.parse("content://tree/book.cbz"), inner?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertTrue((inner?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
