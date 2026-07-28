package com.giwa.strideup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StrideUpDarkColors = darkColorScheme(
    primary = Volt,
    onPrimary = Night,
    primaryContainer = CarbonHigh,
    onPrimaryContainer = VoltSoft,
    secondary = VoltSoft,
    onSecondary = Night,
    secondaryContainer = CarbonHigh,
    onSecondaryContainer = Snow,
    tertiary = Volt,
    onTertiary = Night,
    background = Night,
    onBackground = Snow,
    surface = Carbon,
    onSurface = Snow,
    surfaceVariant = CarbonHigh,
    onSurfaceVariant = Silver,
    surfaceContainer = Carbon,
    surfaceContainerHigh = CarbonHigh,
    surfaceContainerHighest = CarbonHigh,
    error = Alert,
    onError = Night,
    outline = Edge,
    outlineVariant = Edge,
    scrim = Night,
)

/** StrideUp은 네온이 살아나는 딥 블랙 다크 테마만 사용한다. */
@Composable
fun StrideUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StrideUpDarkColors,
        typography = StrideUpTypography,
        shapes = StrideUpShapes,
        content = content,
    )
}
