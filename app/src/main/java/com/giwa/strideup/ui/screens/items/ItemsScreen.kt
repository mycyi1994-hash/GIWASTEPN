package com.giwa.strideup.ui.screens.items

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.StartRunButton
import com.giwa.strideup.ui.components.TokenCard
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

@Composable
fun ItemsScreen(viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory)) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val result by viewModel.upgradeResult.collectAsStateWithLifecycle()

    val comingSoon = stringResource(R.string.toast_coming_soon)
    val upgradedMsg = stringResource(R.string.toast_upgraded)
    val noBalanceMsg = stringResource(R.string.toast_no_balance)
    val showComingSoon = { Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show() }

    LaunchedEffect(result) {
        result?.let { success ->
            Toast.makeText(context, if (success) upgradedMsg else noBalanceMsg, Toast.LENGTH_SHORT).show()
            viewModel.consumeResult()
        }
    }

    val affordable = state.balance >= state.upgradeCost
    val progress = if (state.upgradeCost > 0) {
        (state.balance / state.upgradeCost).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
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
                TokenCard(balance = state.balance)
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

        item { GearCard(state, affordable, progress, viewModel::upgradeSneaker) }

        item {
            SectionHeader(title = stringResource(R.string.items_vault))
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                item {
                    VaultCard(
                        name = "Apex Runner",
                        owned = true,
                        subtitle = stringResource(R.string.level_chip, state.sneakerLevel),
                    )
                }
                item {
                    VaultCard(
                        name = "Neon Dash",
                        owned = false,
                        subtitle = stringResource(R.string.items_unlock_at, 5),
                    )
                }
                item {
                    VaultCard(
                        name = "Phantom Stride",
                        owned = false,
                        subtitle = stringResource(R.string.items_unlock_at, 10),
                    )
                }
            }
        }

        item { SectionHeader(title = stringResource(R.string.items_boosts)) }

        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 11.dp) {
                BoostRow(
                    icon = Icons.Filled.Bolt,
                    title = stringResource(R.string.boost_energy_title),
                    desc = stringResource(R.string.boost_energy_desc),
                    price = stringResource(R.string.price_sup, "50"),
                    onBuy = showComingSoon,
                )
                BoostRow(
                    icon = Icons.Filled.Shield,
                    title = stringResource(R.string.boost_shield_title),
                    desc = stringResource(R.string.boost_shield_desc),
                    price = stringResource(R.string.price_sup, "120"),
                    onBuy = showComingSoon,
                )
                BoostRow(
                    icon = Icons.Filled.AutoAwesome,
                    title = stringResource(R.string.boost_xp_title),
                    desc = stringResource(R.string.boost_xp_desc),
                    price = stringResource(R.string.price_sup, "200"),
                    onBuy = showComingSoon,
                )
            }
        }
    }
}

/** 내 장비 — 현재 스니커즈 + 업그레이드 (실제 SUP 차감) */
@Composable
private fun GearCard(
    state: ItemsViewModel.UiState,
    affordable: Boolean,
    progress: Float,
    onUpgrade: () -> Unit,
) {
    GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 14.dp) {
        SectionHeader(title = stringResource(R.string.items_my_gear))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                HexEmblem(size = 74.dp)
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = Snow,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("Apex Runner", style = MaterialTheme.typography.titleMedium, color = Snow)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Volt.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.level_chip, state.sneakerLevel),
                        color = Volt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Volt, modifier = Modifier.size(12.dp))
                        Text(
                            text = "×%.2f".format(state.multiplier),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Snow,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = Volt, modifier = Modifier.size(12.dp))
                        Text(
                            text = "%.0f".format(state.maxEnergy),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Snow,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    text = "%,.0f / %,.0f SUP".format(
                        state.balance.coerceAtMost(state.upgradeCost),
                        state.upgradeCost,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (affordable) Volt else Slate,
                )
            }
            BarMeter(fraction = progress, height = 7.dp)
        }

        VoltButton(
            text = stringResource(R.string.items_upgrade_cost, "%,.0f".format(state.upgradeCost)),
            onClick = onUpgrade,
            enabled = affordable,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.items_upgrade_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Slate,
        )
    }
}

@Composable
private fun VaultCard(name: String, owned: Boolean, subtitle: String) {
    GlowCard(
        modifier = Modifier.width(150.dp),
        contentPadding = PaddingValues(16.dp),
        spacing = 9.dp,
        accent = owned,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (owned) {
                Box(contentAlignment = Alignment.Center) {
                    HexEmblem(size = 58.dp)
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = Snow,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Box(contentAlignment = Alignment.Center) {
                    HexEmblem(size = 58.dp, glow = false)
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Slate,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = if (owned) Snow else Silver,
        )
        Text(
            text = if (owned) stringResource(R.string.items_owned) + " · " + subtitle else subtitle,
            fontSize = 11.sp,
            color = if (owned) Volt else Slate,
        )
    }
}

@Composable
private fun BoostRow(
    icon: ImageVector,
    title: String,
    desc: String,
    price: String,
    onBuy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonHigh.copy(alpha = 0.6f))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconSquare(icon = icon, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Snow)
            Text(desc, fontSize = 11.sp, color = Silver)
        }
        GhostButton(text = price, onClick = onBuy)
    }
}
