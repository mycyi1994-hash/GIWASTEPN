package com.giwa.strideup.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.ui.components.DetailRow
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GoldButton
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.LineMeter
import com.giwa.strideup.ui.components.LuxeCard
import com.giwa.strideup.ui.components.MemberBadge
import com.giwa.strideup.ui.components.MetalText
import com.giwa.strideup.ui.components.ScreenTitle
import com.giwa.strideup.ui.components.StatColumn
import com.giwa.strideup.ui.theme.Ash
import com.giwa.strideup.ui.theme.AshDim
import com.giwa.strideup.ui.theme.Champagne
import com.giwa.strideup.ui.theme.Copper
import com.giwa.strideup.ui.theme.Ivory
import com.giwa.strideup.ui.theme.Platinum

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenTitle(eyebrow = "Profile", title = "프로필") }

        item { MemberCard(level = state.sneakerLevel, balance = state.balance) }

        item { SneakerCard(state = state, onUpgrade = viewModel::upgradeSneaker) }

        item { GoalCard(goal = state.goal, onGoalChange = viewModel::setGoal) }

        item {
            LuxeCard(spacing = 12.dp) {
                Eyebrow("About")
                DetailRow(label = "버전", value = "StrideUp 1.0.0")
                HairlineDivider()
                DetailRow(label = "네트워크", value = "GIWA Chain (예정)")
                HairlineDivider()
                Text(
                    text = "걷기 → SUP 포인트 적립 → GIWA 체인 온체인 전환. " +
                        "적립은 워킹 세션 중 걸음에 대해서만 이루어집니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AshDim,
                )
            }
        }
    }
}

/** 회원 헤더 — 아바타 + 등급 + 보유 SUP */
@Composable
private fun MemberCard(level: Int, balance: Double) {
    LuxeCard(accent = true, contentPadding = PaddingValues(22.dp), spacing = 18.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MemberBadge(level = level, size = 58.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = "StrideUp 러너",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ivory,
                )
                Eyebrow("GIWA STEPN · M2E MEMBER", color = Champagne.copy(alpha = 0.75f))
            }
        }
        HairlineDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow("보유 SUP", color = AshDim)
            MetalText(
                text = "%,.2f".format(balance),
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

/** 스니커즈 — 레벨, 능력치, 업그레이드 진행률 */
@Composable
private fun SneakerCard(state: ProfileViewModel.UiState, onUpgrade: () -> Unit) {
    val affordable = state.balance >= state.upgradeCost
    val progress = if (state.upgradeCost > 0) {
        (state.balance / state.upgradeCost).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

    LuxeCard(spacing = 18.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow("My sneakers")
            MetalText(
                text = "LEVEL ${state.sneakerLevel}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.6.sp,
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            StatColumn(
                icon = Icons.Filled.TrendingUp,
                tint = Champagne,
                label = "적립 배율",
                value = "×%.2f".format(state.multiplier),
            )
            StatColumn(
                icon = Icons.Filled.Bolt,
                tint = Platinum,
                label = "최대 에너지",
                value = "%.0f".format(state.maxEnergy),
            )
            StatColumn(
                icon = Icons.Filled.Whatshot,
                tint = Copper,
                label = "스트릭",
                value = "${state.streak}일",
            )
        }

        HairlineDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "다음 레벨까지",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ash,
                )
                Text(
                    text = "%,.0f / %,.0f SUP".format(
                        state.balance.coerceAtMost(state.upgradeCost),
                        state.upgradeCost,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (affordable) Champagne else AshDim,
                )
            }
            LineMeter(fraction = progress, height = 5.dp)
        }

        GoldButton(
            text = "Upgrade · %,.0f SUP".format(state.upgradeCost),
            onClick = onUpgrade,
            enabled = affordable,
        )

        Text(
            text = "레벨업 시 적립 배율 +0.15, 최대 에너지 +2가 적용됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = AshDim,
        )
    }
}

/** 일일 목표 설정 */
@Composable
private fun GoalCard(goal: Int, onGoalChange: (Int) -> Unit) {
    var sliderValue by remember(goal) { mutableFloatStateOf(goal.toFloat()) }
    val steps = sliderValue.toInt()
    val distanceKm = RewardEconomy.distanceMeters(steps) / 1000

    LuxeCard(spacing = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = Champagne,
                    modifier = Modifier.size(16.dp),
                )
                Eyebrow("Daily goal")
            }
            Text(
                text = "약 %.1f km".format(distanceKm),
                style = MaterialTheme.typography.bodySmall,
                color = AshDim,
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "%,d".format(steps),
                fontSize = 38.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1.5).sp,
                color = Ivory,
            )
            Text(
                text = " steps",
                style = MaterialTheme.typography.bodyMedium,
                color = AshDim,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onGoalChange(sliderValue.toInt()) },
            valueRange = UserPrefs.MIN_GOAL.toFloat()..UserPrefs.MAX_GOAL.toFloat(),
            steps = (UserPrefs.MAX_GOAL - UserPrefs.MIN_GOAL) / 500 - 1,
            colors = SliderDefaults.colors(
                thumbColor = Champagne,
                activeTrackColor = Champagne,
                inactiveTrackColor = Color.White.copy(alpha = 0.08f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )

        Text(
            text = "목표를 달성하면 보너스 SUP가 지급되고, 연속 달성일이 쌓일수록 보너스가 커집니다.",
            style = MaterialTheme.typography.bodySmall,
            color = AshDim,
        )
    }
}
