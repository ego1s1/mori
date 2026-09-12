package com.mori.feature.reader.impl

import android.view.KeyEvent
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderKeyHandlerTest {

    private fun ready(
        volumeKeys: Boolean = true,
        chromeVisible: Boolean = false,
        settingsOpen: Boolean = false,
    ) = ReaderUiState.Ready(
        comicId = "c",
        title = "Saga",
        subtitle = "",
        bookmarked = false,
        pageIndex = 4,
        pageCount = 10,
        chromeVisible = chromeVisible,
        direction = ReadingDirection.LEFT_TO_RIGHT,
        pageFit = PageFit.WIDTH,
        cropMargins = false,
        settingsOpen = settingsOpen,
        overviewOpen = false,
        volumeKeys = volumeKeys,
        keepScreenOn = true,
        showTapZones = false,
        showPageCounter = true,
        swipeToTurn = true,
    )

    @Test
    fun volumeDownUpNavigatesNext() {
        assertEquals(
            VolumeKeyOutcome.Navigate(ReaderAction.NextPage),
            routeVolumeKey(ready(), KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP),
        )
    }

    @Test
    fun volumeUpUpNavigatesPrev() {
        assertEquals(
            VolumeKeyOutcome.Navigate(ReaderAction.PrevPage),
            routeVolumeKey(ready(), KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_UP),
        )
    }

    @Test
    fun keyDownIsConsumedWithoutAction() {
        assertEquals(
            VolumeKeyOutcome.Consumed,
            routeVolumeKey(ready(), KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN),
        )
    }

    @Test
    fun disabledPrefFallsThrough() {
        assertEquals(
            VolumeKeyOutcome.Ignored,
            routeVolumeKey(ready(volumeKeys = false), KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP),
        )
        assertEquals(
            VolumeKeyOutcome.Ignored,
            routeVolumeKey(ready(volumeKeys = false), KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN),
        )
    }

    @Test
    fun visibleChromeFallsThrough() {
        assertEquals(
            VolumeKeyOutcome.Ignored,
            routeVolumeKey(ready(chromeVisible = true), KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP),
        )
    }

    @Test
    fun openSettingsFallsThrough() {
        assertEquals(
            VolumeKeyOutcome.Ignored,
            routeVolumeKey(ready(settingsOpen = true), KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP),
        )
    }

    @Test
    fun unrelatedKeysFallThrough() {
        assertEquals(
            VolumeKeyOutcome.Ignored,
            routeVolumeKey(ready(), KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.ACTION_UP),
        )
    }

    @Test
    fun nonReadyStatesFallThrough() {
        assertEquals(
            VolumeKeyOutcome.Ignored,
            routeVolumeKey(ReaderUiState.Loading, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP),
        )
    }
}
