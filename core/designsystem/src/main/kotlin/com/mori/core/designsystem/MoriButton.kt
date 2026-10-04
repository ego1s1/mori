package com.mori.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val ExpressiveButtonShape = CircleShape
private val ExpressiveButtonMinHeight = 48.dp
private val ExpressiveIconButtonMinSize = 48.dp

/**
 * Unified Material 3 Expressive primary button: pill-shaped container, 48dp
 * touch target, and unified haptic feedback on press.
 */
@Composable
fun MoriPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ExpressiveButtonShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    haptic: MoriHaptic = MoriHaptic.PrimaryAction,
    content: @Composable RowScope.() -> Unit,
) {
    val haptics = rememberMoriHaptics()
    Button(
        onClick = {
            haptics(haptic)
            onClick()
        },
        modifier = modifier.sizeIn(minHeight = ExpressiveButtonMinHeight),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Unified Material 3 Expressive filled tonal button: softer surface tint with
 * pill geometry and tactile feedback.
 */
@Composable
fun MoriTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ExpressiveButtonShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    haptic: MoriHaptic = MoriHaptic.Select,
    content: @Composable RowScope.() -> Unit,
) {
    val haptics = rememberMoriHaptics()
    FilledTonalButton(
        onClick = {
            haptics(haptic)
            onClick()
        },
        modifier = modifier.sizeIn(minHeight = ExpressiveButtonMinHeight),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Unified Material 3 Expressive outlined button.
 */
@Composable
fun MoriOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ExpressiveButtonShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    haptic: MoriHaptic = MoriHaptic.Select,
    content: @Composable RowScope.() -> Unit,
) {
    val haptics = rememberMoriHaptics()
    OutlinedButton(
        onClick = {
            haptics(haptic)
            onClick()
        },
        modifier = modifier.sizeIn(minHeight = ExpressiveButtonMinHeight),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Unified Material 3 Expressive text button.
 */
@Composable
fun MoriTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ExpressiveButtonShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    haptic: MoriHaptic = MoriHaptic.Select,
    content: @Composable RowScope.() -> Unit,
) {
    val haptics = rememberMoriHaptics()
    TextButton(
        onClick = {
            haptics(haptic)
            onClick()
        },
        modifier = modifier.sizeIn(minHeight = ExpressiveButtonMinHeight),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Unified Material 3 Expressive icon button with guaranteed 48dp minimum
 * interactive bounds and light select haptic.
 */
@Composable
fun MoriIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    haptic: MoriHaptic = MoriHaptic.Select,
    content: @Composable () -> Unit,
) {
    val haptics = rememberMoriHaptics()
    IconButton(
        onClick = {
            haptics(haptic)
            onClick()
        },
        modifier = modifier.sizeIn(
            minWidth = ExpressiveIconButtonMinSize,
            minHeight = ExpressiveIconButtonMinSize,
        ),
        enabled = enabled,
        colors = colors,
        content = content,
    )
}

/**
 * Unified Material 3 Expressive filled tonal icon button with guaranteed 48dp
 * bounds and light select haptic.
 */
@Composable
fun MoriFilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    haptic: MoriHaptic = MoriHaptic.Select,
    content: @Composable () -> Unit,
) {
    val haptics = rememberMoriHaptics()
    FilledTonalIconButton(
        onClick = {
            haptics(haptic)
            onClick()
        },
        modifier = modifier.sizeIn(
            minWidth = ExpressiveIconButtonMinSize,
            minHeight = ExpressiveIconButtonMinSize,
        ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        content = content,
    )
}
