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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.BuildConfig
import com.giwa.strideup.R
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.service.WalkSessionService
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.components.CircleControl
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.EnergyMeter
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.components.NeonRing
import com.giwa.strideup.ui.components.StartRunButton
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.breathing
import com.giwa.strideup.ui.theme.Alert
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

@Composable
fun RunScreen(
    onBack: () -> Unit = {},
    viewModel: WalkViewModel = viewModel(factory = WalkViewModel.Factory),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val energy by viewModel.energy.collectAsStateWithLifecycle()
    val sneakerLevel by viewModel.sneakerLevel.collectAsStateWithLifecycle()
    val equipped by viewModel.equipped.collectAsStateWithLifecycle()
    val xpBoosted by viewModel.xpBoosted.collectAsStateWithLifecycle()
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

    // 착용 스니커즈 · 파티 인원 · XP 부스터를 모두 반영한 예상 적립
    val earningMultiplier = equipped?.earningMultiplier
        ?: RewardEconomy.sneakerMultiplier(sneakerLevel)
    val energyEfficiency = equipped?.energyEfficiency ?: 1.0
    val estimate = RewardEconomy.sessionReward(
        walkedSteps = session.steps,
        energyRemaining = energy,
        earningMultiplier = earningMultiplier,
        energyEfficiency = energyEfficiency,
        partyMultiplier = RewardEconomy.partyMultiplier(session.partySize),
        boostMultiplier = if (xpBoosted) RewardEconomy.XP_BOOST_MULTIPLIER else 1.0,
    )
    val earnableSteps = RewardEconomy.earnableSteps(energy, energyEfficiency)
    val maxEnergy = RewardEconomy.maxEnergy(equipped?.level ?: sneakerLevel)
    val distanceKm = RewardEconomy.distanceMeters(session.steps) / 1000
    val calories = RewardEconomy.calories(session.steps)
    val running = session.isActive && !session.isPaused

    // 러닝 지표 — 아직 값이 없으면 대시로 둔다
    val pace = if (distanceKm >= 0.01 && session.elapsedSec > 0) {
        val secPerKm = (session.elapsedSec / distanceKm).toLong()
        "%d'%02d\"".format(secPerKm / 60, secPerKm % 60)
    } else {
        "—"
    }
    val speedKmh = if (session.elapsedSec > 0) {
        "%.1f".format(distanceKm / (session.elapsedSec / 3600.0))
    } else {
        "—"
    }
    val cadence = if (session.elapsedSec > 0) {
        "%d".format(session.steps * 60L / session.elapsedSec)
    } else {
        "—"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
                StatusChip(isActive = session.isActive, isPaused = session.isPaused)
            }
        }

        item {
            NeonRing(
                progress = if (earnableSteps > 0) session.steps.toFloat() / earnableSteps else 0f,
                modifier = Modifier.size(254.dp),
                ringWidth = 14.dp,
                glowAlpha = if (running) 0.22f else 0.12f,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "%.2f".format(distanceKm),
                        fontSize = 58.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-2).sp,
                        color = Snow,
                    )
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.titleMedium,
                        color = Volt,
                    )
                }
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(vertical = 18.dp, horizontal = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Metric(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        value = "%,d".format(session.steps),
                        label = stringResource(R.string.stat_steps),
                    )
                    Metric(
                        icon = Icons.Filled.Schedule,
                        value = formatDuration(session.elapsedSec),
                        label = stringResource(R.string.stat_time),
                    )
                    Metric(
                        icon = Icons.Filled.LocalFireDepartment,
                        value = "%,.0f".format(calories),
                        label = stringResource(R.string.stat_calories),
                    )
                }
                HairlineDivider(Modifier.padding(horizontal = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Metric(
                        icon = Icons.Filled.Timer,
                        value = pace,
                        label = stringResource(R.string.stat_pace),
                    )
                    Metric(
                        icon = Icons.Filled.Speed,
                        value = speedKmh,
                        label = stringResource(R.string.stat_speed),
                    )
                    Metric(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        value = cadence,
                        label = stringResource(R.string.stat_cadence),
                    )
                }
            }
        }

        item {
            GlowCard(spacing = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Volt,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_energy),
                            style = MaterialTheme.typography.titleSmall,
                            color = Snow,
                        )
                    }
                    Text(
                        text = "+%.2f SUP".format(estimate.points),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                }
                EnergyMeter(current = energy, max = maxEnergy)
                Text(
                    text = stringResource(R.string.run_earnable, "%,d".format(earnableSteps)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
                if (xpBoosted) {
                    Text(
                        text = stringResource(R.string.run_boost_active),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                }
                if (session.partySize > 1) {
                    Text(
                        text = stringResource(R.string.crew_boost, RewardEconomy.partyBonusPercent(session.partySize)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                }
            }
        }

        session.lastRewardPoints?.let { points ->
            item {
                GlowCard(accent = true, contentPadding = PaddingValues(22.dp), spacing = 11.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.run_complete),
                            style = MaterialTheme.typography.titleMedium,
                            color = Snow,
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "+%.2f".format(points),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1.5).sp,
                                color = Volt,
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = "SUP",
                                style = MaterialTheme.typography.labelMedium,
                                color = Volt.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        HairlineDivider()
                        Text(
                            text = stringResource(
                                R.string.run_rewarded,
                                "%,d".format(session.lastRewardedSteps),
                                "%,d".format(session.lastSessionSteps),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Silver,
                        )
                        if (session.lastPartySize > 1) {
                            Text(
                                text = stringResource(
                                    R.string.crew_boost,
                                    RewardEconomy.partyBonusPercent(session.lastPartySize),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Volt,
                            )
                        }
                    }
                    GhostButton(
                        text = stringResource(R.string.common_ok),
                        onClick = viewModel::clearReward,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            if (!session.isActive) {
                StartRunButton(
                    title = stringResource(R.string.start_run),
                    subtitle = stringResource(R.string.start_run_sub),
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
                        accent = if (locked) Volt else Silver,
                    ) {
                        Icon(
                            imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = stringResource(R.string.cd_lock),
                            tint = if (locked) Volt else Silver,
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
                            contentDescription = if (session.isPaused) {
                                stringResource(R.string.cd_resume)
                            } else {
                                stringResource(R.string.cd_pause)
                            },
                            tint = Night,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    CircleControl(
                        onClick = { WalkSessionService.stop(context) },
                        size = 54.dp,
                        filled = false,
                        accent = Alert,
                        enabled = !locked,
                    ) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = stringResource(R.string.cd_stop),
                            tint = Alert,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        if (session.isActive && locked) {
            item {
                Text(
                    text = stringResource(R.string.run_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
        }

        if (BuildConfig.DEBUG) {
            item {
                TextButton(onClick = { viewModel.simulateSteps(100) }) {
                    Text(
                        text = stringResource(R.string.run_simulate),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate,
                    )
                }
            }
        }
    }
}

/** 준비 / 러닝 중 / 일시정지 — 러닝 중엔 볼트 점이 호흡한다. */
@Composable
private fun StatusChip(isActive: Boolean, isPaused: Boolean) {
    val running = isActive && !isPaused
    val pulse = breathing()
    val label = when {
        !isActive -> stringResource(R.string.run_ready)
        isPaused -> stringResource(R.string.run_paused)
        else -> stringResource(R.string.run_active)
    }
    val accent = if (running) Volt else Silver
    Row(
        modifier = Modifier
            .background(
                if (running) Volt.copy(alpha = 0.10f) else CarbonHigh,
                RoundedCornerShape(50),
            )
            .border(
                1.dp,
                if (running) Volt.copy(alpha = 0.35f) else Color.Transparent,
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
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun RowScope.Metric(
    icon: ImageVector,
    value: String,
    label: String,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        IconSquare(icon = icon, size = 32.dp)
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            color = Snow,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Slate,
        )
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
