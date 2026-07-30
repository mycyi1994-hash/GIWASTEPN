package com.giwa.strideup.ui.screens.walk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.BuildConfig
import com.giwa.strideup.R
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.service.WalkSessionService
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.EnergyMeter
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.NeonRing
import com.giwa.strideup.ui.components.RouteMap
import com.giwa.strideup.ui.components.StartRunButton
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.breathing
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.Alert
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/** 랩 스냅샷(누적)을 구간값으로 변환한 것 */
private data class LapSegment(
    val index: Int,
    val km: Double,
    val sec: Long,
    val paceSec: Long,
)

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
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 목표 거리(km) — 다이얼로그 스테퍼로 0.5 단위 조절, 회전에도 유지
    var goalKm by rememberSaveable { mutableStateOf(5.0) }
    var showGoalDialog by remember { mutableStateOf(false) }

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

    // 데모 고도 — 걸음 수 기반 결정값 (센서 없이도 항상 같은 값)
    val elevationM = (session.steps * 0.011).toInt()

    val avgPaceSec: Long? = if (distanceKm >= 0.01 && session.elapsedSec > 0) {
        (session.elapsedSec / distanceKm).toLong()
    } else {
        null
    }
    // 현재 구간(마지막 랩 이후) 페이스 — 구간 데이터가 모자라면 평균으로 대체
    val lastLap = laps.lastOrNull()
    val segKm = distanceKm - (lastLap?.km ?: 0.0)
    val segSec = session.elapsedSec - (lastLap?.splitSec ?: 0L)
    val segPaceSec: Long = if (segKm >= 0.01 && segSec > 0) (segSec / segKm).toLong() else 0L
    val curPaceSec: Long? = if (segPaceSec > 0) segPaceSec else avgPaceSec

    val cadenceVal = if (session.elapsedSec > 0) (session.steps * 60L / session.elapsedSec).toInt() else 0
    val speedVal = if (session.elapsedSec > 0) distanceKm / (session.elapsedSec / 3600.0) else 0.0
    // 데모 심박 — 케이던스 기반 결정값
    val hr: Int? = if (running && session.elapsedSec > 0) {
        96 + (cadenceVal.coerceAtMost(190) * 0.32).toInt()
    } else {
        null
    }

    val segments = lapSegments(laps)

    // 1km 경계를 넘어설 때마다 자동 랩 — 수동 랩과 합쳐 랩 수가 이미 앞서 있으면 건너뛴다
    LaunchedEffect(distanceKm) {
        if (session.isActive && distanceKm >= 1.0 && floor(distanceKm) > laps.size) {
            viewModel.recordLap(distanceKm, session.elapsedSec)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (session.isActive) {
                        StatusChip(isActive = session.isActive, isPaused = session.isPaused)
                    } else {
                        Wordmark(fontSize = 20.sp)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HexEmblem(size = 20.dp)
                    Text(
                        text = "%,.2f".format(balance),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Snow,
                    )
                }
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                ) {
                    RouteMap(Modifier.fillMaxSize())
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Night.copy(alpha = 0.60f), RoundedCornerShape(50))
                            .border(1.dp, Edge, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = Volt,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "GPS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Snow,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(34.dp)
                            .background(Night.copy(alpha = 0.60f), CircleShape)
                            .border(1.dp, Edge, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "N",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Volt,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.run_course_name),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Snow,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "%.1f km".format(goalKm),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Volt,
                            )
                            Row(
                                modifier = Modifier.quietClickable {},
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.run_course_info),
                                    fontSize = 11.sp,
                                    color = Silver,
                                )
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = Silver,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.run_elevation),
                            fontSize = 9.sp,
                            color = Slate,
                        )
                        Text(
                            text = "%d m".format(elevationM),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Snow,
                        )
                        ElevationSparkline(
                            modifier = Modifier
                                .width(110.dp)
                                .height(30.dp),
                        )
                    }
                }
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(vertical = 20.dp, horizontal = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.stat_distance),
                            fontSize = 10.sp,
                            color = Slate,
                        )
                        Text(
                            text = "%.2f".format(distanceKm),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = Snow,
                            maxLines = 1,
                        )
                        Text(
                            text = "km",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Volt,
                        )
                    }
                    NeonRing(
                        progress = if (goalKm > 0) (distanceKm / goalKm).toFloat() else 0f,
                        modifier = Modifier.size(150.dp),
                        ringWidth = 9.dp,
                        glowAlpha = if (running) 0.20f else 0.12f,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = Volt,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = stringResource(R.string.run_total_time),
                                fontSize = 9.sp,
                                color = Slate,
                            )
                            Text(
                                text = formatDuration(session.elapsedSec),
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                color = Snow,
                            )
                            Row(
                                modifier = Modifier.quietClickable { showGoalDialog = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.run_goal_label, "%.2f".format(goalKm)),
                                    fontSize = 10.sp,
                                    color = Silver,
                                )
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = Volt,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.run_remaining),
                            fontSize = 10.sp,
                            color = Slate,
                        )
                        Text(
                            text = "%.2f".format(max(goalKm - distanceKm, 0.0)),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = Snow,
                            maxLines = 1,
                        )
                        Text(
                            text = "km",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Volt,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.run_eta),
                            fontSize = 10.sp,
                            color = Slate,
                        )
                        Text(
                            text = if (distanceKm >= 0.05 && session.elapsedSec > 0) {
                                formatDuration((session.elapsedSec / distanceKm * goalKm).toLong())
                            } else {
                                "—"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Volt,
                        )
                    }
                }
            }
        }

        item {
            val hrZone = hr?.let {
                when {
                    it < 120 -> stringResource(R.string.hr_zone_recovery) to Silver
                    it < 150 -> stringResource(R.string.hr_zone_aerobic) to Volt
                    else -> stringResource(R.string.hr_zone_anaerobic) to Alert
                }
            }
            val lastSplit = segments.lastOrNull()?.paceSec?.takeIf { it > 0 }
            GlowCard(spacing = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricCell(
                        icon = Icons.Filled.Timer,
                        label = stringResource(R.string.stat_pace),
                        value = curPaceSec?.let { formatPace(it) } ?: "—",
                        fraction = paceFraction(curPaceSec),
                    )
                    MetricCell(
                        icon = Icons.Filled.Flag,
                        label = stringResource(R.string.run_lap_split),
                        value = lastSplit?.let { formatPace(it) } ?: "—",
                        fraction = paceFraction(lastSplit),
                    )
                    MetricCell(
                        icon = Icons.Filled.Favorite,
                        label = stringResource(R.string.stat_hr),
                        value = hr?.toString() ?: "—",
                        unit = "bpm",
                        fraction = (hr ?: 0) / 190f,
                        chip = hrZone?.first,
                        chipColor = hrZone?.second ?: Volt,
                    )
                }
                HairlineDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricCell(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        label = stringResource(R.string.stat_cadence),
                        value = if (session.elapsedSec > 0) "%d".format(cadenceVal) else "—",
                        unit = "spm",
                        fraction = cadenceVal / 200f,
                    )
                    MetricCell(
                        icon = Icons.Filled.LocalFireDepartment,
                        label = stringResource(R.string.stat_calories),
                        value = "%,.0f".format(calories),
                        unit = "kcal",
                        fraction = (calories / 600.0).toFloat(),
                    )
                    MetricCell(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        label = stringResource(R.string.stat_steps),
                        value = "%,d".format(session.steps),
                        fraction = session.steps / 10_000f,
                    )
                }
                HairlineDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricCell(
                        icon = Icons.Filled.Speed,
                        label = stringResource(R.string.stat_speed),
                        value = if (session.elapsedSec > 0) "%.1f".format(speedVal) else "—",
                        unit = "km/h",
                        fraction = (speedVal / 15.0).toFloat(),
                    )
                    MetricCell(
                        icon = Icons.Filled.Terrain,
                        label = stringResource(R.string.run_elevation),
                        value = "%d".format(elevationM),
                        unit = "m",
                        fraction = elevationM / 250f,
                    )
                    MetricCell(
                        icon = Icons.Filled.Schedule,
                        label = stringResource(R.string.run_total_time),
                        value = formatDuration(session.elapsedSec),
                        fraction = session.elapsedSec / 3_600f,
                    )
                }
            }
        }

        if (laps.isNotEmpty() || session.isActive) {
            item {
                GlowCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.run_lap),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Snow,
                            )
                            // 최근 5개 랩만 — 마지막 완료 랩을 볼트로 강조
                            segments.takeLast(5).forEach { seg ->
                                LapRow(
                                    index = seg.index,
                                    km = seg.km,
                                    paceSec = seg.paceSec,
                                    bpm = demoLapBpm(seg.paceSec),
                                    highlight = seg.index == segments.size,
                                )
                            }
                            // 진행 중인 부분 랩
                            if (session.isActive && segKm > 0.01) {
                                LapRow(
                                    index = laps.size + 1,
                                    km = segKm,
                                    paceSec = segPaceSec,
                                    bpm = hr ?: 0,
                                    highlight = false,
                                    dimmed = true,
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(CarbonHigh, RoundedCornerShape(50))
                                    .border(1.dp, Edge, RoundedCornerShape(50))
                                    .padding(horizontal = 9.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.run_pace_chart),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Silver,
                                )
                            }
                            PaceChart(
                                paces = segments.map { it.paceSec },
                                modifier = Modifier
                                    .width(132.dp)
                                    .height(104.dp),
                            )
                        }
                    }
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
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GhostButton(
                        text = if (session.isPaused) {
                            stringResource(R.string.cd_resume)
                        } else {
                            stringResource(R.string.cd_pause)
                        },
                        onClick = {
                            if (session.isPaused) WalkSessionService.resume(context)
                            else WalkSessionService.pause(context)
                        },
                        modifier = Modifier.weight(1f),
                        accent = if (session.isPaused) Volt else Silver,
                    )
                    LapButton(
                        text = stringResource(R.string.run_lap),
                        onClick = { viewModel.recordLap(distanceKm, session.elapsedSec) },
                        modifier = Modifier.weight(1f),
                        // 직전 랩에서 최소 50m는 나아가야 새 랩을 찍을 수 있다
                        enabled = running && distanceKm > (laps.lastOrNull()?.km ?: 0.0) + 0.05,
                    )
                    VoltButton(
                        text = stringResource(R.string.run_finish),
                        onClick = { WalkSessionService.stop(context) },
                        modifier = Modifier.weight(1.15f),
                    )
                }
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

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            containerColor = Carbon,
            titleContentColor = Snow,
            textContentColor = Silver,
            confirmButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text(
                        text = stringResource(R.string.common_ok),
                        color = Volt,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.run_goal_dialog_title),
                    fontWeight = FontWeight.Black,
                )
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StepperButton(text = "−") {
                        goalKm = (goalKm - 0.5).coerceIn(1.0, 42.2)
                    }
                    Text(
                        text = "%.1f km".format(goalKm),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Snow,
                    )
                    StepperButton(text = "+") {
                        goalKm = (goalKm + 0.5).coerceIn(1.0, 42.2)
                    }
                }
            },
        )
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
        else -> stringResource(R.string.run_live)
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

/** 지표 그리드 셀 — 아이콘+라벨 / 큰 값+단위 / (선택) 존 칩 / 얇은 게이지 */
@Composable
private fun RowScope.MetricCell(
    icon: ImageVector,
    label: String,
    value: String,
    fraction: Float,
    unit: String? = null,
    chip: String? = null,
    chipColor: Color = Volt,
) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = Slate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = Snow,
                maxLines = 1,
            )
            if (unit != null) {
                Text(
                    text = unit,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Volt,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        if (chip != null) {
            Box(
                modifier = Modifier
                    .border(1.dp, chipColor.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = chip,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = chipColor,
                )
            }
        }
        BarMeter(fraction = fraction, height = 3.dp)
    }
}

/** 랩 한 줄 — [번호] [구간 km] [스플릿 페이스] [데모 bpm] */
@Composable
private fun LapRow(
    index: Int,
    km: Double,
    paceSec: Long,
    bpm: Int,
    highlight: Boolean,
    dimmed: Boolean = false,
) {
    val main = when {
        highlight -> Volt
        dimmed -> Slate
        else -> Silver
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%d".format(index),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Volt else Slate,
            modifier = Modifier.width(18.dp),
        )
        Text(
            text = "%.2f".format(km),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = main,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (paceSec > 0) formatPace(paceSec) else "—",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = main,
            maxLines = 1,
            modifier = Modifier.weight(1.2f),
        )
        Text(
            text = if (bpm > 0) "%d bpm".format(bpm) else "—",
            fontSize = 10.sp,
            color = if (highlight) Volt else Slate,
            maxLines = 1,
        )
    }
}

/** 랩 스플릿 페이스 폴리라인 — 빠를수록 위쪽. 데이터 2개 미만이면 기준선만 그린다. */
@Composable
private fun PaceChart(paces: List<Long>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val valid = paces.filter { it > 0 }
        if (valid.size < 2) {
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = Offset(0f, size.height * 0.55f),
                end = Offset(size.width, size.height * 0.55f),
                strokeWidth = 2f,
            )
            return@Canvas
        }
        val minP = valid.min().toFloat()
        val maxP = valid.max().toFloat()
        val span = (maxP - minP).coerceAtLeast(1f)
        val inset = 5f
        fun pointAt(i: Int): Offset {
            val x = inset + (size.width - inset * 2) * i / (valid.size - 1)
            // 초/km가 작을수록(빠를수록) 위로
            val y = size.height * (0.12f + 0.72f * ((valid[i] - minP) / span))
            return Offset(x, y)
        }
        val line = Path()
        for (i in valid.indices) {
            val p = pointAt(i)
            if (i == 0) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(pointAt(valid.size - 1).x, size.height)
            lineTo(inset, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(Volt.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(line, Volt, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Volt, radius = 3.5f, center = pointAt(valid.size - 1))
    }
}

/** 데모 고도 스파크라인 — sin 조합의 결정적 곡선 */
@Composable
private fun ElevationSparkline(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val steps = 22
        val line = Path()
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = size.width * t
            val y = size.height * (0.55f - 0.22f * sin(t * 5.4f + 0.7f) - 0.12f * sin(t * 11.3f))
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(Volt.copy(alpha = 0.30f), Color.Transparent)))
        drawPath(line, Volt, style = Stroke(width = 2f, cap = StrokeCap.Round))
    }
}

/** GhostButton 톤의 랩 기록 버튼 — 깃발 아이콘 포함 */
@Composable
private fun LapButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(Volt.copy(alpha = if (enabled) 0.08f else 0.03f), shape)
            .border(1.dp, Volt.copy(alpha = if (enabled) 0.45f else 0.15f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Flag,
            contentDescription = null,
            tint = if (enabled) Volt else Slate,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            color = if (enabled) Volt else Slate,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** 목표 거리 스테퍼 (− / +) */
@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CarbonHigh, CircleShape)
            .border(1.dp, Edge, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Volt,
        )
    }
}

/** 누적 랩 스냅샷 → 구간 리스트 (직전 랩과의 차) */
private fun lapSegments(laps: List<Lap>): List<LapSegment> {
    var prevKm = 0.0
    var prevSec = 0L
    return laps.map { lap ->
        val dKm = lap.km - prevKm
        val dSec = lap.splitSec - prevSec
        prevKm = lap.km
        prevSec = lap.splitSec
        LapSegment(
            index = lap.index,
            km = dKm,
            sec = dSec,
            paceSec = if (dKm >= 0.001 && dSec > 0) (dSec / dKm).toLong() else 0L,
        )
    }
}

/** 스플릿 페이스 기반 데모 심박 — 빠를수록 높게 */
private fun demoLapBpm(paceSec: Long): Int =
    if (paceSec <= 0) 0 else (232 - paceSec / 2.4).toInt().coerceIn(98, 186)

private fun formatPace(secPerKm: Long): String =
    "%d'%02d\"".format(secPerKm / 60, secPerKm % 60)

/** 페이스 게이지 정규화 — 15'00"/km ≈ 0, 5'00"/km ≈ 1 */
private fun paceFraction(secPerKm: Long?): Float =
    if (secPerKm == null || secPerKm <= 0) 0f else ((900f - secPerKm) / 600f).coerceIn(0f, 1f)

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
