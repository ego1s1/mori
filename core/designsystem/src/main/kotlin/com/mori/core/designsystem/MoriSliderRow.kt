package com.mori.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Labeled slider row for settings sheets: label with a Flex value readout
 * above, squiggly animated track below. Used for continuous preferences like
 * display-filter channels.
 *
 * Implements Material 3 Expressive squiggly wave dynamics with physics-based
 * spring expansion on scrub and continuous organic phase animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoriSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    onValueChangeFinished: () -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
) {
    val haptics = rememberMoriHaptics()
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val dragged by source.collectIsDraggedAsState()
    var lastTickBucket by remember { mutableIntStateOf(Int.MIN_VALUE) }

    // Quantized scrub ticks: one FrequentTick per twentieth of travel.
    LaunchedEffect(value, dragged) {
        if (!dragged) {
            lastTickBucket = Int.MIN_VALUE
            return@LaunchedEffect
        }
        val span = valueRange.endInclusive - valueRange.start
        val bucket = if (span <= 0f) {
            0
        } else {
            ((value - valueRange.start) / span * SCRUB_TICK_SEGMENTS).toInt()
        }
        if (bucket != lastTickBucket) {
            lastTickBucket = bucket
            haptics(MoriHaptic.FrequentTick)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (valueText != null) {
                Text(
                    text = valueText,
                    style = MoriEmphasized.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            onValueChangeFinished = {
                haptics(MoriHaptic.Tick)
                onValueChangeFinished()
            },
            interactionSource = source,
            thumb = { sliderState ->
                MoriSquigglySliderThumb(
                    sliderState = sliderState,
                    interactionSource = source,
                )
            },
            track = { sliderState ->
                MoriSquigglyTrack(
                    sliderState = sliderState,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Expressive Material 3 squiggly sine-wave track.
 *
 * The active range displays an animated fluid sine wave that smoothly connects to the
 * track origin and the thumb capsule via envelope tapering, and dynamically expands
 * in amplitude when dragging with expressive spring physics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoriSquigglyTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    stopIndicatorColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val isDragging = sliderState.isDragging
    val targetAmplitude = if (isDragging) 4.5.dp else 3.dp
    val amplitude by animateDpAsState(
        targetValue = targetAmplitude,
        animationSpec = MoriMotion.defaultSpatialSpec(),
        label = "squigglyAmp",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "squigglyWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "squigglyPhase",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val fraction = sliderState.coercedValueAsFraction.coerceIn(0f, 1f)
        val thumbX = width * fraction

        val strokeWidth = 4.dp.toPx()
        val ampPx = amplitude.toPx()
        val wavelengthPx = 22.dp.toPx()
        val rampPx = 14.dp.toPx()
        val thumbGapPx = 6.dp.toPx()

        // Inactive track (right of thumb)
        val inactiveStart = (thumbX + thumbGapPx).coerceAtMost(width)
        if (inactiveStart < width - 2.dp.toPx()) {
            drawLine(
                color = inactiveTrackColor,
                start = Offset(inactiveStart, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }

        // Active track (left of thumb)
        val activeEnd = (thumbX - thumbGapPx).coerceAtLeast(0f)
        if (activeEnd > 4.dp.toPx()) {
            val path = Path()
            path.moveTo(0f, centerY)
            val step = 2.dp.toPx().coerceAtLeast(2f)
            var currentX = 0f
            while (currentX < activeEnd) {
                currentX = (currentX + step).coerceAtMost(activeEnd)
                val distFromStart = currentX
                val distFromEnd = activeEnd - currentX
                val minEdge = minOf(distFromStart, distFromEnd)
                val envelope = if (minEdge < rampPx) {
                    val t = (minEdge / rampPx).coerceIn(0f, 1f)
                    t * t * (3f - 2f * t)
                } else {
                    1f
                }
                val waveAngle = (2 * PI * currentX / wavelengthPx - phase).toFloat()
                val y = centerY + ampPx * envelope * sin(waveAngle)
                path.lineTo(currentX, y)
            }
            path.lineTo(activeEnd, centerY)

            drawPath(
                path = path,
                color = activeTrackColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        } else if (thumbX > 0f) {
            drawLine(
                color = activeTrackColor,
                start = Offset(0f, centerY),
                end = Offset(thumbX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }

        // Anchoring stop indicators
        drawCircle(
            color = if (fraction > 0.02f) activeTrackColor else stopIndicatorColor,
            radius = 2.5.dp.toPx(),
            center = Offset(2.5.dp.toPx(), centerY),
        )
        drawCircle(
            color = stopIndicatorColor,
            radius = 2.5.dp.toPx(),
            center = Offset(width - 2.5.dp.toPx(), centerY),
        )
    }
}

/**
 * Expressive pill thumb that expands dynamically on touch / scrub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoriSquigglySliderThumb(
    sliderState: SliderState,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragging = sliderState.isDragging
    val active = isPressed || isDragging

    val width by animateDpAsState(
        targetValue = if (active) 6.dp else 4.dp,
        animationSpec = MoriMotion.defaultSpatialSpec(),
        label = "thumbWidth",
    )
    val height by animateDpAsState(
        targetValue = if (active) 26.dp else 20.dp,
        animationSpec = MoriMotion.defaultSpatialSpec(),
        label = "thumbHeight",
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (active) 3.dp else 2.dp,
        animationSpec = MoriMotion.defaultSpatialSpec(),
        label = "thumbCorner",
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .background(
                color = thumbColor,
                shape = RoundedCornerShape(cornerRadius),
            ),
    )
}

private const val SCRUB_TICK_SEGMENTS = 20
