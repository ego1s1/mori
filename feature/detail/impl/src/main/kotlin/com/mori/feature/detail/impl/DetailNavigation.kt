package com.mori.feature.detail.impl

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.core.designsystem.LocalNavAnimatedVisibilityScope
import com.mori.feature.detail.api.DetailRoute

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.detailScreen(
    onBackClick: () -> Unit,
    onReadClick: (String, Int) -> Unit,
) {
    composable<DetailRoute> {
        CompositionLocalProvider(
            LocalNavAnimatedVisibilityScope provides this,
        ) {
            DetailRoute(
                onBackClick = onBackClick,
                onReadClick = onReadClick,
            )
        }
    }
}
