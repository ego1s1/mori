package com.mori.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Scale
import java.io.File

/**
 * Cover art with an instant placeholder until the bitmap lands. Shared by the
 * library grid and the detail hero so covers load — and fail — identically everywhere.
 *
 * Performance-optimized:
 * - Uses Coil's [AsyncImage] with a custom [CenteredVectorPainter] to draw the placeholder
 *   directly in the Canvas draw pass without subcomposition or StateFlow lifecycle observations.
 * - Memory caching is strongly enabled; redundant disk-cache writes are avoided since cover
 *   files already reside on internal storage as local files.
 * - Zero recompositions occur upon image load completion.
 */
@Composable
fun MoriCoverArt(
    coverPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val vectorPainter = rememberVectorPainter(image = MoriIcons.MenuBook)
    val placeholderTint = MaterialTheme.colorScheme.onSurfaceVariant
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val density = LocalDensity.current
    val iconSizePx = remember(density) { with(density) { 40.dp.toPx() } }
    val placeholder = remember(vectorPainter, placeholderTint, iconSizePx) {
        CenteredVectorPainter(
            painter = vectorPainter,
            iconSizePx = iconSizePx,
            tint = placeholderTint,
        )
    }

    if (coverPath != null) {
        val appContext = LocalContext.current.applicationContext
        val request = remember(coverPath) {
            ImageRequest.Builder(appContext)
                .data(File(coverPath))
                .memoryCacheKey(coverPath)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(false)
                .precision(Precision.INEXACT)
                .scale(Scale.FILL)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            placeholder = placeholder,
            error = placeholder,
            fallback = placeholder,
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
        )
    } else {
        Image(
            painter = placeholder,
            contentDescription = contentDescription,
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
        )
    }
}

private class CenteredVectorPainter(
    private val painter: Painter,
    private val iconSizePx: Float,
    private val tint: Color,
) : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val left = (size.width - iconSizePx) / 2f
        val top = (size.height - iconSizePx) / 2f
        if (left >= 0 && top >= 0) {
            translate(left = left, top = top) {
                with(painter) {
                    draw(
                        size = Size(iconSizePx, iconSizePx),
                        colorFilter = ColorFilter.tint(tint),
                    )
                }
            }
        }
    }
}
