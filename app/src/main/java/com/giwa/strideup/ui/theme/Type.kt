package com.giwa.strideup.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 고급 브랜드 타이포의 두 축:
 *  1) 큰 숫자·제목은 **가늘고 좁게** (Light + 음수 자간) — 두꺼울수록 저가로 읽힌다.
 *  2) 작은 라벨은 **넓은 자간의 대문자** — 여백이 곧 격이 된다.
 */
val StrideUpTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontWeight = FontWeight.Light,
            letterSpacing = (-2.5).sp,
        ),
        displayMedium = base.displayMedium.copy(
            fontWeight = FontWeight.Light,
            letterSpacing = (-1.8).sp,
        ),
        displaySmall = base.displaySmall.copy(
            fontWeight = FontWeight.Light,
            letterSpacing = (-1.0).sp,
        ),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Light),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Normal),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Normal),
        titleLarge = base.titleLarge.copy(
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
        ),
        titleMedium = base.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
        ),
        titleSmall = base.titleSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        ),
        bodyLarge = base.bodyLarge.copy(lineHeight = 25.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp),
        bodySmall = base.bodySmall.copy(lineHeight = 19.sp, letterSpacing = 0.2.sp),
        labelLarge = base.labelLarge.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
        ),
        labelMedium = base.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.4.sp,
        ),
        labelSmall = base.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
        ),
    )
}
