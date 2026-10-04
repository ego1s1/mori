package com.mori.core.designsystem

import android.graphics.Matrix
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Animated Material 3 Expressive morphing geometry.
 * Morphs between an organic flower/star polygon and a soft rounded polygon,
 * with rotating gradient colors.
 */
@Composable
fun MoriMorphingShape(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    color: Color = Color.Unspecified,
    numVerticesPerRadius: Int = 8,
    active: Boolean = true,
) {
    val expressiveMotion = LocalExpressiveMotionEnabled.current && active
    val infiniteTransition = rememberInfiniteTransition(label = "MoriMorphingTransition")

    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (expressiveMotion) 1f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "MorphProgress",
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (expressiveMotion) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "MorphRotation",
    )

    val morph = remember(numVerticesPerRadius) {
        val shapeA = RoundedPolygon.star(
            numVerticesPerRadius = numVerticesPerRadius,
            radius = 1f,
            innerRadius = 0.62f,
            rounding = CornerRounding(0.35f),
            innerRounding = CornerRounding(0.25f),
        )
        val shapeB = RoundedPolygon.star(
            numVerticesPerRadius = numVerticesPerRadius + 4,
            radius = 1f,
            innerRadius = 0.85f,
            rounding = CornerRounding(0.5f),
            innerRounding = CornerRounding(0.4f),
        )
        Morph(shapeA, shapeB)
    }

    val defaultBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primary,
        ),
    )
    val effectiveBrush = brush ?: if (color != Color.Unspecified) SolidColor(color) else defaultBrush
    val matrix = remember { Matrix() }

    Canvas(modifier = modifier) {
        val halfMin = size.minDimension / 2f
        if (halfMin <= 0f) return@Canvas

        matrix.reset()
        matrix.postScale(halfMin, halfMin)
        matrix.postRotate(rotationAngle, 0f, 0f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)

        val androidPath = morph.toPath(morphProgress)
        androidPath.transform(matrix)
        drawPath(androidPath.asComposePath(), brush = effectiveBrush)
    }
}
