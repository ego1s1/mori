package com.mori.feature.reader.impl

import android.view.KeyEvent

/** Outcome of routing one hardware key event. */
internal sealed interface VolumeKeyOutcome {
    /** Not a reader volume press: falls through to the system. */
    data object Ignored : VolumeKeyOutcome

    /** Swallowed with no action (key-down and repeats). */
    data object Consumed : VolumeKeyOutcome

    /** Navigate on key-up. */
    data class Navigate(val action: ReaderAction) : VolumeKeyOutcome
}

/**
 * Routes hardware volume keys, mirroring Mihon's pager viewer: the press is
 * consumed (so system volume never moves) and navigation fires on key-up
 * only — one turn per press, no repeat fire while held. Stands down unless
 * the pref is on with chrome hidden and settings closed, exactly like
 * Mihon's menu-visible guard. The inverted pref swaps down/up (Mihon's
 * `readWithVolumeKeysInverted`). Pure logic, fully unit-testable.
 */
internal fun routeVolumeKey(
    state: ReaderUiState,
    keyCode: Int,
    eventAction: Int,
): VolumeKeyOutcome {
    if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP) {
        return VolumeKeyOutcome.Ignored
    }
    val ready = state as? ReaderUiState.Ready ?: return VolumeKeyOutcome.Ignored
    if (!ready.volumeKeys || ready.chromeVisible || ready.settingsOpen) {
        return VolumeKeyOutcome.Ignored
    }
    if (eventAction != KeyEvent.ACTION_UP) {
        return VolumeKeyOutcome.Consumed
    }
    val down = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
    val forward = if (ready.volumeKeysInverted) !down else down
    val action = if (forward) {
        ReaderAction.NextPage
    } else {
        ReaderAction.PrevPage
    }
    return VolumeKeyOutcome.Navigate(action)
}
