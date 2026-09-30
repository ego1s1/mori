package com.mori.core.designsystem

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.PI
import kotlin.math.sin

/**
 * App loading indicator: the M3E morphing indicator with a curly ring
 * instead of a plain circle, so decoding states feel native to the
 * expressive system rather than bolted on.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoriLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val firstWave = remember { wavyRing(lobes = 5) }
    val secondWave = remember { wavyRing(lobes = 8) }
    LoadingIndicator(
        modifier = modifier,
        color = color,
        polygons = listOf(firstWave, secondWave),
    )
}

/**
 * Five-lobe curly ring in unit space: a flower-like wave that reads as the
 * reference wavy spinner once the indicator morphs and rotates it.
 */
private fun wavyRing(lobes: Int = 5, points: Int = 60): RoundedPolygon {
    val vertices = FloatArray(points * 2)
    for (i in 0 until points) {
        val theta = 2f * PI.toFloat() * i / points
        val radius = 0.42f + 0.07f * sin(lobes * theta)
        vertices[i * 2] = 0.5f + radius * kotlin.math.cos(theta)
        vertices[i * 2 + 1] = 0.5f + radius * kotlin.math.sin(theta)
    }
    return RoundedPolygon(
        vertices = vertices,
        rounding = CornerRounding(0.5f),
        perVertexRounding = null,
        centerX = 0.5f,
        centerY = 0.5f,
    )
}
