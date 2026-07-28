package com.giwa.strideup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StrideUpLightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = Sand,
    onPrimaryContainer = CoralDeep,
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = Sand,
    onSecondaryContainer = Ink,
    tertiary = Honey,
    onTertiary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = Taupe,
    surfaceContainer = CardWhite,
    surfaceContainerHigh = Cream,
    surfaceContainerHighest = Sand,
    error = Rose,
    onError = Color.White,
    outline = Border,
    outlineVariant = Border,
    scrim = Ink,
)

/** StrideUp은 따뜻한 크림 라이트 테마만 사용한다. */
@Composable
fun StrideUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StrideUpLightColors,
        typography = StrideUpTypography,
        shapes = StrideUpShapes,
        content = content,
    )
}
