package com.giwa.strideup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StrideUpDarkColors = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Night,
    secondary = NeonCyan,
    onSecondary = Night,
    tertiary = NeonPurple,
    onTertiary = Night,
    background = Night,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    error = NeonRed,
    onError = Night,
    outline = Stroke,
    outlineVariant = Stroke,
)

/** StrideUp은 M2E 컨셉에 맞춰 항상 다크 테마를 사용한다. */
@Composable
fun StrideUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StrideUpDarkColors,
        typography = StrideUpTypography,
        content = content,
    )
}
