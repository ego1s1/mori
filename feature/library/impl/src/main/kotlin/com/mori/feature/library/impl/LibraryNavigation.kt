package com.mori.feature.library.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.feature.library.api.LibraryRoute

fun NavGraphBuilder.libraryScreen(onComicClick: (String) -> Unit) {
    composable<LibraryRoute> {
        LibraryRoute(onComicClick = onComicClick)
    }
}
