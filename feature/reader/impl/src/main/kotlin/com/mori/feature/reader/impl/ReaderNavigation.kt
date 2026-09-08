package com.mori.feature.reader.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.feature.reader.api.ReaderRoute

fun NavGraphBuilder.readerScreen(onBackClick: () -> Unit) {
    composable<ReaderRoute> {
        ReaderRoute(onBackClick = onBackClick)
    }
}
