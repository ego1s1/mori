package com.mori.feature.library.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

fun NavController.navigateToLibrary() {
    navigate(LibraryRoute) {
        popUpTo(0) { inclusive = false }
        launchSingleTop = true
    }
}
