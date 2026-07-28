package com.giwa.strideup.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.components.ChipButton
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GradientText
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.MemberBadge
import com.giwa.strideup.ui.components.SoftCard
import com.giwa.strideup.ui.components.StatColumn
import com.giwa.strideup.ui.components.SunsetButton
import com.giwa.strideup.ui.components.SunsetRing
import com.giwa.strideup.ui.components.TokenCoin
import com.giwa.strideup.ui.components.VerticalHairline
import com.giwa.strideup.ui.components.animatedInt
import com.giwa.strideup.ui.theme.Coral
import com.giwa.strideup.ui.theme.Honey
import com.giwa.strideup.ui.theme.HoneyInk
import com.giwa.strideup.ui.theme.Ink
import com.giwa.strideup.ui.theme.Sage
import com.giwa.strideup.ui.theme.Sand
import com.giwa.strideup.ui.theme.SunsetVertical
import com.giwa.strideup.ui.theme.Taupe
import com.giwa.strideup.ui.theme.TaupeLight
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val headerDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

/** 시간대에 맞춘 캐주얼한 인사 */
private fun greeting(): String = when (LocalTime.now().hour) {
    in 0..4 -> "고요한 새벽이에요"
    in 5..10 -> "좋은 아침이에요"
    in 11..16 -> "오후도 가볍게 걸어요"
    in 17..20 -> "노을 산책, 어때요?"
    else -> "오늘 하루도 수고했어요"
}

@Composable
fun HomeScreen(
    onStartWalk: () -> Unit = {},
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeHeader(level = state.sneakerLevel) }

        if (!hasPermission) {
            item {
                SoftCard(accent = true, spacing = 12.dp) {
                    Text(
                        "걸음을 세려면 권한이 필요해요",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                    )
                    Text(
                        "오늘의 걸음을 집계하고 SUP를 쌓으려면 신체 활동 권한을 허용해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Taupe,
                    )
                    ChipButton(
                        text = "권한 허용하기",
                        onClick = { permissionLauncher.launch(StepPermissions.missing(context)) },
                    )
                }
            }
        }

        item { TodayCard(state) }

        item { SunsetButton(text = "산책 시작하기", onClick = onStartWalk) }

        item { AssetRow(state) }

        item { RouteCard() }

        item {
            SoftCard(spacing = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("이번 주 걸음", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text(
                        "목표 %,d".format(state.goal),
                        style = MaterialTheme.typography.bodySmall,
                        color = TaupeLight,
                    )
                }
                WeeklyChart(week = state.week, goal = state.goal)
            }
        }

        if (!state.sensorAvailable) {
            item {
                SoftCard(spacing = 8.dp) {
                    Text(
                        "걸음 센서를 찾을 수 없어요",
                        style = MaterialTheme.typography.titleSmall,
                        color = Ink,
                    )
                    Text(
                        "이 기기(또는 에뮬레이터)에는 걸음 센서가 없어요. " +
                            "워킹 탭의 시뮬레이션 버튼으로 적립 흐름을 체험해 보세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Taupe,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(level: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = headerDateFormatter.format(LocalDate.now()),
                style = MaterialTheme.typography.bodySmall,
                color = TaupeLight,
            )
            Text(
                text = greeting(),
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
            )
        }
        MemberBadge(level = level)
    }
}

/** 오늘의 성과 — 화면의 주인공. 큼직한 선셋 링 하나로 말한다. */
@Composable
private fun TodayCard(state: HomeViewModel.UiState) {
    val fraction = if (state.goal > 0) state.todaySteps.toFloat() / state.goal else 0f
    val steps = animatedInt(state.todaySteps)
    val distanceKm = RewardEconomy.distanceMeters(state.todaySteps) / 1000
    val calories = RewardEconomy.calories(state.todaySteps)

    SoftCard(accent = true, contentPadding = PaddingValues(24.dp), spacing = 20.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SunsetRing(
                progress = fraction,
                modifier = Modifier.size(200.dp),
                ringWidth = 15.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "%,d".format(steps),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1.5).sp,
                        color = Ink,
                    )
                    Text(
                        text = "걸음",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Taupe,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Sand)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "오늘 목표 %,d · %d%% 달성".format(
                        state.goal,
                        state.goalPercent.coerceAtMost(999),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Taupe,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        HairlineDivider()

        Row(modifier = Modifier.fillMaxWidth()) {
            StatColumn(
                icon = Icons.Filled.Straighten,
                tint = Sage,
                label = "거리",
                value = "%.2f km".format(distanceKm),
            )
            StatColumn(
                icon = Icons.Filled.LocalFireDepartment,
                tint = Coral,
                label = "칼로리",
                value = "%,.0f".format(calories),
            )
            StatColumn(
                icon = Icons.Filled.AutoAwesome,
                tint = Honey,
                label = "스트릭",
                value = "${state.streak}일",
            )
        }
    }
}

/** 보유 자산 — SUP 잔액과 에너지를 한 줄에. */
@Composable
private fun AssetRow(state: HomeViewModel.UiState) {
    SoftCard(contentPadding = PaddingValues(vertical = 18.dp, horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TokenCoin(size = 40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Eyebrow("SUP", color = TaupeLight)
                    GradientText(
                        text = "%,.1f".format(state.balance),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        brush = HoneyInk,
                    )
                }
            }

            VerticalHairline(height = 42.dp)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SunsetRing(
                    progress = state.energyPercent / 100f,
                    modifier = Modifier.size(40.dp),
                    ringWidth = 4.dp,
                    glowAlpha = 0.06f,
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Eyebrow("에너지", color = TaupeLight)
                    Text(
                        text = "${state.energyPercent}%",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                }
            }
        }
    }
}

/** 오늘의 루트 — 코럴 궤적이 그려진 장식용 지도 카드 */
@Composable
private fun RouteCard() {
    SoftCard(spacing = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("오늘의 루트", style = MaterialTheme.typography.titleMedium, color = Ink)
            Icon(
                Icons.Filled.NearMe,
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(16.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Sand),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 은은한 격자
                val gridColor = Ink.copy(alpha = 0.04f)
                for (c in 1 until 7) {
                    val x = w * c / 7
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                }
                for (r in 1 until 4) {
                    val y = h * r / 4
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                // 산책 궤적
                val points = listOf(
                    0.08f to 0.74f, 0.21f to 0.58f, 0.33f to 0.66f, 0.47f to 0.40f,
                    0.61f to 0.53f, 0.77f to 0.33f, 0.92f to 0.25f,
                ).map { Offset(w * it.first, h * it.second) }
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                }
                drawPath(path, Coral.copy(alpha = 0.18f), style = Stroke(width = 12f, cap = StrokeCap.Round))
                drawPath(path, brush = SunsetVertical, style = Stroke(width = 4f, cap = StrokeCap.Round))

                // 출발 · 도착 핀
                drawCircle(Coral.copy(alpha = 0.25f), radius = 11f, center = points.first())
                drawCircle(Coral, radius = 4f, center = points.first())
                drawCircle(Coral.copy(alpha = 0.30f), radius = 13f, center = points.last())
                drawCircle(Color.White, radius = 6.5f, center = points.last())
                drawCircle(Coral, radius = 4f, center = points.last())
            }
        }
    }
}

@Composable
private fun WeeklyChart(week: List<DailyStepsEntity>, goal: Int) {
    val today = LocalDate.now().toEpochDay()
    val days = (0..6).map { offset -> today - 6 + offset }
    val byDay = week.associateBy { it.epochDay }
    val maxSteps = maxOf(goal, week.maxOfOrNull { it.steps } ?: 0, 1)

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            // 목표선
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - goal.toFloat() / maxSteps)
                drawLine(
                    color = TaupeLight,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
                )
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    val steps = byDay[day]?.steps ?: 0
                    val fraction = (steps.toFloat() / maxSteps).coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(7.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (day == today) {
                                        Modifier.background(SunsetVertical)
                                    } else {
                                        Modifier.background(Sand)
                                    },
                                ),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            days.forEach { day ->
                Text(
                    text = LocalDate.ofEpochDay(day).dayOfWeek
                        .getDisplayName(TextStyle.NARROW, Locale.KOREAN),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day == today) Coral else TaupeLight,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
