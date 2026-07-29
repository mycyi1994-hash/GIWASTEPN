package com.giwa.strideup.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.giwa.strideup.R
import com.giwa.strideup.domain.Faction
import com.giwa.strideup.domain.Rarity
import com.giwa.strideup.domain.Sneaker

/**
 * 포스터에서 잘라낸 실사 신발 이미지.
 *
 * 불·물 포스터는 11종 전부, 번개·바람 포스터는 노말 3종 + 희귀 3종만 있으므로
 * 이미지가 없는 슬롯(번개/바람의 레어·전설)은 null을 돌려주고
 * 호출부는 Canvas 아트(SneakerArt)로 폴백한다.
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
            Rarity.EPIC -> listOf(
                R.drawable.sneaker_lightning_epic_0,
                R.drawable.sneaker_lightning_epic_1,
                R.drawable.sneaker_lightning_epic_2,
            )[v]
            else -> null
        }

        Faction.WIND -> when (rarity) {
            Rarity.COMMON -> listOf(
                R.drawable.sneaker_wind_common_0,
                R.drawable.sneaker_wind_common_1,
                R.drawable.sneaker_wind_common_2,
            )[v]
            Rarity.EPIC -> listOf(
                R.drawable.sneaker_wind_epic_0,
                R.drawable.sneaker_wind_epic_1,
                R.drawable.sneaker_wind_epic_2,
            )[v]
            else -> null
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
 * 신발 비주얼 — 포스터 이미지가 있으면 이미지를, 없으면 Canvas 아트를 그린다.
 * contentScale 기본은 Crop, 히어로 컨텍스트에서는 Fit으로 전체를 보여준다.
 */
@Composable
fun SneakerVisual(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
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
