package com.giwa.strideup.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
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
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.LevelAvatar
import com.giwa.strideup.ui.components.NeonRing
import com.giwa.strideup.ui.components.RouteMap
import com.giwa.strideup.ui.components.SneakerArt
import com.giwa.strideup.ui.components.StartRunButton
import com.giwa.strideup.ui.components.TokenCard
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.animatedInt
import com.giwa.strideup.ui.components.label
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.components.tint
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

/**
 * 홈 — 스크롤 없이 한 화면에 전부 담는다.
 *
 * 고정 높이 요소(상단바 · 인사 · CTA)를 먼저 잡고, 남는 공간을 카드들이
 * weight로 나눠 갖는다. 화면이 작아도 잘리지 않고 비율대로 줄어든다.
 */
@Composable
fun HomeScreen(
    onStartRun: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenItems: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var hasPermission by remember {
        mutableStateOf(StepPermissions.hasActivityRecognition(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = StepPermissions.hasActivityRecognition(context)
        if (hasPermission) viewModel.onPermissionGranted()
    }

    LifecycleResumeEffect(Unit) {
        val granted = StepPermissions.hasActivityRecognition(context)
        if (granted != hasPermission) {
            hasPermission = granted
            if (granted) viewModel.onPermissionGranted()
        }
        onPauseOrDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TopBar(unread, onOpenNotifications, onOpenWallet)

        GreetingRow(
            level = state.level,
            balance = state.balance,
            onOpenWallet = onOpenWallet,
            onOpenProfile = onOpenProfile,
        )

        if (!hasPermission) {
            PermissionStrip(
                onClick = { permissionLauncher.launch(StepPermissions.missing(context)) },
            )
        }

        StepsHeroCard(
            state = state,
            onOpenProfile = onOpenProfile,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.30f),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.12f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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

        SneakerStrip(
            state = state,
            onOpenItems = onOpenItems,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.74f),
        )

        StartRunButton(
            title = stringResource(R.string.start_run),
            subtitle = stringResource(R.string.start_run_sub),
            onClick = onStartRun,
        )
    }
}

@Composable
private fun TopBar(unread: Int, onOpenNotifications: () -> Unit, onOpenWallet: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(fontSize = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DarkIconButton(
                icon = Icons.Filled.Notifications,
                contentDescription = stringResource(R.string.cd_notifications),
                onClick = onOpenNotifications,
                badge = unread > 0,
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
private fun GreetingRow(
    level: Int,
    balance: Double,
    onOpenWallet: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val greetingRes = when (LocalTime.now().hour) {
        in 0..4 -> R.string.greeting_dawn
        in 5..10 -> R.string.greeting_morning
        in 11..16 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LevelAvatar(
            level = level,
            size = 46.dp,
            modifier = Modifier.quietClickable(onOpenProfile),
            contentDescription = stringResource(R.string.cd_profile),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = stringResource(greetingRes),
                fontSize = 11.sp,
                color = Silver,
            )
            Text(
                text = stringResource(R.string.greeting_runner),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = Snow,
            )
            Text(
                text = stringResource(R.string.level_chip, level) + " · " + runnerTier(level),
                fontSize = 10.sp,
                color = Volt,
                fontWeight = FontWeight.SemiBold,
            )
        }
        TokenCard(balance = balance, onClick = onOpenWallet)
    }
}

@Composable
private fun PermissionStrip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Volt.copy(alpha = 0.12f))
            .quietClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.DirectionsWalk,
            contentDescription = null,
            tint = Volt,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.perm_allow),
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Volt,
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Volt,
            modifier = Modifier.size(15.dp),
        )
    }
}

/** 오늘 걸음 히어로 — 좌측 큰 숫자 + 우측 루트 맵 + 목표 진행 바 */
@Composable
private fun StepsHeroCard(
    state: HomeViewModel.UiState,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (state.goal > 0) state.todaySteps.toFloat() / state.goal else 0f
    val steps = animatedInt(state.todaySteps)

    GlowCard(
        modifier = modifier,
        accent = true,
        contentPadding = PaddingValues(15.dp),
        spacing = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_total_steps),
                    fontSize = 11.sp,
                    color = Silver,
                )
                Text(
                    text = "%,d".format(steps),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.8).sp,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.home_daily_goal, "%,d".format(state.goal)),
                    fontSize = 10.sp,
                    color = Slate,
                )
            }
            RouteMap(
                modifier = Modifier
                    .weight(0.82f)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            BarMeter(fraction = fraction, modifier = Modifier.weight(1f), height = 7.dp)
            Text(
                text = "${state.goalPercent.coerceAtMost(999)}%",
                color = Volt,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.home_view_analytics),
                tint = Slate,
                modifier = Modifier
                    .size(16.dp)
                    .quietClickable(onOpenProfile),
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

    GlowCard(modifier = modifier, contentPadding = PaddingValues(13.dp), spacing = 6.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Volt, modifier = Modifier.size(13.dp))
            Text(
                text = stringResource(R.string.home_energy),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            NeonRing(
                progress = percent / 100f,
                modifier = Modifier.fillMaxHeight(),
                ringWidth = 7.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percent%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Snow,
                    )
                    Text(
                        text = "%.0f / %.0f".format(energy, maxEnergy),
                        fontSize = 9.sp,
                        color = Slate,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.home_recharge_in, countdown),
            fontSize = 10.sp,
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

    GlowCard(
        modifier = modifier.quietClickable(onOpenProfile),
        contentPadding = PaddingValues(13.dp),
        spacing = 6.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Volt, modifier = Modifier.size(13.dp))
            Text(
                text = stringResource(R.string.home_distance),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "%.2f".format(weekKm),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = Snow,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "km",
                fontSize = 11.sp,
                color = Slate,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
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
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            days.forEach { day ->
                val label = LocalDate.ofEpochDay(day).dayOfWeek
                    .getDisplayName(TextStyle.NARROW, Locale.getDefault())
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (day == today) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Volt, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = Night, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(label, color = Slate, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

/** 착용 스니커즈 요약 — 실제 NFT 아트를 보여준다 */
@Composable
private fun SneakerStrip(
    state: HomeViewModel.UiState,
    onOpenItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sneaker = state.equipped
    GlowCard(
        modifier = modifier.quietClickable(onOpenItems),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        spacing = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sneaker != null) {
                SneakerArt(
                    sneaker = sneaker,
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                )
            } else {
                Spacer(Modifier.weight(0.62f))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = sneaker?.let { "${it.faction.label()} ${it.variantName}" }
                        ?: "Wind Runner",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CarbonHigh)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.level_chip, state.level),
                            color = Volt,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "+%.1f%%".format(sneaker?.boostPercent ?: 0.0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = sneaker?.faction?.tint() ?: Silver,
                    )
                }
                BarMeter(
                    fraction = (sneaker?.durability ?: 85) / 100f,
                    height = 5.dp,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
