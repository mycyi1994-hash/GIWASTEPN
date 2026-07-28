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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.data.local.RewardEntity
import com.giwa.strideup.data.local.RewardType
import com.giwa.strideup.ui.components.ChipButton
import com.giwa.strideup.ui.components.GuillochePattern
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.IconMedallion
import com.giwa.strideup.ui.components.ScreenTitle
import com.giwa.strideup.ui.components.SoftCard
import com.giwa.strideup.ui.components.VerticalHairline
import com.giwa.strideup.ui.components.sheen
import com.giwa.strideup.ui.theme.Coral
import com.giwa.strideup.ui.theme.Honey
import com.giwa.strideup.ui.theme.Ink
import com.giwa.strideup.ui.theme.Rose
import com.giwa.strideup.ui.theme.Sage
import com.giwa.strideup.ui.theme.SunsetPlate
import com.giwa.strideup.ui.theme.Taupe
import com.giwa.strideup.ui.theme.TaupeLight
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ledgerTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(ZoneId.systemDefault())

/** 스니커즈 레벨에 따른 멤버십 등급 */
private fun tierName(level: Int): String = when {
    level >= 7 -> "OBSIDIAN"
    level >= 5 -> "PLATINUM"
    level >= 3 -> "GOLD"
    level >= 2 -> "SILVER"
    else -> "CLASSIC"
}

@Composable
fun RewardsScreen(viewModel: RewardsViewModel = viewModel(factory = RewardsViewModel.Factory)) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val sneakerLevel by viewModel.sneakerLevel.collectAsStateWithLifecycle()

    val earned = ledger.filter { it.amount > 0 }.sumOf { it.amount }
    val spent = ledger.filter { it.amount < 0 }.sumOf { -it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenTitle(eyebrow = "Rewards", title = "내 리워드") }

        item { WalletHeroCard(balance = balance, level = sneakerLevel) }

        item { SummaryRow(earned = earned, spent = spent) }

        item { GiwaWalletCard() }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("적립 내역", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    text = "${ledger.size}건",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaupeLight,
                )
            }
        }

        if (ledger.isEmpty()) {
            item {
                SoftCard(contentPadding = PaddingValues(28.dp), spacing = 8.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "아직 적립 내역이 없어요",
                            style = MaterialTheme.typography.titleSmall,
                            color = Ink,
                        )
                        Text(
                            text = "산책 한 번이면 첫 SUP가 쌓여요. 가볍게 시작해 볼까요?",
                            style = MaterialTheme.typography.bodySmall,
                            color = Taupe,
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

/**
 * SUP 지갑 히어로 카드 — 화면에서 유일하게 진한 면.
 * 선셋 그라데이션 + 기요셰 각인 + 흐르는 광택.
 */
@Composable
private fun WalletHeroCard(balance: Double, level: Int) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                spotColor = Coral.copy(alpha = 0.45f),
                ambientColor = Coral.copy(alpha = 0.20f),
            )
            .background(SunsetPlate, shape)
            .clip(shape)
            .sheen(alpha = 0.20f, durationMillis = 5200),
    ) {
        GuillochePattern(Modifier.matchParentSize())

        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SUP 지갑",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "걷기만 해도 차곡차곡",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = tierName(level),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        color = Color.White,
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%,.2f".format(balance),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.8).sp,
                    color = Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SUP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 9.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.25f)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "LV $level 스니커즈",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Text(
                    text = "GIWA STEPN",
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** 누적 적립 / 사용 요약 */
@Composable
private fun SummaryRow(earned: Double, spent: Double) {
    SoftCard(contentPadding = PaddingValues(vertical = 18.dp, horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryCell(label = "지금까지 적립", value = "+%,.2f".format(earned), tint = Sage)
            VerticalHairline(height = 38.dp)
            SummaryCell(label = "지금까지 사용", value = "-%,.2f".format(spent), tint = Rose)
        }
    }
}

@Composable
private fun RowScope.SummaryCell(
    label: String,
    value: String,
    tint: Color,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Taupe,
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

/** GIWA 지갑 — 온체인 전환 준비 상태 */
@Composable
private fun GiwaWalletCard() {
    SoftCard(spacing = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconMedallion(icon = Icons.Filled.AccountBalanceWallet, tint = Sage, size = 44.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "GIWA 지갑",
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                )
                Text(
                    text = "온체인 연동 준비 중",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sage,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        HairlineDivider()
        Text(
            text = "조금만 기다려 주세요. 모아둔 SUP를 GIWA 체인 토큰으로 바꿀 수 있게 준비하고 있어요.",
            style = MaterialTheme.typography.bodySmall,
            color = Taupe,
        )
        ChipButton(text = "출금하기 (준비 중)", onClick = {}, enabled = false)
    }
}

@Composable
private fun LedgerRow(entry: RewardEntity) {
    val (icon, tint) = when (entry.type) {
        RewardType.EARN_WALK -> Icons.AutoMirrored.Filled.DirectionsWalk to Coral
        RewardType.BONUS_GOAL -> Icons.Filled.EmojiEvents to Honey
        RewardType.SPEND_UPGRADE -> Icons.Filled.Upgrade to Taupe
        else -> Icons.Filled.EmojiEvents to Sage
    }
    SoftCard(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconMedallion(icon = icon, tint = tint, size = 38.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                )
                Text(
                    text = ledgerTimeFormatter.format(Instant.ofEpochMilli(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TaupeLight,
                )
            }
            Text(
                text = (if (entry.amount >= 0) "+" else "") + "%,.2f".format(entry.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (entry.amount >= 0) Sage else Rose,
            )
        }
    }
}
