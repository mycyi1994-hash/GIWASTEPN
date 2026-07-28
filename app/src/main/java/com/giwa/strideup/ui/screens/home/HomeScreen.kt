package com.giwa.strideup.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.LevelAvatar
import com.giwa.strideup.ui.components.NeonRing
import com.giwa.strideup.ui.components.RouteMap
import com.giwa.strideup.ui.components.StartRunButton
import com.giwa.strideup.ui.components.TokenCard
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.animatedInt
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

/** 스니커즈 레벨 → 러너 칭호 (브랜드 고정 명칭) */
fun runnerTier(level: Int): String = when {
    level >= 7 -> "Legend"
    level >= 5 -> "Pacesetter"
    level >= 3 -> "Trailblazer"
    level >= 2 -> "Strider"
    else -> "Rookie"
}

@Composable
fun HomeScreen(
    onStartRun: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenEvents: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenItems: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(StepPermissions.hasActivityRecognition(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = StepPermissions.hasActivityRecognition(context)
        if (hasPermission) viewModel.onPermissionGranted()
    }

    // 시스템 설정에서 권한이 허용된 경우에도 카드가 남지 않도록 재개 시 재확인.
    LifecycleResumeEffect(Unit) {
        val granted = StepPermissions.hasActivityRecognition(context)
        if (granted != hasPermission) {
            hasPermission = granted
            if (granted) viewModel.onPermissionGranted()
        }
        onPauseOrDispose { }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item { TopBar(onOpenEvents, onOpenWallet) }

        item { GreetingRow(level = state.sneakerLevel, balance = state.balance, onOpenWallet = onOpenWallet) }

        if (!hasPermission) {
            item {
                GlowCard(accent = true, spacing = 11.dp) {
                    Text(
                        stringResource(R.string.perm_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Snow,
                    )
                    Text(
                        stringResource(R.string.perm_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                    GhostButton(
                        text = stringResource(R.string.perm_allow),
                        onClick = { permissionLauncher.launch(StepPermissions.missing(context)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item { StepsHeroCard(state, onOpenProfile) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                EnergyCard(
                    energy = state.energy,
                    maxEnergy = state.maxEnergy,
                    percent = state.energyPercent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                DistanceCard(
                    week = state.week,
                    onOpenProfile = onOpenProfile,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        item { SneakerCard(level = state.sneakerLevel, onOpenItems = onOpenItems) }

        item {
            StartRunButton(
                title = stringResource(R.string.start_run),
                subtitle = stringResource(R.string.start_run_sub),
                onClick = onStartRun,
            )
        }

        if (!state.sensorAvailable) {
            item {
                GlowCard(spacing = 7.dp) {
                    Text(
                        stringResource(R.string.sensor_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                    Text(
                        stringResource(R.string.sensor_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onOpenEvents: () -> Unit, onOpenWallet: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(fontSize = 22.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DarkIconButton(
                icon = Icons.Filled.Notifications,
                contentDescription = stringResource(R.string.cd_notifications),
                onClick = onOpenEvents,
                badge = true,
            )
            DarkIconButton(
                icon = Icons.Filled.AccountBalanceWallet,
                contentDescription = stringResource(R.string.cd_wallet),
                onClick = onOpenWallet,
            )
        }
    }
}

@Composable
private fun GreetingRow(level: Int, balance: Double, onOpenWallet: () -> Unit) {
    val greetingRes = when (LocalTime.now().hour) {
        in 0..4 -> R.string.greeting_dawn
        in 5..10 -> R.string.greeting_morning
        in 11..16 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LevelAvatar(level = level, size = 54.dp, contentDescription = stringResource(R.string.cd_profile))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(greetingRes),
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
            )
            Text(
                text = stringResource(R.string.greeting_runner),
                style = MaterialTheme.typography.headlineSmall,
                color = Snow,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Volt.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.level_chip, level),
                        color = Volt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = runnerTier(level),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
        }
        TokenCard(balance = balance, onClick = onOpenWallet)
    }
}

/** 오늘 걸음 히어로 — 좌측 큰 숫자 + 우측 루트 맵 + 목표 진행 바 */
@Composable
private fun StepsHeroCard(state: HomeViewModel.UiState, onOpenProfile: () -> Unit) {
    val fraction = if (state.goal > 0) state.todaySteps.toFloat() / state.goal else 0f
    val steps = animatedInt(state.todaySteps)

    GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_total_steps),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
                Text(
                    text = "%,d".format(steps),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-2).sp,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.home_today),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = Silver,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_daily_goal, "%,d".format(state.goal)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            RouteMap(
                modifier = Modifier
                    .weight(0.85f)
                    .height(128.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BarMeter(fraction = fraction, modifier = Modifier.weight(1f))
            Text(
                text = "${state.goalPercent.coerceAtMost(999)}%",
                color = Volt,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(CarbonHigh)
                .quietClickable(onOpenProfile)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = stringResource(R.string.home_view_analytics),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Snow,
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** 에너지 카드 — 네온 링 + 자정까지 리필 카운트다운 */
@Composable
private fun EnergyCard(
    energy: Double,
    maxEnergy: Double,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    var secondsLeft by remember { mutableIntStateOf(86_400 - LocalTime.now().toSecondOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            secondsLeft = 86_400 - LocalTime.now().toSecondOfDay()
            delay(1_000)
        }
    }
    val countdown = "%02d:%02d:%02d".format(
        secondsLeft / 3600,
        (secondsLeft % 3600) / 60,
        secondsLeft % 60,
    )

    GlowCard(modifier = modifier, contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Volt, modifier = Modifier.size(15.dp))
            Text(
                text = stringResource(R.string.home_energy),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            NeonRing(
                progress = percent / 100f,
                modifier = Modifier.size(108.dp),
                ringWidth = 9.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percent%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Snow,
                    )
                    Text(
                        text = "%.0f / %.0f".format(energy, maxEnergy),
                        fontSize = 10.sp,
                        color = Slate,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.home_recharge_in, countdown),
            style = MaterialTheme.typography.bodySmall,
            color = Volt,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 거리 카드 — 주간 미니 바 차트 */
@Composable
private fun DistanceCard(
    week: List<DailyStepsEntity>,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now().toEpochDay()
    val days = (0..6).map { offset -> today - 6 + offset }
    val byDay = week.associateBy { it.epochDay }
    val weekSteps = days.sumOf { (byDay[it]?.steps ?: 0).toLong() }
    val weekKm = RewardEconomy.distanceMeters(weekSteps.toInt()) / 1000
    val maxSteps = maxOf(week.maxOfOrNull { it.steps } ?: 0, 1)

    GlowCard(modifier = modifier, contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Volt, modifier = Modifier.size(15.dp))
            Text(
                text = stringResource(R.string.home_distance),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "%.2f".format(weekKm),
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = Snow,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "km",
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = stringResource(R.string.home_this_week),
            style = MaterialTheme.typography.bodySmall,
            color = Slate,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                val steps = byDay[day]?.steps ?: 0
                val fraction = (steps.toFloat() / maxSteps).coerceIn(0.08f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (day == today) Volt else Volt.copy(alpha = 0.30f)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            days.forEach { day ->
                val label = LocalDate.ofEpochDay(day).dayOfWeek
                    .getDisplayName(TextStyle.NARROW, Locale.getDefault())
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (day == today) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Volt, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = Night, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(label, color = Slate, fontSize = 9.sp)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(CarbonHigh)
                .quietClickable(onOpenProfile)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_view_details),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Snow,
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** 스니커즈 카드 — 헥사곤 플랫폼 위 스니커 + 능력치 */
@Composable
private fun SneakerCard(level: Int, onOpenItems: () -> Unit) {
    val multiplier = RewardEconomy.sneakerMultiplier(level)

    GlowCard(
        modifier = Modifier.quietClickable(onOpenItems),
        contentPadding = PaddingValues(18.dp),
        spacing = 13.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SneakerVisual(modifier = Modifier.size(96.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Apex Runner", style = MaterialTheme.typography.titleMedium, color = Snow)
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = Slate,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Volt.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.level_chip, level),
                        color = Volt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.home_durability),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                    Text(
                        text = "85/100",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Snow,
                    )
                }
                BarMeter(fraction = 0.85f, height = 6.dp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            MiniStat(
                label = stringResource(R.string.stat_efficiency),
                value = "×%.2f".format(multiplier),
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = stringResource(R.string.stat_luck),
                value = "3.6",
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = stringResource(R.string.stat_comfort),
                value = "4.2",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonHigh)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = Slate, fontSize = 10.sp)
        Text(value, color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** 글로우 플랫폼 위 스니커즈 비주얼 */
@Composable
private fun SneakerVisual(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val platformY = size.height * 0.82f
            // 바닥 글로우 타원
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Volt.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(cx, platformY),
                    radius = size.width * 0.55f,
                ),
                topLeft = Offset(cx - size.width * 0.48f, platformY - size.height * 0.10f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.96f, size.height * 0.20f),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
            contentDescription = null,
            tint = Volt,
            modifier = Modifier
                .size(54.dp)
                .padding(bottom = 8.dp),
        )
    }
}
