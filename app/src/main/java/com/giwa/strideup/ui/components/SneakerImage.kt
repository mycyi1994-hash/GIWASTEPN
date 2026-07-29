package com.giwa.strideup.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * 신발이 담기는 둥근 액자.
 *
 * 배경을 카드 아트와 같은 블랙으로 깔고 [ContentScale.Fit]으로 그려서,
 * 어떤 비율의 자리에 놓여도 신발이 위아래로 잘리지 않는다.
 * 남는 여백은 이미지 배경과 같은 검정이라 눈에 띄지 않는다.
 */
@Composable
fun SneakerFrame(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    animate: Boolean = false,
) {
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
