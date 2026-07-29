package com.giwa.strideup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.domain.Faction
import com.giwa.strideup.domain.Rarity
import com.giwa.strideup.domain.Sneaker
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/** 희귀도 표시 색 — 속성색과 무관하게 등급을 한눈에 읽히게 한다. */
fun Rarity.tint(): Color = when (this) {
    Rarity.COMMON -> Color(0xFF9CA3AB)
    Rarity.RARE -> Color(0xFF4FB3FF)
    Rarity.EPIC -> Color(0xFFB47BFF)
    Rarity.LEGENDARY -> Color(0xFFFFC24F)
}

@Composable
fun Rarity.label(): String = stringResource(
    when (this) {
        Rarity.COMMON -> R.string.rarity_common
        Rarity.RARE -> R.string.rarity_rare
        Rarity.EPIC -> R.string.rarity_epic
        Rarity.LEGENDARY -> R.string.rarity_legendary
    }
)

/** 속성 표시색 — 도메인이 들고 있는 네온색을 그대로 쓴다. */
fun Faction.tint(): Color = Color(accent)

@Composable
fun Faction.label(): String = stringResource(
    when (this) {
        Faction.FIRE -> R.string.faction_fire
        Faction.WATER -> R.string.faction_water
        Faction.LIGHTNING -> R.string.faction_lightning
        Faction.WIND -> R.string.faction_wind
    }
)

/** 속성 pill 배지 */
@Composable
fun FactionChip(faction: Faction, modifier: Modifier = Modifier, small: Boolean = false) {
    val c = faction.tint()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.14f))
            .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = if (small) 7.dp else 10.dp, vertical = if (small) 2.dp else 4.dp),
    ) {
        Text(
            text = faction.label(),
            color = c,
            fontSize = if (small) 8.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/** 희귀도 pill 배지 */
@Composable
fun RarityChip(rarity: Rarity, modifier: Modifier = Modifier, small: Boolean = false) {
    val c = rarity.tint()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.15f))
            .border(1.dp, c.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = if (small) 7.dp else 10.dp, vertical = if (small) 2.dp else 4.dp),
    ) {
        Text(
            text = rarity.label(),
            color = c,
            fontSize = if (small) 8.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/**
 * 컬렉션 그리드에 쓰는 스니커즈 카드.
 * 희귀도가 카드 테두리와 배경 그라데이션을 결정해 수집 진열대처럼 보이게 한다.
 */
@Composable
fun SneakerCollectionCard(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    count: Int = 1,
    onClick: () -> Unit = {},
) {
    val rc = sneaker.rarity.tint()
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(rc.copy(alpha = 0.10f), Color(0xFF0F1113)),
                ),
                shape,
            )
            .border(1.dp, if (sneaker.equipped) Volt else rc.copy(alpha = 0.40f), shape)
            .quietClickable(onClick)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            RarityChip(sneaker.rarity, small = true)
            if (sneaker.equipped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Volt)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.items_equipped),
                        color = Night,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            SneakerVisual(
                sneaker = sneaker,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            if (count > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Night.copy(alpha = 0.82f))
                        .border(1.dp, Volt.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "×$count",
                        color = Volt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        Text(
            text = sneaker.variantName,
            style = MaterialTheme.typography.titleSmall,
            color = Snow,
            fontSize = 13.sp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sneaker.faction.label(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = sneaker.faction.tint(),
            )
            Text(
                text = "+%.1f%%".format(sneaker.boostPercent),
                fontSize = 11.sp,
                color = Silver,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CarbonHigh)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.level_chip, sneaker.level),
                    color = Volt,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(R.string.sneaker_mint_no, sneaker.mintNumber),
                fontSize = 9.sp,
                color = Slate,
            )
        }
    }
}

/** 아직 획득하지 못한 도감 슬롯 */
@Composable
fun SneakerLockedSlot(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF0C0E10), shape)
            .border(1.dp, Edge, shape)
            .height(210.dp),
        contentAlignment = Alignment.Center,
    ) {
        HexEmblem(size = 40.dp, glow = false)
    }
}

/** 스탯 한 줄 — 이름 · 막대 · 값 */
@Composable
fun StatBar(
    label: String,
    value: Double,
    max: Double,
    modifier: Modifier = Modifier,
    accent: Color = Volt,
    display: String = "%.2f".format(value),
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Silver)
            Text(
                text = display,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.07f)),
        ) {
            val fraction = (value / max).coerceIn(0.0, 1.0).toFloat()
            if (fraction > 0.002f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), accent)),
                        ),
                )
            }
        }
    }
}

/** 착용 중인 스니커즈 히어로 카드 */
@Composable
fun EquippedSneakerCard(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    GlowCard(
        modifier = modifier.quietClickable(onClick),
        accent = true,
        contentPadding = PaddingValues(18.dp),
        spacing = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "${sneaker.faction.label()} ${sneaker.variantName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Snow,
                )
                FactionChip(sneaker.faction, small = true)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RarityChip(sneaker.rarity)
                Text(
                    text = stringResource(R.string.sneaker_mint_no, sneaker.mintNumber),
                    fontSize = 10.sp,
                    color = Slate,
                )
            }
        }

        SneakerVisual(
            sneaker = sneaker,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(16.dp)),
            animate = true,
            contentScale = ContentScale.Fit,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SneakerMiniStat(
                label = stringResource(R.string.stat_boost),
                value = "+%.1f%%".format(sneaker.boostPercent),
                modifier = Modifier.weight(1f),
            )
            SneakerMiniStat(
                label = stringResource(R.string.stat_luck),
                value = "%.2f".format(sneaker.luck),
                modifier = Modifier.weight(1f),
            )
            SneakerMiniStat(
                label = stringResource(R.string.stat_comfort),
                value = "%.2f".format(sneaker.comfort),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SneakerMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonHigh)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = Slate, fontSize = 10.sp)
        Text(value, color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
