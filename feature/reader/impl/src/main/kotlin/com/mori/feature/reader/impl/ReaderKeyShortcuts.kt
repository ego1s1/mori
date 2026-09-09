package com.mori.feature.reader.impl

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Maps hardware volume keys to reader navigation, mirroring the established
 * volume-down-next / volume-up-previous convention.
 *
 * Returns the action to dispatch, or null when keys are disabled, the event is not a
 * key-up, or the key is unrelated. Pure logic, fully unit-testable without a device.
 */
internal fun volumeKeyAction(key: Key, type: KeyEventType, volumeKeysEnabled: Boolean): ReaderAction? {
    if (!volumeKeysEnabled || type != KeyEventType.KeyUp) return null
    return when (key) {
        Key.VolumeDown -> ReaderAction.NextPage
        Key.VolumeUp -> ReaderAction.PrevPage
        else -> null
    }
}
