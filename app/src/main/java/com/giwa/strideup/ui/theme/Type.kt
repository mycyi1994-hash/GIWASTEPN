package com.giwa.strideup.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 캐주얼 + 프리미엄 타이포의 균형:
 *  - 큰 숫자는 여전히 Light + 음수 자간(고급감의 뼈대) — 단, 과하지 않게.
 *  - 제목·라벨은 SemiBold로 한 단계 친근하게.
 *  - 본문은 넉넉한 행간으로 부담을 덜어낸다.
 */
val StrideUpTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontWeight = FontWeight.Light,
            letterSpacing = (-2.0).sp,
        ),
        displayMedium = base.displayMedium.copy(
            fontWeight = FontWeight.Light,
            letterSpacing = (-1.5).sp,
        ),
        displaySmall = base.displaySmall.copy(
            fontWeight = FontWeight.Light,
            letterSpacing = (-1.0).sp,
        ),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Normal),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Medium),
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        titleLarge = base.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        titleMedium = base.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp,
        ),
        titleSmall = base.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
        ),
        bodyLarge = base.bodyLarge.copy(lineHeight = 25.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp),
        bodySmall = base.bodySmall.copy(lineHeight = 19.sp, letterSpacing = 0.1.sp),
        labelLarge = base.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.0.sp,
        ),
        labelMedium = base.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.0.sp,
        ),
        labelSmall = base.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        ),
    )
}
