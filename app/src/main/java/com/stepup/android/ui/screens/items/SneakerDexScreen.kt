package com.stepup.android.ui.screens.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.Faction
import com.stepup.android.domain.Rarity
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.TOTAL_COLLECTION
import com.stepup.android.domain.VariantNames
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.PillChip
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.components.tint
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * NFT 도감 — 44칸 전부를 보여준다.
 *
 * 보유한 신발만 보여주는 보관함과 반대다. **아직 없는 것까지 보여주는 것이
 * 도감의 일**이다. 무엇이 비어 있는지 알아야 다음에 무엇을 노릴지 정할 수 있고,
 * 그게 민팅을 한 번 더 누르게 만드는 이유가 된다.
 *
 * 가진 것은 제 색으로, 없는 것은 회색 실루엣으로 그린다. 없는 칸을 아예
 * 비워 두면 "이 자리에 무엇이 오는가"를 알 수 없어 도감이 아니라 빈칸표가 된다.
 */
@Composable
fun SneakerDexScreen(
    onBack: () -> Unit = {},
    onOpenSneaker: (Long) -> Unit = {},
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
) {
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()

    var factionFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var rarityFilter by rememberSaveable { mutableStateOf<String?>(null) }
    // "가진 것만" / "없는 것만" — 도감에서 가장 자주 하는 질문이다.
    var ownedFilter by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // 보유 신발을 슬롯별로 묶는다. 같은 슬롯을 여러 장 가졌으면 레벨이 가장
    // 높은 것을 대표로 세운다 — 도감에서 보고 싶은 것은 "내 최고의 그 신발"이다.
    val owned = remember(inventory) {
        inventory.groupBy { it.slotKey }.mapValues { (_, list) -> list.maxBy { it.level } }
    }

    val slots = remember(owned, factionFilter, rarityFilter, ownedFilter) {
        dexSlots().filter { slot ->
            (factionFilter == null || slot.faction.id == factionFilter) &&
                (rarityFilter == null || slot.rarity.id == rarityFilter) &&
                (ownedFilter == null || owned.containsKey(slot.key) == ownedFilter)
        }
    }

    val ownedCount = owned.keys.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Text(
                    text = stringResource(R.string.dex_title),
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Snow,
                )
            }
        }

        // ── 전체 진행도 ──
        item {
            GlowCard(accent = true, contentPadding = PaddingValues(16.dp), spacing = 9.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = stringResource(R.string.dex_progress),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                    Text(
                        text = "$ownedCount / $TOTAL_COLLECTION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Volt,
                    )
                }
                BarMeter(
                    fraction = ownedCount.toFloat() / TOTAL_COLLECTION.coerceAtLeast(1),
                    height = 7.dp,
                )
            }
        }

        // ── 필터 ──
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    PillChip(
                        text = stringResource(R.string.dex_filter_all),
                        selected = factionFilter == null && rarityFilter == null && ownedFilter == null,
                        onClick = {
                            factionFilter = null
                            rarityFilter = null
                            ownedFilter = null
                        },
                    )
                }
                item {
                    PillChip(
                        text = stringResource(R.string.dex_filter_owned),
                        selected = ownedFilter == true,
                        onClick = { ownedFilter = if (ownedFilter == true) null else true },
                    )
                }
                item {
                    PillChip(
                        text = stringResource(R.string.dex_filter_missing),
                        selected = ownedFilter == false,
                        onClick = { ownedFilter = if (ownedFilter == false) null else false },
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Faction.entries.toList()) { faction ->
                    PillChip(
                        text = faction.label(),
                        selected = factionFilter == faction.id,
                        onClick = {
                            factionFilter = if (factionFilter == faction.id) null else faction.id
                        },
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Rarity.entries.toList()) { rarity ->
                    PillChip(
                        text = rarity.label(),
                        selected = rarityFilter == rarity.id,
                        onClick = {
                            rarityFilter = if (rarityFilter == rarity.id) null else rarity.id
                        },
                    )
                }
            }
        }

        if (slots.isEmpty()) {
            item {
                GlowCard(contentPadding = PaddingValues(26.dp)) {
                    Text(
                        text = stringResource(R.string.dex_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // 3칸씩. LazyVerticalGrid 를 쓰지 않는 이유는 이 화면이 이미
        // LazyColumn 안이기 때문이다 — 스크롤 컨테이너를 겹치면 높이 계산이 깨진다.
        items(slots.chunked(3)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { slot ->
                    DexCell(
                        slot = slot,
                        sneaker = owned[slot.key],
                        modifier = Modifier.weight(1f),
                        onClick = { owned[slot.key]?.let { onOpenSneaker(it.id) } },
                    )
                }
                // 마지막 줄이 3칸을 못 채우면 남는 자리를 비워 둔다.
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** 도감 한 칸이 가리키는 조합 */
private data class DexSlot(
    val faction: Faction,
    val rarity: Rarity,
    val variant: Int,
) {
    val key: String get() = "${faction.id}:${rarity.id}:$variant"
}

/** 44칸 전체 — 속성 × 등급 × 변형 */
private fun dexSlots(): List<DexSlot> = Faction.entries.flatMap { faction ->
    Rarity.entries.flatMap { rarity ->
        (0 until rarity.variantCount).map { variant -> DexSlot(faction, rarity, variant) }
    }
}

/**
 * 도감 한 칸.
 *
 * @param sneaker 가지고 있으면 그 신발, 없으면 null
 */
@Composable
private fun DexCell(
    slot: DexSlot,
    sneaker: Sneaker?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hasIt = sneaker != null
    // 없는 칸을 그릴 견본. 스탯은 쓰지 않으므로 기본값으로 둔다.
    val sample = remember(slot) {
        sneaker ?: Sneaker(
            id = -1,
            faction = slot.faction,
            rarity = slot.rarity,
            variant = slot.variant,
            level = 1,
            mintNumber = 0,
            luck = 1.0,
            comfort = 1.0,
            durability = 100,
            equipped = false,
            acquiredAt = 0,
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonHigh)
            .border(
                width = if (hasIt) 1.dp else 1.dp,
                color = if (hasIt) slot.faction.tint().copy(alpha = 0.55f) else Edge,
                shape = RoundedCornerShape(16.dp),
            )
            .quietClickable(onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            // 없는 칸은 채도를 0으로 죽이고 어둡게 눌러 회색 실루엣으로 만든다.
            // 형태는 남으므로 "이 자리에 무엇이 오는가"는 그대로 읽힌다.
            SneakerFrame(
                sneaker = sample,
                modifier = if (hasIt) Modifier.fillMaxSize() else Modifier.fillMaxSize().grayedOut(),
                corner = 12.dp,
            )
            if (!hasIt) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Slate,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(13.dp),
                )
            }
        }

        Text(
            text = VariantNames.of(slot.rarity, slot.variant),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (hasIt) Snow else Slate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (hasIt) {
                stringResource(R.string.level_chip, sneaker.level)
            } else {
                slot.rarity.label()
            },
            fontSize = 9.sp,
            color = if (hasIt) slot.faction.tint() else Slate,
            maxLines = 1,
        )
    }
}

/**
 * 아직 없는 칸을 회색으로.
 *
 * 그린 결과 위에 **채도 0인 회색을 Saturation 블렌드로** 덮는다. 색만 빠지고
 * 형태와 명암은 그대로 남아, 무엇이 오는 자리인지는 계속 읽힌다.
 *
 * 신발은 이미지일 때도 있고 Canvas 로 그린 그림일 때도 있어서, 컴포저블마다
 * 색을 바꾸는 방법이 다르다. 이렇게 그려진 결과에 거는 방식이면 둘 다 한 번에
 * 먹는다. 블렌드는 별도 레이어에서 합성해야 해서 Offscreen 을 지정한다.
 */
private fun Modifier.grayedOut(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(Color(0xFF8A9199), blendMode = BlendMode.Saturation)
    }
    .alpha(0.5f)
