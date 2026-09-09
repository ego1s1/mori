package com.mori.feature.settings.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavController.navigateToSettings() {
    navigate(SettingsRoute)
}
