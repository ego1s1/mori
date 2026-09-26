package com.mori.feature.reader.impl

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId

/**
 * Double-tap-hold-drag (quick-scale) state shared by [zoneTaps] and [zoomPan].
 *
 * The second tap down arms instead of zooming immediately. A vertical
 * hold-drag then zooms continuously; a clean lift falls through to the
 * stepped double-tap zoom. A second finger disarms (pinch takes over).
 */
internal class QuickScaleState {
    var armed: Boolean = false
        private set

    /** Second tap moved enough to zoom continuously (not a stepped zoom). */
    var moved: Boolean = false
        private set

    /** Finger that armed the pair; other fingers disarm. */
    var pointerId: PointerId? = null
        private set

    var startY: Float = 0f
        private set

    /** Arm point in page coordinates (pan anchor while scaling). */
    var anchor: Offset = Offset.Zero
        private set

    var lastDistance: Float = -1f

    /** Arms a hold-drag pair; resets any stale drag progress. */
    fun arm(id: PointerId, y: Float, anchor: Offset) {
        pointerId = id
        startY = y
        this.anchor = anchor
        armed = true
        moved = false
        lastDistance = -1f
    }

    /** Marks the drag as a continuous zoom (not a stepped one). */
    fun markMoved() {
        moved = true
    }

    /** Records the latest span sample for the next delta. */
    fun sampleSpan(distance: Float) {
        lastDistance = distance
    }

    /** Disarms and clears all drag progress. */
    fun disarm() {
        armed = false
        pointerId = null
        moved = false
        lastDistance = -1f
    }
}

/** Double-tap-hold drag threshold in dp. */
internal const val QUICK_SCALE_THRESHOLD_DP = 20f

/**
 * Span change that starts continuous zoom: relative span deltas at or below
 * this only track, never scale.
 */
internal const val QUICK_SCALE_SPAN = 0.03f

/**
 * Continuous-zoom multiplier for one quick-scale move: 1f until the span
 * change passes the engage threshold, then 1±span (down-drag zooms in,
 * up-drag zooms out). Pure for testability.
 */
internal fun quickScaleMultiplier(distPx: Float, lastDistPx: Float, downwards: Boolean): Float {
    if (lastDistPx <= 0f) return 1f
    val spanDiff = kotlin.math.abs(1f - distPx / lastDistPx) * 0.5f
    if (spanDiff <= QUICK_SCALE_SPAN) return 1f
    return if (downwards) 1f + spanDiff else 1f - spanDiff
}
