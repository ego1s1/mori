package com.mori.feature.reader.impl

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import com.mori.core.model.DisplayFilter
import com.mori.core.model.FilterBlendMode
import com.mori.core.model.FilterColorTone

/**
 * Render mapping for [DisplayFilter]: which color matrix the page [Image]
 * takes plus overlay strengths drawn above the art (inside the zoom
 * transform, so filters pan and zoom with the page).
 *
 * Matrix order is grayscale -> contrast -> invert:
 * desaturate first, adjust contrast, then invert luminance matching print-negative intuition.
 */
internal fun colorMatrixFor(filter: DisplayFilter): ColorMatrix? {
    if (!filter.enabled) return null
    val hasContrast = filter.contrast != 0f
    val hasGray = filter.grayscale
    val hasInvert = filter.invert

    if (!hasGray && !hasInvert && !hasContrast) return null

    if (hasGray && !hasInvert && !hasContrast) {
        return ColorMatrix().apply { setToSaturation(0f) }
    }

    var current = IDENTITY_VALUES

    if (hasGray) {
        current = multiply4x5(GRAY_VALUES, current)
    }

    if (hasContrast) {
        val scale = 1f + filter.contrast.coerceIn(-1f, 1f)
        val offset = 128f * (1f - scale)
        val contrastMatrix = floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )
        current = multiply4x5(contrastMatrix, current)
    }

    if (hasInvert) {
        current = multiply4x5(INVERT_VALUES, current)
    }

    return ColorMatrix(current)
}

private val IDENTITY_VALUES = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

private val GRAY_VALUES = floatArrayOf(
    0.2126f, 0.7152f, 0.0722f, 0f, 0f,
    0.2126f, 0.7152f, 0.0722f, 0f, 0f,
    0.2126f, 0.7152f, 0.0722f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

private val INVERT_VALUES = floatArrayOf(
    -1f, 0f, 0f, 0f, 255f,
    0f, -1f, 0f, 0f, 255f,
    0f, 0f, -1f, 0f, 255f,
    0f, 0f, 0f, 1f, 0f,
)

/** Affine 4x5 product `a * b` (b applied first). Pure for testability. */
internal fun multiply4x5(a: FloatArray, b: FloatArray): FloatArray {
    require(a.size == 20 && b.size == 20)
    val out = FloatArray(20)
    for (row in 0..3) {
        for (col in 0..4) {
            var sum = if (col == 4) a[row * 5 + 4] else 0f
            for (k in 0..3) {
                sum += a[row * 5 + k] * b[k * 5 + col]
            }
            out[row * 5 + col] = sum
        }
    }
    return out
}

/** Map [FilterBlendMode] to Compose [BlendMode] mirroring Mihon. */
internal fun composeBlendModeFor(mode: FilterBlendMode): BlendMode = when (mode) {
    FilterBlendMode.DEFAULT -> BlendMode.SrcOver
    FilterBlendMode.MULTIPLY -> BlendMode.Multiply
    FilterBlendMode.SCREEN -> BlendMode.Screen
    FilterBlendMode.OVERLAY -> BlendMode.Overlay
    FilterBlendMode.LIGHTEN -> BlendMode.Lighten
    FilterBlendMode.DARKEN -> BlendMode.Darken
}

/** Color value for [FilterColorTone]. */
internal fun toneColorFor(tone: FilterColorTone): Color = Color(tone.argb)

/** Black dim overlay alpha for negative brightness (0 when lifting). */
internal fun dimAlphaFor(brightness: Float): Float =
    (-brightness.coerceIn(-1f, 1f)).coerceAtLeast(0f) * MAX_DIM_ALPHA

/** White lift overlay alpha for positive brightness (0 when dimming). */
internal fun liftAlphaFor(brightness: Float): Float =
    brightness.coerceIn(-1f, 1f).coerceAtLeast(0f) * MAX_LIFT_ALPHA

/** Warm night overlay alpha for a 0..1 tint strength. */
internal fun nightAlphaFor(nightTint: Float): Float =
    nightTint.coerceIn(0f, 1f) * MAX_NIGHT_ALPHA

internal const val MAX_DIM_ALPHA = 0.85f
internal const val MAX_LIFT_ALPHA = 0.6f
internal const val MAX_NIGHT_ALPHA = 0.35f
