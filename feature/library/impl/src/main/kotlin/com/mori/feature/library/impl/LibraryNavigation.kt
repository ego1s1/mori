package com.mori.feature.library.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.feature.library.api.LibraryRoute

fun NavGraphBuilder.libraryScreen(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
) {
    composable<LibraryRoute> {
        LibraryRoute(
            onReadClick = onReadClick,
            onComicLongClick = onComicLongClick,
        )
    }
}
