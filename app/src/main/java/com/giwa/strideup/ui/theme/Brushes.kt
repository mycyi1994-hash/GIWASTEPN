package com.giwa.strideup.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 고급감을 만드는 핵심은 "단색"이 아니라 "면의 반사"다.
 * 금속·유리 표현에 쓰는 브러시를 한곳에 모아 화면 전체의 광택 방향을 통일한다.
 */

/** 브러시드 메탈 스윕 — 링/원형 요소용. 한 바퀴에 광택이 두 번 스친다. */
val MetalSweep: Brush = Brush.sweepGradient(
    0.00f to ChampagneDeep,
    0.14f to Champagne,
    0.26f to ChampagneLight,
    0.42f to Champagne,
    0.58f to ChampagneDeep,
    0.72f to Champagne,
    0.86f to ChampagneLight,
    1.00f to ChampagneDeep,
)

/** 금속 판 — pill 버튼·멤버십 카드 등 가로로 긴 면 */
val MetalPlate: Brush = Brush.linearGradient(
    listOf(ChampagneDeep, Champagne, ChampagneLight, Champagne, ChampagneDeep),
)

/** 금속 텍스트 채움 — 위에서 빛을 받는 각인 느낌 */
val MetalInk: Brush = Brush.verticalGradient(
    listOf(ChampagneLight, Champagne, ChampagneDeep),
)

/** 유리 표면 — 위가 밝고 아래로 사라지는 아주 옅은 오버레이 */
val GlassFill: Brush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.055f), Color.White.copy(alpha = 0.010f)),
)

/** 유리 모서리 — 좌상단이 밝은 1px 엣지 라이팅 */
val GlassEdge: Brush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.16f),
        Color.White.copy(alpha = 0.04f),
        Color.White.copy(alpha = 0.10f),
    ),
)

/** 금색 모서리 — 강조 카드용 */
val GoldEdge: Brush = Brush.linearGradient(
    listOf(
        Champagne.copy(alpha = 0.55f),
        Champagne.copy(alpha = 0.10f),
        ChampagneLight.copy(alpha = 0.38f),
    ),
)

/** 헤어라인 — 가운데가 진하고 양끝이 사라지는 구분선 */
val HairlineFade: Brush = Brush.horizontalGradient(
    listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
)

/** 앱 배경 — 위에서 아래로 가라앉는 흑요석 */
val ObsidianBackdrop: Brush = Brush.verticalGradient(
    listOf(ObsidianTop, Obsidian, ObsidianDeep),
)

/** 하단 네비게이션 유리판 */
val NavGlass: Brush = Brush.verticalGradient(
    listOf(Charcoal.copy(alpha = 0.94f), ObsidianDeep),
)
