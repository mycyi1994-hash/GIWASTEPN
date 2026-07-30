package com.giwa.strideup.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * 신발 비주얼의 표준 프레임.
 *
 * 이미지 파일에 이미 알파 페더가 구워져 있어(배경은 검정, 가장자리는 투명)
 * 배경판이나 클립 없이 그대로 얹으면 어떤 카드 위에서도 이질감 없이 섞인다.
 * [ContentScale.Fit]이라 신발이 잘리지 않고, 남는 여백은 투명이라 보이지 않는다.
 */
@Composable
fun SneakerFrame(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    animate: Boolean = false,
    fade: Boolean = true,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SneakerVisual(
            sneaker = sneaker,
            modifier = Modifier.fillMaxSize(),
            animate = animate,
            contentScale = ContentScale.Fit,
        )
    }
}

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
