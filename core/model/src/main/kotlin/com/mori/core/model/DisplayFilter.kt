package com.mori.core.model

/**
 * Color blending mode for reader display filters, mirroring Mihon's filter modes.
 */
enum class FilterBlendMode {
    DEFAULT,   // Standard overlay (SrcOver)
    MULTIPLY,  // Darkens with tint, keeps line art deep black (favorite for manga)
    SCREEN,    // Lightens with tint
    OVERLAY,   // Combines multiply and screen, preserving highlights & shadows
    LIGHTEN,   // Takes lighter pixels
    DARKEN,    // Takes darker pixels
}

/**
 * Curated tone variations for reader display filters.
 */
enum class FilterColorTone(val argb: Long) {
    WARM_AMBER(0xFFFFB74DL),          // Warm candlelight / blue-light reduction
    SEPIA_PAPER(0xFFD7C4A5L),         // Novel / vintage manga paperback parchment
    COOL_SLATE(0xFF90A4AEL),          // Calm daylight / slate
    FOREST_MINT(0xFF81C784L),         // Green tint / eye strain relief
    E_INK_HIGH_CONTRAST(0xFFFFFFFFL), // E-ink style monochrome paper
}

/**
 * Display filters applied to page art at render time (never mutating decodes).
 *
 * - [enabled]: Master toggle. When false, the entire filter pipeline is bypassed.
 * - [brightness] in -1..1: negative dims with black, positive lifts with white. 0 is neutral.
 * - [contrast] in -1..1: color matrix contrast scaling. 0 is neutral.
 * - [grayscale]: full desaturation.
 * - [invert]: color inversion (applied after grayscale and contrast).
 * - [nightTint] in 0..1: warm/color overlay strength for eye comfort.
 * - [colorTone]: preset color tone applied with [nightTint].
 * - [blendMode]: Mihon-style PorterDuff/Compose blend mode for tone overlay.
 *
 * A global default lives in [ReaderPreferences]; per-comic overrides replace
 * it wholesale (never merged channel-by-channel).
 */
data class DisplayFilter(
    val enabled: Boolean = true,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val grayscale: Boolean = false,
    val invert: Boolean = false,
    val nightTint: Float = 0f,
    val colorTone: FilterColorTone = FilterColorTone.WARM_AMBER,
    val blendMode: FilterBlendMode = FilterBlendMode.DEFAULT,
) {
    /** True when every channel is neutral: rendering skips all layers. */
    val isNeutral: Boolean
        get() = brightness == 0f &&
            contrast == 0f &&
            !grayscale &&
            !invert &&
            nightTint == 0f

    fun coerce(): DisplayFilter = copy(
        brightness = brightness.coerceIn(-1f, 1f),
        contrast = contrast.coerceIn(-1f, 1f),
        nightTint = nightTint.coerceIn(0f, 1f),
    )

    companion object {
        val Neutral = DisplayFilter()
    }
}
