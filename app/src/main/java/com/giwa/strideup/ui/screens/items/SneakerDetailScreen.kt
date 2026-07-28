package com.giwa.strideup.ui.screens.items

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.FactionChip
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.RarityChip
import com.giwa.strideup.ui.components.SneakerHero
import com.giwa.strideup.ui.components.StatBar
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.label
import com.giwa.strideup.ui.components.tint
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

@Composable
fun SneakerDetailScreen(
    sneakerId: Long,
    onBack: () -> Unit = {},
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
) {
    val context = LocalContext.current
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val sneaker = inventory.firstOrNull { it.id == sneakerId }

    val msgNoBalance = stringResource(R.string.toast_no_balance)
    val msgMaxLevel = stringResource(R.string.toast_max_level)
    val msgUpgraded = stringResource(R.string.toast_upgraded)
    val equippedFmt = stringResource(R.string.toast_equipped, "%s")

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val text = when (m) {
            ItemsMessage.NotEnoughBalance -> msgNoBalance
            ItemsMessage.MaxLevel -> msgMaxLevel
            is ItemsMessage.Upgraded -> msgUpgraded
            is ItemsMessage.Equipped -> equippedFmt.format(m.name)
            else -> null
        }
        if (text != null) Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    text = sneaker?.let { "${it.faction.label()} ${it.variantName}" }.orEmpty(),
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Snow,
                )
            }
        }

        if (sneaker == null) {
            item {
                GlowCard(contentPadding = PaddingValues(24.dp)) {
                    Text(
                        text = stringResource(R.string.common_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                }
            }
            return@LazyColumn
        }

        // 히어로
        item {
            GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 13.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FactionChip(sneaker.faction)
                        RarityChip(sneaker.rarity)
                    }
                    Text(
                        text = stringResource(R.string.sneaker_mint_no, sneaker.mintNumber),
                        fontSize = 11.sp,
                        color = Slate,
                    )
                }
                SneakerHero(
                    sneaker = sneaker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = sneaker.variantName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Snow,
                    )
                    Text(
                        text = stringResource(R.string.level_chip, sneaker.level) +
                            " · " + stringResource(R.string.items_owned),
                        fontSize = 12.sp,
                        color = Silver,
                    )
                }
            }
        }

        // 스탯
        item {
            GlowCard(contentPadding = PaddingValues(18.dp), spacing = 14.dp) {
                Text(
                    text = stringResource(R.string.sneaker_stats),
                    style = MaterialTheme.typography.titleMedium,
                    color = Snow,
                )
                StatBar(
                    label = stringResource(R.string.sneaker_earning),
                    value = sneaker.boostPercent,
                    max = 20.0,
                    accent = sneaker.faction.tint(),
                    display = "+%.1f%%".format(sneaker.boostPercent),
                )
                StatBar(
                    label = stringResource(R.string.stat_luck),
                    value = sneaker.luck,
                    max = 2.5,
                    accent = sneaker.rarity.tint(),
                )
                StatBar(
                    label = stringResource(R.string.stat_comfort),
                    value = sneaker.comfort,
                    max = 2.5,
                    accent = sneaker.rarity.tint(),
                )
                StatBar(
                    label = stringResource(R.string.sneaker_energy_saving),
                    value = (1.0 - sneaker.energyEfficiency) * 100,
                    max = 15.0,
                    accent = Volt,
                    display = "%.0f%%".format((1.0 - sneaker.energyEfficiency) * 100),
                )
                StatBar(
                    label = stringResource(R.string.sneaker_durability),
                    value = sneaker.durability.toDouble(),
                    max = 100.0,
                    accent = Volt,
                    display = "${sneaker.durability}/100",
                )
            }
        }

        // 액션
        item {
            GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
                if (!sneaker.equipped) {
                    VoltButton(
                        text = stringResource(R.string.items_equip),
                        onClick = { viewModel.equip(sneaker.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
                    BarMeter(fraction = (balance / cost).coerceIn(0.0, 1.0).toFloat(), height = 7.dp)
                    GhostButton(
                        text = stringResource(R.string.items_upgrade_cost, "%,.0f".format(cost)),
                        onClick = { viewModel.upgrade(sneaker.id) },
                        enabled = balance >= cost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.items_max_level),
                            style = MaterialTheme.typography.titleSmall,
                            color = Volt,
                        )
                    }
                }
            }
        }

        // 설명
        item {
            GlowCard(contentPadding = PaddingValues(18.dp), spacing = 8.dp) {
                Text(
                    text = stringResource(R.string.sneaker_about),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.sneaker_about_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
        }
    }
}
