package com.giwa.strideup.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.giwa.strideup.R
import com.giwa.strideup.domain.Faction
import com.giwa.strideup.domain.Rarity
import com.giwa.strideup.domain.Sneaker

/**
 * 포스터에서 잘라낸 실사 신발 이미지.
 *
 * 44개 도감 슬롯 전부에 실제 이미지가 있다. 번개·바람의 레어·전설은
 * STRIDE VAULT 시트(#06~#10, #16~#20)를 팩션 색상으로 돌려 채웠다.
 */
@DrawableRes
fun sneakerImageRes(faction: Faction, rarity: Rarity, variant: Int): Int? {
    val v = variant.coerceIn(0, rarity.variantCount - 1)
    return when (faction) {
        Faction.FIRE -> when (rarity) {
            Rarity.COMMON -> listOf(
                R.drawable.sneaker_fire_common_0,
                R.drawable.sneaker_fire_common_1,
                R.drawable.sneaker_fire_common_2,
            )[v]
            Rarity.RARE -> listOf(
                R.drawable.sneaker_fire_rare_0,
                R.drawable.sneaker_fire_rare_1,
                R.drawable.sneaker_fire_rare_2,
            )[v]
            Rarity.EPIC -> listOf(
                R.drawable.sneaker_fire_epic_0,
                R.drawable.sneaker_fire_epic_1,
                R.drawable.sneaker_fire_epic_2,
            )[v]
            Rarity.LEGENDARY -> listOf(
                R.drawable.sneaker_fire_legendary_0,
                R.drawable.sneaker_fire_legendary_1,
            )[v]
        }

        Faction.WATER -> when (rarity) {
            Rarity.COMMON -> listOf(
                R.drawable.sneaker_water_common_0,
                R.drawable.sneaker_water_common_1,
                R.drawable.sneaker_water_common_2,
            )[v]
            Rarity.RARE -> listOf(
                R.drawable.sneaker_water_rare_0,
                R.drawable.sneaker_water_rare_1,
                R.drawable.sneaker_water_rare_2,
            )[v]
            Rarity.EPIC -> listOf(
                R.drawable.sneaker_water_epic_0,
                R.drawable.sneaker_water_epic_1,
                R.drawable.sneaker_water_epic_2,
            )[v]
            Rarity.LEGENDARY -> listOf(
                R.drawable.sneaker_water_legendary_0,
                R.drawable.sneaker_water_legendary_1,
            )[v]
        }

        Faction.LIGHTNING -> when (rarity) {
            Rarity.COMMON -> listOf(
                R.drawable.sneaker_lightning_common_0,
                R.drawable.sneaker_lightning_common_1,
                R.drawable.sneaker_lightning_common_2,
            )[v]
            Rarity.RARE -> listOf(
                R.drawable.sneaker_lightning_rare_0,
                R.drawable.sneaker_lightning_rare_1,
                R.drawable.sneaker_lightning_rare_2,
            )[v]
            Rarity.EPIC -> listOf(
                R.drawable.sneaker_lightning_epic_0,
                R.drawable.sneaker_lightning_epic_1,
                R.drawable.sneaker_lightning_epic_2,
            )[v]
            Rarity.LEGENDARY -> listOf(
                R.drawable.sneaker_lightning_legendary_0,
                R.drawable.sneaker_lightning_legendary_1,
            )[v]
        }

        Faction.WIND -> when (rarity) {
            Rarity.COMMON -> listOf(
                R.drawable.sneaker_wind_common_0,
                R.drawable.sneaker_wind_common_1,
                R.drawable.sneaker_wind_common_2,
            )[v]
            Rarity.RARE -> listOf(
                R.drawable.sneaker_wind_rare_0,
                R.drawable.sneaker_wind_rare_1,
                R.drawable.sneaker_wind_rare_2,
            )[v]
            Rarity.EPIC -> listOf(
                R.drawable.sneaker_wind_epic_0,
                R.drawable.sneaker_wind_epic_1,
                R.drawable.sneaker_wind_epic_2,
            )[v]
            Rarity.LEGENDARY -> listOf(
                R.drawable.sneaker_wind_legendary_0,
                R.drawable.sneaker_wind_legendary_1,
            )[v]
        }
    }
}

/** 이미지가 있는 모든 도감 슬롯 — 스플래시 로테이션 등에 쓴다 */
val AllSneakerImages: List<Int> by lazy {
    buildList {
        for (f in Faction.entries) {
            for (r in Rarity.entries) {
                for (v in 0 until r.variantCount) {
                    sneakerImageRes(f, r, v)?.let(::add)
                }
            }
        }
    }
}

/**
 * 이미지 가장자리를 투명으로 녹여 뒤 배경과 이질감 없이 섞는다.
 *
 * 사진을 잘라 붙인 듯한 직사각형 경계가 사라지고, 신발과 발광 링만
 * 배경 위에 떠 있는 것처럼 보인다. 어떤 배경(카드 그라데이션, 스플래시의
 * 스피드 라인) 위에서도 통한다 — 색을 맞추는 게 아니라 알파를 지우기 때문.
 */
fun Modifier.fadedEdges(fraction: Float = 0.16f): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fx = size.width * fraction
        val fy = size.height * fraction
        // 각 변에서 안쪽으로 알파를 지운다 (검정 = 완전히 지움)
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Black, Color.Transparent),
                startX = 0f,
                endX = fx,
            ),
            blendMode = BlendMode.DstOut,
        )
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.Black),
                startX = size.width - fx,
                endX = size.width,
            ),
            blendMode = BlendMode.DstOut,
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black, Color.Transparent),
                startY = 0f,
                endY = fy,
            ),
            blendMode = BlendMode.DstOut,
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black),
                startY = size.height - fy,
                endY = size.height,
            ),
            blendMode = BlendMode.DstOut,
        )
    }

/**
 * 신발 비주얼의 표준 프레임.
 *
 * 크롭 원본과 같은 4:3 비율을 유지한 채 자리 안에 가운데 놓고,
 * 가장자리를 [fadedEdges]로 녹여 어떤 배경과도 자연스럽게 섞는다.
 * 목록 썸네일처럼 아주 작은 자리는 [fade]를 끄면 기존의
 * 둥근 액자(검정 배경 + 클립)로 그려진다.
 */
@Composable
fun SneakerFrame(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    animate: Boolean = false,
    fade: Boolean = true,
) {
    if (fade) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            SneakerVisual(
                sneaker = sneaker,
                modifier = Modifier
                    .aspectRatio(4f / 3f)
                    .fadedEdges(),
                animate = animate,
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(corner))
                .background(SneakerBackdrop),
            contentAlignment = Alignment.Center,
        ) {
            SneakerVisual(
                sneaker = sneaker,
                modifier = Modifier.fillMaxSize(),
                animate = animate,
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** 크롭 이미지의 배경과 같은 톤 — 레터박스가 티나지 않게 */
private val SneakerBackdrop = Color(0xFF0A0A0A)

/**
 * 신발 비주얼 — 포스터 이미지가 있으면 이미지를, 없으면 Canvas 아트를 그린다.
 * 기본은 Fit이라 이미지가 잘리지 않는다.
 */
@Composable
fun SneakerVisual(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val res = sneakerImageRes(sneaker.faction, sneaker.rarity, sneaker.variant)
    if (res != null) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else if (animate) {
        SneakerHero(sneaker = sneaker, modifier = modifier)
    } else {
        SneakerArt(sneaker = sneaker, modifier = modifier)
    }
}
