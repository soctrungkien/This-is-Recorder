package com.zeroui.kit.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import top.yukonga.miuix.kmp.theme.Colors

private const val ContainerDarken = 0.45f

private fun Color.darken(): Color = lerp(this, Color.Black, ContainerDarken)

/** Black page with darkened containers for the Miuix palette. */
internal fun Colors.pureBlack(): Colors = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = surfaceVariant.darken(),
    surfaceContainer = surfaceContainer.darken(),
    surfaceContainerHigh = surfaceContainerHigh.darken(),
    surfaceContainerHighest = surfaceContainerHighest.darken(),
)

/** Black page with darkened containers for the Material palette. */
internal fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceVariant = surfaceVariant.darken(),
    surfaceBright = surfaceBright.darken(),
    surfaceContainerHigh = surfaceContainerHigh.darken(),
    surfaceContainerHighest = surfaceContainerHighest.darken(),
)
