package com.mori.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.io.File

/**
 * Cover art with a book placeholder behind it. Shared by the library grid and
 * the detail hero so covers load — and fail — identically everywhere.
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
        Icon(
            imageVector = MoriIcons.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp),
        )
        if (coverPath != null) {
            // No per-load fade: the global loader crossfades, but grid cells
            // recycling through a fling must snap, not alpha-blend per frame.
            // Remembered on path: rebuilding the request every composition
            // restarts in-flight loads, flashing placeholders mid-scroll.
            val context = LocalContext.current
            val request = remember(context, coverPath) {
                ImageRequest.Builder(context)
                    .data(File(coverPath))
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
