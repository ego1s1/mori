package com.mori.core.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Labeled slider row for settings sheets: label with a Flex value readout
 * above, full-width slider below. Used for continuous preferences like
 * display-filter channels.
 *
 * While scrubbing, quantized ticks fire at most once per twentieth of the
 * range in fixed quanta; release lands a single tick.
 */
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val SCRUB_TICK_SEGMENTS = 20
