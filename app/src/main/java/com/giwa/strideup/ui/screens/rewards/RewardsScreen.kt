package com.giwa.strideup.ui.screens.rewards

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.data.local.RewardEntity
import com.giwa.strideup.data.local.RewardType
import com.giwa.strideup.ui.components.IconBadge
import com.giwa.strideup.ui.components.StrideCard
import com.giwa.strideup.ui.theme.NeonAmber
import com.giwa.strideup.ui.theme.NeonCyan
import com.giwa.strideup.ui.theme.NeonGreen
import com.giwa.strideup.ui.theme.NeonPurple
import com.giwa.strideup.ui.theme.NeonRed
import com.giwa.strideup.ui.theme.Night
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ledgerTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun RewardsScreen(viewModel: RewardsViewModel = viewModel(factory = RewardsViewModel.Factory)) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("리워드", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(NeonGreen, NeonCyan)),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "보유 SUP 포인트",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Night.copy(alpha = 0.7f),
                    )
                    Text(
                        "%,.2f SUP".format(balance),
                        style = MaterialTheme.typography.displayMedium,
                        color = Night,
                    )
                    Text(
                        "걸을수록 쌓이는 무브 투 언 리워드",
                        style = MaterialTheme.typography.bodySmall,
                        color = Night.copy(alpha = 0.7f),
                    )
                }
            }
        }

        item {
            StrideCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBadge(icon = Icons.Filled.AccountBalanceWallet, tint = NeonPurple)
                        Column(Modifier.weight(1f)) {
                            Text("GIWA 지갑", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "온체인 연동 준비 중",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonPurple,
                            )
                        }
                    }
                    Text(
                        "SUP 포인트는 추후 GIWA 체인의 온체인 토큰으로 전환·출금할 수 있게 될 예정이에요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("출금하기 (준비 중)")
                    }
                }
            }
        }

        item {
            Text("적립 내역", style = MaterialTheme.typography.titleMedium)
        }

        if (ledger.isEmpty()) {
            item {
                StrideCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("아직 적립 내역이 없어요", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "워킹 세션을 시작하고 첫 SUP를 적립해 보세요!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun LedgerRow(entry: RewardEntity) {
    val (icon, tint) = when (entry.type) {
        RewardType.EARN_WALK -> Icons.AutoMirrored.Filled.DirectionsWalk to NeonGreen
        RewardType.BONUS_GOAL -> Icons.Filled.EmojiEvents to NeonAmber
        RewardType.SPEND_UPGRADE -> Icons.Filled.Star to NeonPurple
        else -> Icons.Filled.Star to NeonCyan
    }
    StrideCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(icon = icon, tint = tint)
            Column(Modifier.weight(1f)) {
                Text(entry.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    ledgerTimeFormatter.format(Instant.ofEpochMilli(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                (if (entry.amount >= 0) "+" else "") + "%,.2f".format(entry.amount),
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.amount >= 0) NeonGreen else NeonRed,
            )
        }
    }
}
