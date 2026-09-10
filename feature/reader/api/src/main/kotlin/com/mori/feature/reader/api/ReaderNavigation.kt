package com.mori.feature.reader.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class ReaderRoute(val comicId: String, val pageIndex: Int = 0)

fun NavController.navigateToReader(comicId: String, pageIndex: Int = 0) {
    navigate(ReaderRoute(comicId, pageIndex)) {
        launchSingleTop = true
    }
}
