package com.mori.feature.settings.impl

/** Test tags for the settings screen. */
object SettingsTestTags {
    const val Content = "settingsContent"
    const val Snackbar = "settingsSnackbar"
    const val GroupCreateButton = "settingsGroupCreate"
    const val GroupNameField = "settingsGroupNameField"
    const val GroupConfirm = "settingsGroupConfirm"
    const val GroupDeleteConfirm = "settingsGroupDeleteConfirm"
    const val GroupDialog = "settingsGroupDialog"
    const val LicensesRow = "settingsLicenses"

    fun segmentFor(label: String) = "settingsSegment_$label"

    fun groupRow(id: Long): String = "settingsGroup:$id"
}
