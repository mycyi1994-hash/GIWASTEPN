package com.giwa.strideup.ui.screens.items

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.domain.BoostType
import com.giwa.strideup.domain.Faction
import com.giwa.strideup.domain.Rarity
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.domain.VARIANTS_PER_FACTION
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.EquippedSneakerCard
import com.giwa.strideup.ui.components.FactionChip
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.components.PillChip
import com.giwa.strideup.ui.components.RarityChip
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.SneakerCollectionCard
import com.giwa.strideup.ui.components.SneakerFrame
import com.giwa.strideup.ui.components.fullLabel
import com.giwa.strideup.ui.components.TokenCard
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.label
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.components.tint
import com.giwa.strideup.ui.guide.GuideTour
import com.giwa.strideup.ui.guide.guideTarget
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

@Composable
fun ItemsScreen(
    onOpenSneaker: (Long) -> Unit = {},
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
) {
    val context = LocalContext.current
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    var rarityFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var factionFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var copiesFor by rememberSaveable { mutableStateOf<String?>(null) }
    val equipped by viewModel.equipped.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val boosts by viewModel.activeBoosts.collectAsStateWithLifecycle()
    val progress by viewModel.collectionProgress.collectAsStateWithLifecycle()
    val factions by viewModel.factionProgress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val minted by viewModel.mintResult.collectAsStateWithLifecycle()

    val msgNoBalance = stringResource(R.string.toast_no_balance)
    val msgBoostActive = stringResource(R.string.toast_boost_active)
    val msgBoostBought = stringResource(R.string.toast_boost_bought)
    val msgMaxLevel = stringResource(R.string.toast_max_level)
    val msgUpgraded = stringResource(R.string.toast_upgraded)
    val equippedFmt = stringResource(R.string.toast_equipped, "%s")

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val text = when (m) {
            ItemsMessage.NotEnoughBalance -> msgNoBalance
            ItemsMessage.BoostAlreadyActive -> msgBoostActive
            ItemsMessage.BoostBought -> msgBoostBought
            ItemsMessage.MaxLevel -> msgMaxLevel
            is ItemsMessage.Upgraded -> msgUpgraded
            is ItemsMessage.Equipped -> equippedFmt.format(m.sneaker.fullLabel(context))
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(fontSize = 22.sp)
                TokenCard(balance = balance)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.tab_items),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.items_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
        }

        // ── 속성별 도감 진행도 — 탭하면 그 속성만 필터링 ─────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Faction.entries.forEach { faction ->
                    FactionProgressCell(
                        faction = faction,
                        owned = factions[faction] ?: 0,
                        total = VARIANTS_PER_FACTION,
                        selected = factionFilter == faction.id,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            factionFilter = if (factionFilter == faction.id) null else faction.id
                        },
                    )
                }
            }
        }

        // ── 필터: 등급 · 속성 ───────────────────────────────
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    PillChip(
                        text = stringResource(R.string.post_cat_all),
                        selected = rarityFilter == null && factionFilter == null,
                        onClick = {
                            rarityFilter = null
                            factionFilter = null
                        },
                    )
                }
                items(Rarity.entries.size) { i ->
                    val r = Rarity.entries[i]
                    PillChip(
                        text = r.label(),
                        selected = rarityFilter == r.id,
                        onClick = { rarityFilter = if (rarityFilter == r.id) null else r.id },
                    )
                }
                items(Faction.entries.size) { i ->
                    val f = Faction.entries[i]
                    PillChip(
                        text = f.label(),
                        selected = factionFilter == f.id,
                        onClick = { factionFilter = if (factionFilter == f.id) null else f.id },
                    )
                }
            }
        }

        // ── 컬렉션 헤더 — "N / 44 조합" ───────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.items_vault),
                    style = MaterialTheme.typography.titleMedium,
                    color = Snow,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.items_combo, progress.first, progress.second),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate,
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = Slate,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }

        // ── 착용 중인 스니커즈 ──────────────────────────────
        equipped?.let { sneaker ->
            item {
                EquippedSneakerCard(
                    sneaker = sneaker,
                    modifier = Modifier.guideTarget(GuideTour.Targets.ITEMS_EQUIPPED),
                    onClick = { onOpenSneaker(sneaker.id) },
                )
            }
            item {
                GlowCard(contentPadding = PaddingValues(16.dp), spacing = 11.dp) {
                    if (sneaker.canUpgrade) {
                        val cost = sneaker.upgradeCost
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.items_next_level),
                                style = MaterialTheme.typography.bodySmall,
                                color = Silver,
                            )
                            Text(
                                text = "%,.0f / %,.0f SUP".format(balance.coerceAtMost(cost), cost),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= cost) Volt else Slate,
                            )
                        }
                        BarMeter(
                            fraction = (balance / cost).coerceIn(0.0, 1.0).toFloat(),
                            height = 7.dp,
                        )
                        VoltButton(
                            text = stringResource(R.string.items_upgrade_cost, "%,.0f".format(cost)),
                            onClick = { viewModel.upgrade(sneaker.id) },
                            enabled = balance >= cost,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.items_upgrade_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.items_max_level),
                            style = MaterialTheme.typography.titleSmall,
                            color = Volt,
                        )
                    }
                }
            }
        }

        // ── 민팅 ────────────────────────────────────────────
        item {
            GlowCard(
                modifier = Modifier.guideTarget(GuideTour.Targets.ITEMS_MINT),
                contentPadding = PaddingValues(18.dp),
                spacing = 12.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    HexEmblem(size = 44.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = stringResource(R.string.items_mint_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Snow,
                        )
                        Text(
                            text = stringResource(R.string.items_mint_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Silver,
                        )
                    }
                }
                VoltButton(
                    text = stringResource(
                        R.string.items_mint_button,
                        "%,.0f".format(RewardEconomy.MINT_COST),
                    ),
                    onClick = { viewModel.mint() },
                    enabled = balance >= RewardEconomy.MINT_COST,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── 컬렉션 — 한 줄, 옆으로 밀어서 넘긴다 ───────────────
        item {
            val filtered = groups.filter { g ->
                (rarityFilter == null || g.representative.rarity.id == rarityFilter) &&
                    (factionFilter == null || g.representative.faction.id == factionFilter)
            }
            if (filtered.isEmpty()) {
                GlowCard(contentPadding = PaddingValues(24.dp)) {
                    Text(
                        text = stringResource(R.string.common_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.guideTarget(GuideTour.Targets.ITEMS_COLLECTION),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered.size, key = { filtered[it].representative.slotKey }) { index ->
                        val group = filtered[index]
                        SneakerCollectionCard(
                            sneaker = group.representative,
                            count = group.count,
                            modifier = Modifier.width(172.dp),
                            onClick = {
                                if (group.count == 1) {
                                    onOpenSneaker(group.representative.id)
                                } else {
                                    copiesFor = group.representative.slotKey
                                }
                            },
                        )
                    }
                }
            }
        }

        // ── 활성 부스트 ─────────────────────────────────────
        if (boosts.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.items_active_boosts)) }
            items(boosts.size) { index ->
                val boost = boosts[index]
                GlowCard(
                    accent = true,
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconSquare(icon = boostIcon(boost.type), size = 36.dp)
                        Text(
                            text = stringResource(boostTitle(boost.type)),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            color = Snow,
                        )
                        Text(
                            text = stringResource(
                                R.string.boost_time_left,
                                remainingLabel(boost.expiresAt),
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Volt,
                        )
                    }
                }
            }
        }

        // ── 부스트 상점 — 3개 카드 한 줄 ─────────────────────
        item {
            SectionHeader(
                title = stringResource(R.string.items_boosts),
                actionText = stringResource(R.string.common_more),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BoostType.entries.forEach { type ->
                    BoostCard(
                        type = type,
                        affordable = balance >= type.cost,
                        onBuy = { viewModel.buyBoost(type) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    // ── 사본 목록 ───────────────────────────────────────────
    copiesFor?.let { slot ->
        val copies = inventory.filter { it.slotKey == slot }
            .sortedWith(
                compareByDescending<com.giwa.strideup.domain.Sneaker> { it.equipped }
                    .thenByDescending { it.level }
                    .thenBy { it.mintNumber },
            )
        if (copies.isEmpty()) {
            copiesFor = null
        } else {
            CopiesDialog(
                copies = copies,
                onOpen = { id ->
                    copiesFor = null
                    onOpenSneaker(id)
                },
                onDismiss = { copiesFor = null },
            )
        }
    }

    // ── 민팅 결과 ───────────────────────────────────────────
    minted?.let { sneaker ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissMintResult() },
            containerColor = Carbon,
            titleContentColor = Snow,
            textContentColor = Silver,
            confirmButton = {
                TextButton(onClick = { viewModel.dismissMintResult() }) {
                    Text(stringResource(R.string.common_ok), color = Volt, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.mint_result_title),
                    fontWeight = FontWeight.Black,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FactionChip(sneaker.faction)
                        RarityChip(sneaker.rarity)
                    }
                    SneakerFrame(
                        sneaker = sneaker,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        corner = 16.dp,
                        animate = true,
                    )
                    Text(
                        text = sneaker.fullLabel(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Snow,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.sneaker_mint_no, sneaker.mintNumber),
                        fontSize = 11.sp,
                        color = Slate,
                    )
                }
            },
        )
    }
}

/** 속성 아이콘 */
private fun factionIcon(faction: Faction) = when (faction) {
    Faction.FIRE -> Icons.Filled.LocalFireDepartment
    Faction.WATER -> Icons.Filled.WaterDrop
    Faction.LIGHTNING -> Icons.Filled.Bolt
    Faction.WIND -> Icons.Filled.Air
}

/**
 * 속성 하나의 도감 진행도 카드.
 * 탭하면 그 속성만 보는 필터가 되고, 선택 중에는 볼트 테두리가 붙는다.
 */
@Composable
private fun FactionProgressCell(
    faction: Faction,
    owned: Int,
    total: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val c = faction.tint()
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(c.copy(alpha = if (selected) 0.13f else 0.07f))
            .border(
                width = 1.dp,
                color = if (selected) Volt else c.copy(alpha = 0.25f),
                shape = shape,
            )
            .quietClickable(onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(c.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = factionIcon(faction),
                contentDescription = faction.label(),
                tint = c,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = faction.label(),
            color = c,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
        Text(
            text = "$owned / $total",
            color = Snow,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        BarMeter(
            fraction = if (total == 0) 0f else (owned.toFloat() / total).coerceIn(0f, 1f),
            height = 4.dp,
            color = c,
        )
    }
}

/** 같은 도감 슬롯의 사본 목록 — 민팅 번호·레벨·부스트로 구분한다 */
@Composable
private fun CopiesDialog(
    copies: List<com.giwa.strideup.domain.Sneaker>,
    onOpen: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val first = copies.firstOrNull() ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_close),
                    color = Volt,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        title = {
            Text(
                text = first.fullLabel() + " ×${copies.size}",
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                copies.forEach { copy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CarbonHigh)
                            .quietClickable { onOpen(copy.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        SneakerFrame(
                            sneaker = copy,
                            modifier = Modifier.size(52.dp),
                            corner = 10.dp,
                            fade = false,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.level_chip, copy.level),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Volt,
                                )
                                if (copy.equipped) {
                                    Text(
                                        text = stringResource(R.string.items_equipped),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Volt,
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.sneaker_mint_no, copy.mintNumber) +
                                    "  ·  +%.1f%%".format(copy.boostPercent),
                                fontSize = 11.sp,
                                color = Silver,
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Slate,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun BoostCard(
    type: BoostType,
    affordable: Boolean,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CarbonHigh.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        // 볼트 링 안의 아이콘 — 목업의 원형 아이콘
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(1.5.dp, Volt.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = boostIcon(type),
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(boostTitle(type)),
            style = MaterialTheme.typography.titleSmall,
            color = Snow,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(boostDesc(type)),
            fontSize = 10.sp,
            color = Silver,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            minLines = 2,
        )
        GhostButton(
            text = stringResource(R.string.price_sup, "%,.0f".format(type.cost)),
            onClick = onBuy,
            enabled = affordable,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun boostIcon(type: BoostType): ImageVector = when (type) {
    BoostType.ENERGY_CELL -> Icons.Filled.Bolt
    BoostType.STREAK_SHIELD -> Icons.Filled.Shield
    BoostType.XP_BOOSTER -> Icons.Filled.AutoAwesome
}

private fun boostTitle(type: BoostType): Int = when (type) {
    BoostType.ENERGY_CELL -> R.string.boost_energy_title
    BoostType.STREAK_SHIELD -> R.string.boost_shield_title
    BoostType.XP_BOOSTER -> R.string.boost_xp_title
}

private fun boostDesc(type: BoostType): Int = when (type) {
    BoostType.ENERGY_CELL -> R.string.boost_energy_desc
    BoostType.STREAK_SHIELD -> R.string.boost_shield_desc
    BoostType.XP_BOOSTER -> R.string.boost_xp_desc
}

/** 남은 시간 라벨 (h/m) */
private fun remainingLabel(expiresAt: Long): String {
    val left = ((expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
    val h = left / 3600
    val m = (left % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
