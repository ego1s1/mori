package com.mori.feature.detail.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class DetailRoute(val comicId: String)

fun NavController.navigateToDetail(comicId: String) {
    navigate(DetailRoute(comicId))
}
