package com.mori.feature.detail.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.feature.detail.api.DetailRoute

fun NavGraphBuilder.detailScreen(
    onBackClick: () -> Unit,
    onReadClick: (String, Int) -> Unit,
) {
    composable<DetailRoute> {
        DetailRoute(
            onBackClick = onBackClick,
            onReadClick = onReadClick,
        )
    }
}
