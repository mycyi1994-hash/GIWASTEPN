package com.giwa.strideup.ui.screens.walk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.BuildConfig
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.service.WalkSessionService
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.components.CircleControl
import com.giwa.strideup.ui.components.EnergyMeter
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GoldButton
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.LuxeCard
import com.giwa.strideup.ui.components.MetalRing
import com.giwa.strideup.ui.components.MetalText
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.breathing
import com.giwa.strideup.ui.theme.Ash
import com.giwa.strideup.ui.theme.AshDim
import com.giwa.strideup.ui.theme.Champagne
import com.giwa.strideup.ui.theme.Copper
import com.giwa.strideup.ui.theme.Crimson
import com.giwa.strideup.ui.theme.Ivory
import com.giwa.strideup.ui.theme.ObsidianDeep
import com.giwa.strideup.ui.theme.Platinum

@Composable
fun WalkScreen(viewModel: WalkViewModel = viewModel(factory = WalkViewModel.Factory)) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val energy by viewModel.energy.collectAsStateWithLifecycle()
    val sneakerLevel by viewModel.sneakerLevel.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 화면 잠금(오조작 방지). 잠금 상태에서는 일시정지/종료가 비활성.
    var locked by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (StepPermissions.hasActivityRecognition(context)) {
            WalkSessionService.start(context)
        }
    }

    val estimate = RewardEconomy.sessionReward(session.steps, energy, sneakerLevel)
    val earnableSteps = (energy * RewardEconomy.STEPS_PER_ENERGY).toInt()
    val distanceKm = RewardEconomy.distanceMeters(session.steps) / 1000
    val calories = RewardEconomy.calories(session.steps)
    val running = session.isActive && !session.isPaused

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(fontSize = 19.sp)
                StatusChip(isActive = session.isActive, isPaused = session.isPaused)
            }
        }

        item {
            MetalRing(
                progress = if (earnableSteps > 0) session.steps.toFloat() / earnableSteps else 0f,
                modifier = Modifier.size(256.dp),
                ringWidth = 12.dp,
                glowAlpha = if (running) 0.16f else 0.08f,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "%.2f".format(distanceKm),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-3).sp,
                        color = Ivory,
                    )
                    MetalText(
                        text = "KILOMETERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 4.sp,
                    )
                }
            }
        }

        item {
            LuxeCard(contentPadding = PaddingValues(vertical = 20.dp, horizontal = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Metric(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        tint = Champagne,
                        value = "%,d".format(session.steps),
                        label = "걸음",
                    )
                    Metric(
                        icon = Icons.Filled.Schedule,
                        tint = Platinum,
                        value = formatDuration(session.elapsedSec),
                        label = "시간",
                    )
                    Metric(
                        icon = Icons.Filled.LocalFireDepartment,
                        tint = Copper,
                        value = "%,.0f".format(calories),
                        label = "칼로리",
                    )
                }
            }
        }

        item {
            LuxeCard(spacing = 13.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Champagne,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Eyebrow("Energy")
                    }
                    MetalText(
                        text = "+%.2f SUP".format(estimate.points),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                EnergyMeter(current = energy, max = RewardEconomy.maxEnergy(sneakerLevel))
                Text(
                    text = "적립 가능 %,d보 · 걸을수록 SUP가 쌓입니다".format(earnableSteps),
                    style = MaterialTheme.typography.bodySmall,
                    color = AshDim,
                )
            }
        }

        session.lastRewardPoints?.let { points ->
            item {
                SettlementCard(
                    points = points,
                    sessionSteps = session.lastSessionSteps,
                    rewardedSteps = session.lastRewardedSteps,
                    onConfirm = viewModel::clearReward,
                )
            }
        }

        item {
            if (!session.isActive) {
                GoldButton(
                    text = "Start Walk",
                    onClick = {
                        val missing = StepPermissions.missing(context)
                        if (missing.isEmpty()) {
                            WalkSessionService.start(context)
                        } else {
                            permissionLauncher.launch(missing)
                        }
                    },
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleControl(
                        onClick = { locked = !locked },
                        size = 52.dp,
                        filled = false,
                        accent = if (locked) Champagne else Ash,
                    ) {
                        Icon(
                            imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = "화면 잠금",
                            tint = if (locked) Champagne else Ash,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    CircleControl(
                        onClick = {
                            if (session.isPaused) WalkSessionService.resume(context)
                            else WalkSessionService.pause(context)
                        },
                        size = 82.dp,
                        halo = running,
                        enabled = !locked,
                    ) {
                        Icon(
                            imageVector = if (session.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (session.isPaused) "재개" else "일시정지",
                            tint = ObsidianDeep,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    CircleControl(
                        onClick = { WalkSessionService.stop(context) },
                        size = 52.dp,
                        filled = false,
                        accent = Crimson,
                        enabled = !locked,
                    ) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = "종료",
                            tint = Crimson,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        if (session.isActive && locked) {
            item {
                Text(
                    text = "화면이 잠겼습니다 · 자물쇠를 눌러 해제",
                    style = MaterialTheme.typography.bodySmall,
                    color = AshDim,
                )
            }
        }

        if (BuildConfig.DEBUG) {
            item {
                TextButton(onClick = { viewModel.simulateSteps(100) }) {
                    Text(
                        text = "+100 걸음 시뮬레이션 (디버그)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AshDim,
                    )
                }
            }
        }
    }
}

/** READY / ACTIVE / PAUSED — 활성 시 금빛 점이 천천히 호흡한다. */
@Composable
private fun StatusChip(isActive: Boolean, isPaused: Boolean) {
    val running = isActive && !isPaused
    val pulse = breathing()
    val label = when {
        !isActive -> "READY"
        isPaused -> "PAUSED"
        else -> "ACTIVE"
    }
    val accent = if (running) Champagne else AshDim
    Row(
        modifier = Modifier
            .background(accent.copy(alpha = 0.07f), RoundedCornerShape(50))
            .border(1.dp, accent.copy(alpha = 0.26f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .alpha(if (running) 0.45f + pulse * 0.55f else 0.6f)
                .background(accent, CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun RowScope.Metric(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.5).sp,
            color = Ivory,
        )
        Eyebrow(label, color = AshDim)
    }
}

/** 세션 정산 — 금장 인증서처럼 마감한다. */
@Composable
private fun SettlementCard(
    points: Double,
    sessionSteps: Int,
    rewardedSteps: Int,
    onConfirm: () -> Unit,
) {
    LuxeCard(accent = true, contentPadding = PaddingValues(24.dp), spacing = 12.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Eyebrow("Session complete", color = Champagne)
            Row(verticalAlignment = Alignment.Bottom) {
                MetalText(
                    text = "+%.2f".format(points),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "SUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = Champagne.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            HairlineDivider()
            Text(
                text = "총 %,d보 중 %,d보 적립 인정".format(sessionSteps, rewardedSteps),
                style = MaterialTheme.typography.bodySmall,
                color = Ash,
            )
        }
        GhostButton(text = "확인", onClick = onConfirm)
    }
}

private fun formatDuration(totalSec: Long): String {
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
