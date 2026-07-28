package com.giwa.strideup.ui.screens.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.data.local.RewardEntity
import com.giwa.strideup.data.local.RewardType
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.VerticalHairline
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.sheen
import com.giwa.strideup.ui.theme.Alert
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import com.giwa.strideup.ui.theme.VoltPlate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ledgerTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M.d HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun WalletScreen(
    onBack: () -> Unit = {},
    viewModel: RewardsViewModel = viewModel(factory = RewardsViewModel.Factory),
) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()

    val earned = ledger.filter { it.amount > 0 }.sumOf { it.amount }
    val spent = ledger.filter { it.amount < 0 }.sumOf { -it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
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
                Wordmark(fontSize = 20.sp, modifier = Modifier.weight(1f))
            }
        }

        item {
            Text(
                text = stringResource(R.string.settings_wallet),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = Snow,
            )
        }

        item { BalanceHero(balance) }

        item { SummaryRow(earned = earned, spent = spent) }

        item { GiwaCard(balance) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.wallet_history),
                    style = MaterialTheme.typography.titleMedium,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.wallet_records, ledger.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
        }

        if (ledger.isEmpty()) {
            item {
                GlowCard(contentPadding = PaddingValues(26.dp), spacing = 6.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.wallet_empty_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Snow,
                        )
                        Text(
                            text = stringResource(R.string.wallet_empty_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = Silver,
                        )
                    }
                }
            }
        } else {
            items(ledger, key = { it.id }) { entry ->
                LedgerRow(entry)
            }
        }
    }
}

/** 잔액 히어로 — 볼트 플레이트 + 헥사곤 워터마크 + sheen */
@Composable
private fun BalanceHero(balance: Double) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VoltPlate, shape)
            .sheen(alpha = 0.20f, durationMillis = 5200),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val cx = size.width * 0.85f
            val cy = size.height * 0.35f
            repeat(3) { i ->
                drawCircle(
                    color = Night.copy(alpha = 0.10f),
                    radius = size.minDimension * (0.35f + i * 0.22f),
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f),
                )
            }
        }
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.wallet_balance),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Night.copy(alpha = 0.65f),
                    )
                    Text(
                        text = stringResource(R.string.wallet_tagline),
                        fontSize = 10.sp,
                        color = Night.copy(alpha = 0.5f),
                    )
                }
                HexEmblem(size = 30.dp, glow = false)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%,.2f".format(balance),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    color = Night,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SUP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Night.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                text = "≈ $%,.2f".format(balance * 0.01) + "  ·  +0.51%",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Night.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun SummaryRow(earned: Double, spent: Double) {
    GlowCard(contentPadding = PaddingValues(vertical = 17.dp, horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryCell(
                label = stringResource(R.string.wallet_earned),
                value = "+%,.2f".format(earned),
                tint = Volt,
            )
            VerticalHairline(height = 38.dp)
            SummaryCell(
                label = stringResource(R.string.wallet_spent),
                value = "-%,.2f".format(spent),
                tint = Alert,
            )
        }
    }
}

@Composable
private fun RowScope.SummaryCell(label: String, value: String, tint: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Silver)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = tint)
    }
}

@Composable
private fun GiwaCard(balance: Double) {
    var showWithdraw by rememberSaveable { mutableStateOf(false) }
    GlowCard(spacing = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            IconSquare(icon = Icons.Filled.AccountBalanceWallet, size = 42.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.wallet_giwa),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.wallet_giwa_status),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Volt,
                )
            }
        }
        Text(
            text = stringResource(R.string.wallet_giwa_body),
            style = MaterialTheme.typography.bodySmall,
            color = Silver,
        )
        GhostButton(
            text = stringResource(R.string.wallet_withdraw),
            onClick = { showWithdraw = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showWithdraw) {
        AlertDialog(
            onDismissRequest = { showWithdraw = false },
            containerColor = Carbon,
            titleContentColor = Snow,
            textContentColor = Silver,
            confirmButton = {
                TextButton(onClick = { showWithdraw = false }) {
                    Text(
                        text = stringResource(R.string.common_ok),
                        color = Volt,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            title = { Text(stringResource(R.string.wallet_withdraw_title), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        text = stringResource(R.string.wallet_withdraw_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                    Text(
                        text = stringResource(R.string.wallet_withdraw_min),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Snow,
                    )
                    if (balance >= 1_000.0) {
                        Text(
                            text = stringResource(R.string.wallet_withdraw_eligible),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Volt,
                        )
                    } else {
                        Text(
                            text = stringResource(
                                R.string.wallet_withdraw_short,
                                "%,.0f".format(1_000.0 - balance),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun LedgerRow(entry: RewardEntity) {
    val (icon, labelRes) = when (entry.type) {
        RewardType.EARN_WALK -> Icons.AutoMirrored.Filled.DirectionsWalk to R.string.ledger_earn_walk
        RewardType.BONUS_GOAL -> Icons.Filled.EmojiEvents to R.string.ledger_bonus_goal
        RewardType.SPEND_UPGRADE -> Icons.Filled.Upgrade to R.string.ledger_spend_upgrade
        else -> Icons.Filled.EmojiEvents to R.string.ledger_other
    }
    GlowCard(
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            IconSquare(icon = icon, size = 38.dp, tint = if (entry.amount >= 0) Volt else Alert)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Snow,
                )
                Text(
                    text = ledgerTimeFormatter.format(Instant.ofEpochMilli(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
            Text(
                text = (if (entry.amount >= 0) "+" else "") + "%,.2f".format(entry.amount),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (entry.amount >= 0) Volt else Alert,
            )
        }
    }
}
