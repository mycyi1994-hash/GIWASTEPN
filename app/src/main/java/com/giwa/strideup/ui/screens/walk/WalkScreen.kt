package com.giwa.strideup.ui.screens.walk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.giwa.strideup.ui.components.ChipButton
import com.giwa.strideup.ui.components.CircleControl
import com.giwa.strideup.ui.components.EnergyMeter
import com.giwa.strideup.ui.components.GradientText
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.IconMedallion
import com.giwa.strideup.ui.components.SoftCard
import com.giwa.strideup.ui.components.SunsetButton
import com.giwa.strideup.ui.components.SunsetRing
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.breathing
import com.giwa.strideup.ui.theme.Coral
import com.giwa.strideup.ui.theme.Honey
import com.giwa.strideup.ui.theme.HoneyInk
import com.giwa.strideup.ui.theme.Ink
import com.giwa.strideup.ui.theme.Rose
import com.giwa.strideup.ui.theme.Sage
import com.giwa.strideup.ui.theme.Sand
import com.giwa.strideup.ui.theme.Taupe
import com.giwa.strideup.ui.theme.TaupeLight

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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
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
                Wordmark(fontSize = 20.sp)
                StatusChip(isActive = session.isActive, isPaused = session.isPaused)
            }
        }

        item {
            SunsetRing(
                progress = if (earnableSteps > 0) session.steps.toFloat() / earnableSteps else 0f,
                modifier = Modifier.size(256.dp),
                ringWidth = 15.dp,
                glowAlpha = if (running) 0.18f else 0.10f,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "%.2f".format(distanceKm),
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2.5).sp,
                        color = Ink,
                    )
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.titleMedium,
                        color = Coral,
                    )
                }
            }
        }

        item {
            SoftCard(contentPadding = PaddingValues(vertical = 20.dp, horizontal = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Metric(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        tint = Coral,
                        value = "%,d".format(session.steps),
                        label = "걸음",
                    )
                    Metric(
                        icon = Icons.Filled.Schedule,
                        tint = Sage,
                        value = formatDuration(session.elapsedSec),
                        label = "시간",
                    )
                    Metric(
                        icon = Icons.Filled.LocalFireDepartment,
                        tint = Honey,
                        value = "%,.0f".format(calories),
                        label = "칼로리",
                    )
                }
            }
        }

        item {
            SoftCard(spacing = 13.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Honey,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("에너지", style = MaterialTheme.typography.titleSmall, color = Ink)
                    }
                    GradientText(
                        text = "+%.2f SUP".format(estimate.points),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        brush = HoneyInk,
                    )
                }
                EnergyMeter(current = energy, max = RewardEconomy.maxEnergy(sneakerLevel))
                Text(
                    text = "지금 %,d걸음까지 적립할 수 있어요".format(earnableSteps),
                    style = MaterialTheme.typography.bodySmall,
                    color = TaupeLight,
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
                SunsetButton(
                    text = "산책 시작하기",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleControl(
                        onClick = { locked = !locked },
                        size = 54.dp,
                        filled = false,
                        accent = if (locked) Coral else Taupe,
                    ) {
                        Icon(
                            imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = "화면 잠금",
                            tint = if (locked) Coral else Taupe,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    CircleControl(
                        onClick = {
                            if (session.isPaused) WalkSessionService.resume(context)
                            else WalkSessionService.pause(context)
                        },
                        size = 84.dp,
                        enabled = !locked,
                    ) {
                        Icon(
                            imageVector = if (session.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (session.isPaused) "재개" else "일시정지",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    CircleControl(
                        onClick = { WalkSessionService.stop(context) },
                        size = 54.dp,
                        filled = false,
                        accent = Rose,
                        enabled = !locked,
                    ) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = "종료",
                            tint = Rose,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        if (session.isActive && locked) {
            item {
                Text(
                    text = "화면이 잠겼어요 · 자물쇠를 눌러 해제해 주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaupeLight,
                )
            }
        }

        if (BuildConfig.DEBUG) {
            item {
                TextButton(onClick = { viewModel.simulateSteps(100) }) {
                    Text(
                        text = "+100 걸음 시뮬레이션 (디버그)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaupeLight,
                    )
                }
            }
        }
    }
}

/** 준비 / 걷는 중 / 일시정지 — 걷는 중엔 코럴 점이 천천히 호흡한다. */
@Composable
private fun StatusChip(isActive: Boolean, isPaused: Boolean) {
    val running = isActive && !isPaused
    val pulse = breathing()
    val label = when {
        !isActive -> "준비 완료"
        isPaused -> "일시정지"
        else -> "걷는 중"
    }
    val accent = if (running) Coral else Taupe
    Row(
        modifier = Modifier
            .background(
                if (running) Coral.copy(alpha = 0.10f) else Sand,
                RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(if (running) 0.45f + pulse * 0.55f else 0.7f)
                .background(accent, CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
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
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        IconMedallion(icon = icon, tint = tint, size = 32.dp)
        Text(
            text = value,
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp,
            color = Ink,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Taupe,
        )
    }
}

/** 세션 정산 — 오늘의 보상을 축하하는 카드 */
@Composable
private fun SettlementCard(
    points: Double,
    sessionSteps: Int,
    rewardedSteps: Int,
    onConfirm: () -> Unit,
) {
    SoftCard(accent = true, contentPadding = PaddingValues(24.dp), spacing = 12.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("산책 완료! 🎉", style = MaterialTheme.typography.titleMedium, color = Ink)
            Row(verticalAlignment = Alignment.Bottom) {
                GradientText(
                    text = "+%.2f".format(points),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp,
                    brush = HoneyInk,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "SUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = Honey,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            HairlineDivider()
            Text(
                text = "총 %,d걸음 중 %,d걸음이 적립됐어요".format(sessionSteps, rewardedSteps),
                style = MaterialTheme.typography.bodySmall,
                color = Taupe,
            )
        }
        ChipButton(text = "확인", onClick = onConfirm)
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
