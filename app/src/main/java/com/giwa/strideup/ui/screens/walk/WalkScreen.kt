package com.giwa.strideup.ui.screens.walk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
import com.giwa.strideup.ui.components.EnergyBar
import com.giwa.strideup.ui.components.GlowCircleButton
import com.giwa.strideup.ui.components.ProgressRing
import com.giwa.strideup.ui.components.StartWalkButton
import com.giwa.strideup.ui.components.StrideCard
import com.giwa.strideup.ui.components.StrideUpWordmark
import com.giwa.strideup.ui.theme.NeonGreen
import com.giwa.strideup.ui.theme.NeonRed
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Surface2
import com.giwa.strideup.ui.theme.TextSecondary

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrideUpWordmark(fontSize = 24.sp)
                Text(
                    when {
                        !session.isActive -> "READY"
                        session.isPaused -> "PAUSED"
                        else -> "ACTIVE"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (session.isActive && !session.isPaused) NeonGreen else TextSecondary,
                    letterSpacing = 2.sp,
                )
            }
        }

        item {
            ProgressRing(
                progress = if (earnableSteps > 0) session.steps.toFloat() / earnableSteps else 0f,
                modifier = Modifier.size(260.dp),
                ringWidth = 18.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%.2f".format(distanceKm),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "KM",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonGreen,
                        letterSpacing = 4.sp,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = NeonGreen, modifier = Modifier.size(22.dp)) },
                    value = "%,d".format(session.steps),
                    label = "STEPS",
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Filled.LocalFireDepartment, null, tint = NeonGreen, modifier = Modifier.size(22.dp)) },
                    value = "%.0f".format(calories),
                    label = "KCAL",
                )
            }
        }

        item {
            StrideCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("에너지", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            "예상 +%.2f SUP".format(estimate.points),
                            style = MaterialTheme.typography.titleSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    EnergyBar(current = energy, max = RewardEconomy.maxEnergy(sneakerLevel))
                    Text(
                        "적립 가능 %,d보 · 걸을수록 SUP가 쌓여요".format(earnableSteps),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        session.lastRewardPoints?.let { points ->
            item {
                StrideCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("세션 완료! 🎉", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "+%.2f SUP".format(points),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonGreen,
                        )
                        Text(
                            "총 %,d보 중 %,d보 적립 인정".format(
                                session.lastSessionSteps,
                                session.lastRewardedSteps,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { viewModel.clearReward() }) { Text("확인") }
                    }
                }
            }
        }

        item {
            if (!session.isActive) {
                StartWalkButton(
                    text = "START WALK",
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
                    // 잠금 토글
                    GlowCircleButton(
                        onClick = { locked = !locked },
                        size = 56.dp,
                        containerColor = if (locked) NeonGreen else Surface2,
                        glow = locked,
                    ) {
                        Icon(
                            if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = "화면 잠금",
                            tint = if (locked) Night else TextSecondary,
                        )
                    }
                    // 일시정지 / 재개 (메인)
                    GlowCircleButton(
                        onClick = {
                            if (session.isPaused) WalkSessionService.resume(context)
                            else WalkSessionService.pause(context)
                        },
                        size = 84.dp,
                        containerColor = NeonGreen,
                        enabled = !locked,
                    ) {
                        Icon(
                            if (session.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (session.isPaused) "재개" else "일시정지",
                            tint = Night,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    // 종료
                    GlowCircleButton(
                        onClick = { WalkSessionService.stop(context) },
                        size = 56.dp,
                        containerColor = NeonRed,
                        glow = false,
                        enabled = !locked,
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "종료", tint = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }

        if (session.isActive && locked) {
            item {
                Text(
                    "🔒 화면이 잠겼어요 · 자물쇠를 눌러 해제",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        if (BuildConfig.DEBUG) {
            item {
                TextButton(onClick = { viewModel.simulateSteps(100) }) {
                    Text(
                        "+100 걸음 시뮬레이션 (디버그)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    value: String,
    label: String,
) {
    StrideCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
            )
        }
    }
}
