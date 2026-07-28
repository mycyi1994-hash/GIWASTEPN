package com.giwa.strideup.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** 화면 전체의 빛(글로우) 방향을 한곳에서 통일한다. */

/** 볼트 스윕 — 진행 링. 한 바퀴 돌며 라임의 밝기가 굽이친다. */
val VoltSweep: Brush = Brush.sweepGradient(
    0.00f to VoltDeep,
    0.30f to Volt,
    0.62f to VoltSoft,
    1.00f to VoltDeep,
)

/** 볼트 판 — START RUN 등 큰 CTA 면 */
val VoltPlate: Brush = Brush.linearGradient(
    listOf(VoltDeep, Volt, VoltSoft),
)

/** 볼트 텍스트 채움 */
val VoltInk: Brush = Brush.verticalGradient(
    listOf(VoltSoft, Volt),
)

/** 세로 볼트 — 차트 바 */
val VoltVertical: Brush = Brush.verticalGradient(
    listOf(VoltSoft, Volt),
)

/** 카드 표면 — 위가 미세하게 밝은 카본 */
val CardFill: Brush = Brush.verticalGradient(
    listOf(Color(0xFF15181B), Color(0xFF0F1113)),
)

/** 앱 배경 */
val NightBackdrop: Brush = Brush.verticalGradient(
    listOf(Color(0xFF090B0C), Night),
)

/** 양끝이 사라지는 헤어라인 */
val HairlineFade: Brush = Brush.horizontalGradient(
    listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
)
