package com.giwa.strideup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StrideUpDarkColors = darkColorScheme(
    primary = Champagne,
    onPrimary = ObsidianDeep,
    primaryContainer = CharcoalHigh,
    onPrimaryContainer = ChampagneLight,
    secondary = Platinum,
    onSecondary = ObsidianDeep,
    secondaryContainer = CharcoalHigh,
    onSecondaryContainer = Platinum,
    tertiary = Jade,
    onTertiary = ObsidianDeep,
    background = Obsidian,
    onBackground = Ivory,
    surface = Charcoal,
    onSurface = Ivory,
    surfaceVariant = CharcoalHigh,
    onSurfaceVariant = Ash,
    surfaceContainer = Charcoal,
    surfaceContainerHigh = CharcoalHigh,
    surfaceContainerHighest = CharcoalHigh,
    error = Crimson,
    onError = Ivory,
    outline = Hairline,
    outlineVariant = Hairline,
    scrim = ObsidianDeep,
)

/** StrideUp은 금속 광택이 살아나는 다크 테마만 사용한다. */
@Composable
fun StrideUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StrideUpDarkColors,
        typography = StrideUpTypography,
        shapes = StrideUpShapes,
        content = content,
    )
}
