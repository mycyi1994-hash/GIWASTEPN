package com.giwa.strideup.ui.screens.rewards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.data.local.RewardEntity
import com.giwa.strideup.data.local.RewardType
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GuillocheOverlay
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.IconMedallion
import com.giwa.strideup.ui.components.LuxeCard
import com.giwa.strideup.ui.components.ScreenTitle
import com.giwa.strideup.ui.components.VerticalHairline
import com.giwa.strideup.ui.components.sheen
import com.giwa.strideup.ui.theme.Ash
import com.giwa.strideup.ui.theme.AshDim
import com.giwa.strideup.ui.theme.Champagne
import com.giwa.strideup.ui.theme.Copper
import com.giwa.strideup.ui.theme.Crimson
import com.giwa.strideup.ui.theme.GoldEdge
import com.giwa.strideup.ui.theme.Ivory
import com.giwa.strideup.ui.theme.Jade
import com.giwa.strideup.ui.theme.MetalPlate
import com.giwa.strideup.ui.theme.ObsidianDeep
import com.giwa.strideup.ui.theme.Platinum
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenTitle(eyebrow = "Rewards", title = "리워드") }

        item { MembershipCard(balance = balance, level = sneakerLevel) }

        item { SummaryRow(earned = earned, spent = spent) }

        item { WalletCard() }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Eyebrow("Ledger")
                Text(
                    text = "${ledger.size}건",
                    style = MaterialTheme.typography.bodySmall,
                    color = AshDim,
                )
            }
        }

        if (ledger.isEmpty()) {
            item {
                LuxeCard(contentPadding = PaddingValues(28.dp), spacing = 8.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "아직 적립 내역이 없습니다",
                            style = MaterialTheme.typography.titleSmall,
                            color = Ivory,
                        )
                        Text(
                            text = "워킹 세션을 시작하고 첫 SUP를 적립해 보세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ash,
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
 * SUP 멤버십 카드.
 * 금속판 + 기요셰 각인 + 흐르는 광택 — 화면에서 유일하게 밝은 면이라 시선이 여기 머문다.
 */
@Composable
private fun MembershipCard(balance: Double, level: Int) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MetalPlate, shape)
            .sheen(alpha = 0.30f, durationMillis = 5200)
            .border(1.dp, GoldEdge, shape),
    ) {
        GuillocheOverlay(Modifier.matchParentSize())

        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = "STRIDEUP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 4.sp,
                        color = ObsidianDeep,
                    )
                    Text(
                        text = "MEMBER · ${tierName(level)}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.2.sp,
                        color = ObsidianDeep.copy(alpha = 0.55f),
                    )
                }
                // 카드 우상단 각인 엠블럼
                Canvas(Modifier.size(34.dp)) {
                    drawCircle(
                        color = ObsidianDeep.copy(alpha = 0.30f),
                        radius = size.minDimension / 2 - 1f,
                        style = Stroke(width = 1.2f),
                    )
                    drawArc(
                        color = ObsidianDeep.copy(alpha = 0.55f),
                        startAngle = -120f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = 2.4f, cap = StrokeCap.Round),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "SUP BALANCE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.4.sp,
                    color = ObsidianDeep.copy(alpha = 0.55f),
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%,.2f".format(balance),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp,
                        color = ObsidianDeep,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SUP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.6.sp,
                        color = ObsidianDeep.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 9.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ObsidianDeep.copy(alpha = 0.22f),
                                ObsidianDeep.copy(alpha = 0.04f),
                            ),
                        ),
                    ),
            )

            Text(
                text = "걸을수록 쌓이는 Move-to-Earn 리워드",
                fontSize = 11.sp,
                letterSpacing = 0.3.sp,
                color = ObsidianDeep.copy(alpha = 0.6f),
            )
        }
    }
}

/** 누적 적립 / 사용 요약 */
@Composable
private fun SummaryRow(earned: Double, spent: Double) {
    LuxeCard(contentPadding = PaddingValues(vertical = 18.dp, horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryCell(label = "누적 적립", value = "+%,.2f".format(earned), tint = Jade)
            VerticalHairline(height = 38.dp)
            SummaryCell(label = "누적 사용", value = "-%,.2f".format(spent), tint = Copper)
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
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Eyebrow(label, color = AshDim)
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            color = tint,
        )
    }
}

/** GIWA 지갑 — 온체인 전환 준비 상태 */
@Composable
private fun WalletCard() {
    LuxeCard(spacing = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconMedallion(icon = Icons.Filled.AccountBalanceWallet, tint = Platinum, size = 44.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "GIWA 지갑",
                    style = MaterialTheme.typography.titleSmall,
                    color = Ivory,
                )
                Eyebrow("온체인 연동 준비 중", color = Champagne.copy(alpha = 0.75f))
            }
        }
        HairlineDivider()
        Text(
            text = "SUP 포인트는 추후 GIWA 체인의 온체인 토큰으로 전환·출금할 수 있게 됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = Ash,
        )
        GhostButton(text = "출금하기 (준비 중)", onClick = {}, enabled = false)
    }
}

@Composable
private fun LedgerRow(entry: RewardEntity) {
    val (icon, tint) = when (entry.type) {
        RewardType.EARN_WALK -> Icons.AutoMirrored.Filled.DirectionsWalk to Champagne
        RewardType.BONUS_GOAL -> Icons.Filled.EmojiEvents to Jade
        RewardType.SPEND_UPGRADE -> Icons.Filled.Upgrade to Copper
        else -> Icons.Filled.EmojiEvents to Platinum
    }
    LuxeCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), shape = RoundedCornerShape(18.dp)) {
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
                    color = Ivory,
                )
                Text(
                    text = ledgerTimeFormatter.format(Instant.ofEpochMilli(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AshDim,
                )
            }
            Text(
                text = (if (entry.amount >= 0) "+" else "") + "%,.2f".format(entry.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = if (entry.amount >= 0) Ivory else Crimson,
            )
        }
    }
}
