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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.BuildConfig
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.service.WalkSessionService
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.components.EnergyBar
import com.giwa.strideup.ui.components.ProgressRing
import com.giwa.strideup.ui.components.StatItem
import com.giwa.strideup.ui.components.StrideCard
import com.giwa.strideup.ui.theme.NeonAmber
import com.giwa.strideup.ui.theme.NeonCyan
import com.giwa.strideup.ui.theme.NeonGreen
import com.giwa.strideup.ui.theme.NeonRed

@Composable
fun WalkScreen(viewModel: WalkViewModel = viewModel(factory = WalkViewModel.Factory)) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val energy by viewModel.energy.collectAsStateWithLifecycle()
    val sneakerLevel by viewModel.sneakerLevel.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (StepPermissions.hasActivityRecognition(context)) {
            WalkSessionService.start(context)
        }
    }

    val estimate = RewardEconomy.sessionReward(session.steps, energy, sneakerLevel)
    val earnableSteps = (energy * RewardEconomy.STEPS_PER_ENERGY).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                "워킹 세션",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            StrideCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    ProgressRing(
                        progress = if (earnableSteps > 0) {
                            session.steps.toFloat() / earnableSteps
                        } else 0f,
                        modifier = Modifier.size(220.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "%,d".format(session.steps),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (session.isActive) {
                                    if (session.isPaused) "일시정지됨" else "걷는 중…"
                                } else "세션 대기 중",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(
                            icon = Icons.Filled.Timer,
                            tint = NeonCyan,
                            label = "시간",
                            value = formatDuration(session.elapsedSec),
                        )
                        StatItem(
                            icon = Icons.Filled.Straighten,
                            tint = NeonAmber,
                            label = "거리",
                            value = "%.2f km".format(
                                RewardEconomy.distanceMeters(session.steps) / 1000
                            ),
                        )
                        StatItem(
                            icon = Icons.Filled.Bolt,
                            tint = NeonGreen,
                            label = "예상 적립",
                            value = "+%.2f".format(estimate.points),
                        )
                    }
                }
            }
        }

        item {
            StrideCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("에너지", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "적립 가능 %,d보".format(earnableSteps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonGreen,
                        )
                    }
                    EnergyBar(current = energy, max = RewardEconomy.maxEnergy(sneakerLevel))
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
                            style = MaterialTheme.typography.headlineMedium,
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
                        TextButton(onClick = { viewModel.clearReward() }) {
                            Text("확인")
                        }
                    }
                }
            }
        }

        item {
            when {
                !session.isActive -> {
                    Button(
                        onClick = {
                            val missing = StepPermissions.missing(context)
                            if (missing.isEmpty()) {
                                WalkSessionService.start(context)
                            } else {
                                permissionLauncher.launch(missing)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("세션 시작", style = MaterialTheme.typography.titleMedium)
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (session.isPaused) {
                                    WalkSessionService.resume(context)
                                } else {
                                    WalkSessionService.pause(context)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                        ) {
                            Icon(
                                if (session.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = null,
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(if (session.isPaused) "재개" else "일시정지")
                        }
                        Button(
                            onClick = { WalkSessionService.stop(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonRed,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("종료")
                        }
                    }
                }
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

private fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
