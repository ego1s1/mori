package com.mori.core.model

/**
 * Display filters applied to page art at render time (never mutating decodes).
 *
 * - [brightness] in -1..1: negative dims with black, positive lifts with
 *   white. 0 is neutral.
 * - [grayscale]: full desaturation.
 * - [invert]: color inversion (applied after grayscale when both are on).
 * - [nightTint] in 0..1: warm overlay strength for night reading.
 *
 * A global default lives in [ReaderPreferences]; per-comic overrides replace
 * it wholesale (never merged channel-by-channel).
 */
data class DisplayFilter(
    val brightness: Float = 0f,
    val grayscale: Boolean = false,
    val invert: Boolean = false,
    val nightTint: Float = 0f,
) {
    /** True when every channel is neutral: rendering can skip all layers. */
    val isNeutral: Boolean
        get() = brightness == 0f && !grayscale && !invert && nightTint == 0f

    fun coerce(): DisplayFilter = copy(
        brightness = brightness.coerceIn(-1f, 1f),
        nightTint = nightTint.coerceIn(0f, 1f),
    )

    companion object {
        val Neutral = DisplayFilter()
    }
}
