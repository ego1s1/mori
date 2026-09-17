package com.mori.feature.detail.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DetailShareTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

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
    fun documentUriPassesThrough() {
        val uri = shareableUri(context, "content://authority/book.cbz")

        assertEquals(Uri.parse("content://authority/book.cbz"), uri)
    }

    @Test
    fun folderGrantsAndMissingFilesAreUnshareable() {
        assertNull(
            shareableUri(
                context,
                "content://com.android.externalstorage.documents/tree/primary%3ABooks",
            ),
        )
        assertNull(shareableUri(context, "/no/such/file.cbz"))
    }

    @Test
    fun shareIntentCarriesStreamGrantClipDataAndChooser() {
        val intent = shareComicIntent(
            context = context,
            uri = Uri.parse("content://authority/book.cbz"),
            displayName = "book.cbz",
            mimeType = "application/zip",
            chooserTitle = "Share comic",
        )

        assertEquals(Intent.ACTION_CHOOSER, intent.action)
        assertTrue((intent.flags) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val inner = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals(Intent.ACTION_SEND, inner?.action)
        assertEquals("application/zip", inner?.type)
        assertEquals(
            Uri.parse("content://authority/book.cbz"),
            inner?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java),
        )
        assertEquals(
            Uri.parse("content://authority/book.cbz"),
            inner?.clipData?.getItemAt(0)?.uri,
        )
        assertTrue((inner?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun launchShareReturnsFalseWhenUnhandled() {
        // Folder grant: unresolvable to a shareable URI, no crash.
        val refused = launchShare(
            context,
            DetailMessage.ShareFile(
                uri = "content://com.android.externalstorage.documents/tree/primary%3ABooks",
                displayName = "Books",
                mimeType = "*/*",
            ),
            "Share comic",
        )
        assertFalse(refused)

        // No handler installed under Robolectric: ActivityNotFound maps to false.
        val unhandled = launchShare(
            context,
            DetailMessage.ShareFile(
                uri = "content://authority/book.cbz",
                displayName = "book.cbz",
                mimeType = "application/zip",
            ),
            "Share comic",
        )
        assertFalse(unhandled)
    }
}
