package com.mori.feature.settings.impl

/** Test tags for the settings screen. */
object SettingsTestTags {
    const val Content = "settingsContent"
    const val Snackbar = "settingsSnackbar"

    fun segmentFor(label: String) = "settingsSegment_$label"
}
