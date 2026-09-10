package com.mori.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Scale
import java.io.File

/**
 * Cover art with a book placeholder until the bitmap lands. Shared by the
 * library grid and the detail hero so covers load — and fail — identically
 * everywhere.
 *
 * The placeholder unmounts on success instead of riding behind every loaded
 * cover as permanent overdraw; the request is remembered per path (rebuilding
 * it each composition restarts in-flight loads mid-scroll) and bounded to an
 * inexact fill so Coil may serve a smaller cached bitmap for grid cells.
 */
@Composable
fun MoriCoverArt(
    coverPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (coverPath != null) {
            // No per-load fade: the global loader crossfades, but grid cells
            // recycling through a fling must snap, not alpha-blend per frame.
            val context = LocalContext.current
            val request = remember(context, coverPath) {
                ImageRequest.Builder(context)
                    .data(File(coverPath))
                    .crossfade(false)
                    .precision(Precision.INEXACT)
                    .scale(Scale.FILL)
                    .build()
            }
            val painter = rememberAsyncImagePainter(
                model = request,
                contentScale = ContentScale.Crop,
            )
            val painterState by painter.state.collectAsState()
            Image(
                painter = painter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (painterState !is AsyncImagePainter.State.Success) {
                Icon(
                    imageVector = MoriIcons.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp),
                )
            }
        } else {
            Icon(
                imageVector = MoriIcons.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
