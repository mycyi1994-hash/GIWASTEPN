package com.giwa.strideup.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 화면 전체의 "빛의 방향"을 한곳에서 통일한다.
 * 면은 종이(크림·화이트), 강조는 선셋(코럴→살구)과 허니 그라데이션.
 */

/** 선셋 스윕 — 진행 링용. 한 바퀴 돌며 코럴에서 살구로 물든다. */
val SunsetSweep: Brush = Brush.sweepGradient(
    0.00f to CoralDeep,
    0.30f to Coral,
    0.65f to Peach,
    1.00f to CoralDeep,
)

/** 선셋 판 — 주 CTA·히어로 카드 등 가로로 긴 면 */
val SunsetPlate: Brush = Brush.linearGradient(
    listOf(CoralDeep, Coral, Peach),
)

/** 선셋 텍스트 채움 */
val SunsetInk: Brush = Brush.linearGradient(
    listOf(CoralDeep, Peach),
)

/** 세로 선셋 — 차트 바 등 세로 면 */
val SunsetVertical: Brush = Brush.verticalGradient(
    listOf(Peach, Coral),
)

/** 허니 판 — SUP 코인·게이지 */
val HoneyPlate: Brush = Brush.linearGradient(
    listOf(HoneyDeep, Honey, Peach),
)

/** 허니 텍스트 채움 — SUP 수치 */
val HoneyInk: Brush = Brush.verticalGradient(
    listOf(Honey, HoneyDeep),
)

/** 카드 표면 — 위가 미세하게 따뜻한 종이 질감 */
val CardSheen: Brush = Brush.verticalGradient(
    listOf(CardWarm, CardWhite),
)

/** 앱 배경 — 아래로 갈수록 가라앉는 크림 */
val CreamBackdrop: Brush = Brush.verticalGradient(
    listOf(Cream, CreamDeep),
)

/** 양끝이 사라지는 헤어라인 */
val HairlineFade: Brush = Brush.horizontalGradient(
    listOf(Color.Transparent, Ink.copy(alpha = 0.10f), Color.Transparent),
)
