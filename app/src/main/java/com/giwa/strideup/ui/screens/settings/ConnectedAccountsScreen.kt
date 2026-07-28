package com.giwa.strideup.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/**
 * 연결된 계정 — GIWA 지갑 · Health Connect · 소셜 계정 연동 상태.
 *
 * 아직 연결 가능한 항목이 없으므로 행은 클릭되지 않는다.
 * 상태는 칩으로만 표시한다 (Coming soon / Not connected).
 */
@Composable
fun ConnectedAccountsScreen(onBack: () -> Unit = {}) {
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Eyebrow(text = stringResource(R.string.profile_account))
                    Text(
                        text = stringResource(R.string.settings_connected),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                }
            }
        }

        item {
            GlowCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    IconSquare(icon = Icons.Filled.Link, size = 38.dp)
                    Text(
                        text = stringResource(R.string.connected_body),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                }
            }
        }

        item {
            AccountRow(
                icon = Icons.Filled.AccountBalanceWallet,
                name = stringResource(R.string.connected_giwa),
                statusText = stringResource(R.string.connected_status_soon),
                statusColor = Volt,
                statusBackground = Volt.copy(alpha = 0.12f),
            )
        }

        item {
            AccountRow(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                name = stringResource(R.string.connected_health),
                statusText = stringResource(R.string.connected_status_soon),
                statusColor = Volt,
                statusBackground = Volt.copy(alpha = 0.12f),
            )
        }

        item {
            AccountRow(
                icon = Icons.Filled.Link,
                name = stringResource(R.string.connected_social),
                statusText = stringResource(R.string.connected_status_off),
                statusColor = Slate,
                statusBackground = CarbonHigh,
                iconTint = Slate,
            )
        }
    }
}

/** 계정 한 줄 — 아이콘 배지 + 이름 + 상태 칩. 아직 연결할 수 없어 클릭되지 않는다. */
@Composable
private fun AccountRow(
    icon: ImageVector,
    name: String,
    statusText: String,
    statusColor: Color,
    statusBackground: Color,
    iconTint: Color = Volt,
) {
    GlowCard(
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            IconSquare(icon = icon, size = 38.dp, tint = iconTint)
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            StatusChip(
                text = statusText,
                color = statusColor,
                background = statusBackground,
            )
        }
    }
}

/** 상태 pill 칩 — 배경 알약 위 소형 볼드 라벨. */
@Composable
private fun StatusChip(
    text: String,
    color: Color,
    background: Color,
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
        )
    }
}
