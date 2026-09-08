package com.mori.comic.decode

/**
 * Pure (JVM-testable) logic for picking a power-of-two [inSampleSize][_] that bounds decoded
 * dimensions. Kept free of `android.graphics` types so it can be exhaustively unit tested.
 */
object SampleSizeCalculator {

    /** The valid power-of-two inSampleSize values: 1, 2, 4, 8, 16, ... */
    private const val MAX_SAMPLE_SIZE = 1 shl 15

    /**
     * Computes the inSampleSize that keeps [width] x [height] within [maxDimension] on the
     * longest side, as a power of two.
     *
     * - Returns `1` when [maxDimension] is `<= 0` (meaning "no limit").
     * - Returns `1` when a side already fits within [maxDimension].
     * - Otherwise doubles from `1` until `longestSide / sample <= maxDimension`, then backs
     *   off one step so the result is the largest power of two that *still fits*.
     *
     * This mirrors `BitmapFactory`'s documented inSampleSize semantics but guarantees a
     * power-of-two result (BitmapFactory rounds inSampleSize down to a power of two anyway).
     */
    fun forDimension(width: Int, height: Int, maxDimension: Int): Int {
        val longest = maxOf(width, height)
        return if (maxDimension <= 0 || longest <= maxDimension) {
            1
        } else {
            var sample = 1
            while (longest / sample > maxDimension && sample < MAX_SAMPLE_SIZE) {
                sample = sample shl 1
            }
            // `sample` is the first power of two whose scaled longest side is <= maxDimension.
            sample
        }
    }
    /**
     * Coerces an arbitrary positive [requested] value to a valid power-of-two inSampleSize in
     * `1, 2, 4, 8, ...`, clamped to [MAX_SAMPLE_SIZE].
     */
    fun roundToPowerOfTwo(requested: Int): Int {
        if (requested <= 1) return 1
        var value = requested
        var result = 1
        while (value > 1 && result < MAX_SAMPLE_SIZE) {
            value = value shr 1
            result = result shl 1
        }
        return if (value > 1 && result >= MAX_SAMPLE_SIZE) MAX_SAMPLE_SIZE else result
    }
}
