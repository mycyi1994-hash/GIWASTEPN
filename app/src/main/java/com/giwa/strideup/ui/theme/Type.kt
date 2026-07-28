package com.giwa.strideup.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 테크 + 몽글몽글의 균형:
 *  - 큰 숫자·제목은 두툼한 Bold/ExtraBold + 타이트한 자간 (토스식 신뢰감).
 *  - 소형 라벨(눈썹)은 넓은 자간의 대문자 — 크립토 대시보드의 긴장감.
 */
val StrideUpTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1.8).sp,
        ),
        displayMedium = base.displayMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1.4).sp,
        ),
        displaySmall = base.displaySmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1.0).sp,
        ),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
        bodyLarge = base.bodyLarge.copy(lineHeight = 25.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp),
        bodySmall = base.bodySmall.copy(lineHeight = 18.sp, letterSpacing = 0.1.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.3.sp),
    )
}
