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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.graphics.Brush
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
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GoldButton
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.LineMeter
import com.giwa.strideup.ui.components.LuxeCard
import com.giwa.strideup.ui.components.MemberBadge
import com.giwa.strideup.ui.components.MetalRing
import com.giwa.strideup.ui.components.MetalText
import com.giwa.strideup.ui.components.StatColumn
import com.giwa.strideup.ui.components.TokenEmblem
import com.giwa.strideup.ui.components.VerticalHairline
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.animatedInt
import com.giwa.strideup.ui.theme.Ash
import com.giwa.strideup.ui.theme.AshDim
import com.giwa.strideup.ui.theme.Champagne
import com.giwa.strideup.ui.theme.ChampagneLight
import com.giwa.strideup.ui.theme.Copper
import com.giwa.strideup.ui.theme.Ivory
import com.giwa.strideup.ui.theme.MetalInk
import com.giwa.strideup.ui.theme.ObsidianDeep
import com.giwa.strideup.ui.theme.Platinum
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val headerDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

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

    // 루트/시스템 설정에서 권한이 허용된 경우에도 카드가 남지 않도록 재개 시 재확인.
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { HomeHeader(level = state.sneakerLevel) }

        if (!hasPermission) {
            item {
                LuxeCard(accent = true, spacing = 12.dp) {
                    Eyebrow("PERMISSION", color = Champagne)
                    Text(
                        "걸음 측정 권한이 필요합니다",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ivory,
                    )
                    Text(
                        "오늘의 걸음을 집계하고 SUP를 적립하려면 신체 활동 권한을 허용해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ash,
                    )
                    GhostButton(
                        text = "권한 허용하기",
                        onClick = { permissionLauncher.launch(StepPermissions.missing(context)) },
                    )
                }
            }
        }

        item { TodayCard(state) }

        item { GoldButton(text = "Start Walk", onClick = onStartWalk) }

        item { AssetRow(state) }

        item { RouteCard() }

        item {
            LuxeCard(spacing = 18.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Eyebrow("Last 7 days")
                    Text(
                        "목표 %,d".format(state.goal),
                        style = MaterialTheme.typography.bodySmall,
                        color = AshDim,
                    )
                }
                WeeklyChart(week = state.week, goal = state.goal)
            }
        }

        if (!state.sensorAvailable) {
            item {
                LuxeCard(spacing = 8.dp) {
                    Eyebrow("Sensor", color = AshDim)
                    Text(
                        "걸음 센서를 찾을 수 없습니다",
                        style = MaterialTheme.typography.titleSmall,
                        color = Ivory,
                    )
                    Text(
                        "이 기기(또는 에뮬레이터)에는 걸음 센서가 없습니다. " +
                            "워킹 탭의 시뮬레이션 버튼으로 적립 흐름을 체험해 보세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ash,
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
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Wordmark(fontSize = 19.sp)
            Text(
                text = headerDateFormatter.format(LocalDate.now()),
                style = MaterialTheme.typography.bodySmall,
                color = AshDim,
            )
        }
        MemberBadge(level = level)
    }
}

/** 오늘의 성과 — 화면의 주인공. 가는 획의 큰 숫자 + 금속 링. */
@Composable
private fun TodayCard(state: HomeViewModel.UiState) {
    val fraction = if (state.goal > 0) state.todaySteps.toFloat() / state.goal else 0f
    val steps = animatedInt(state.todaySteps)
    val distanceKm = RewardEconomy.distanceMeters(state.todaySteps) / 1000
    val calories = RewardEconomy.calories(state.todaySteps)

    LuxeCard(accent = true, contentPadding = PaddingValues(22.dp), spacing = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Eyebrow("Today")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%,d".format(steps),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2.5).sp,
                        color = Ivory,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "steps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AshDim,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
            MetalRing(
                progress = fraction,
                modifier = Modifier.size(92.dp),
                ringWidth = 7.dp,
            ) {
                MetalText(
                    text = "${state.goalPercent.coerceAtMost(999)}%",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Light,
                )
            }
        }

        LineMeter(fraction = fraction)

        HairlineDivider()

        Row(modifier = Modifier.fillMaxWidth()) {
            StatColumn(
                icon = Icons.Filled.Straighten,
                tint = Platinum,
                label = "거리",
                value = "%.2f km".format(distanceKm),
            )
            StatColumn(
                icon = Icons.Filled.LocalFireDepartment,
                tint = Copper,
                label = "칼로리",
                value = "%,.0f kcal".format(calories),
            )
            StatColumn(
                icon = Icons.Filled.Whatshot,
                tint = Champagne,
                label = "스트릭",
                value = "${state.streak}일",
            )
        }
    }
}

/** 보유 자산 — SUP 잔액과 에너지를 한 줄에 나란히. */
@Composable
private fun AssetRow(state: HomeViewModel.UiState) {
    LuxeCard(contentPadding = PaddingValues(vertical = 18.dp, horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                TokenEmblem(size = 40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Eyebrow("SUP 잔액", color = AshDim)
                    MetalText(
                        text = "%,.1f".format(state.balance),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }

            VerticalHairline(height = 42.dp)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                MetalRing(
                    progress = state.energyPercent / 100f,
                    modifier = Modifier.size(40.dp),
                    ringWidth = 4.dp,
                    glowAlpha = 0.06f,
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = Champagne,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Eyebrow("에너지", color = AshDim)
                    Text(
                        text = "${state.energyPercent}%",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Light,
                        color = Ivory,
                    )
                }
            }
        }
    }
}

/** YOUR ROUTE — 금빛 궤적이 그려진 장식용 지도 카드 */
@Composable
private fun RouteCard() {
    LuxeCard(spacing = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow("Your route")
            Icon(
                Icons.Filled.NearMe,
                contentDescription = null,
                tint = Champagne.copy(alpha = 0.8f),
                modifier = Modifier.size(15.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianDeep),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 지적도 격자
                val gridColor = Color.White.copy(alpha = 0.035f)
                for (c in 1 until 7) {
                    val x = w * c / 7
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                }
                for (r in 1 until 4) {
                    val y = h * r / 4
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                // 궤적
                val points = listOf(
                    0.08f to 0.74f, 0.21f to 0.58f, 0.33f to 0.66f, 0.47f to 0.40f,
                    0.61f to 0.53f, 0.77f to 0.33f, 0.92f to 0.25f,
                ).map { Offset(w * it.first, h * it.second) }
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                }
                drawPath(path, Champagne.copy(alpha = 0.16f), style = Stroke(width = 11f, cap = StrokeCap.Round))
                drawPath(
                    path,
                    brush = Brush.horizontalGradient(listOf(Champagne, ChampagneLight)),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                )

                // 출발 · 도착
                drawCircle(Champagne.copy(alpha = 0.22f), radius = 11f, center = points.first())
                drawCircle(Champagne, radius = 3.5f, center = points.first())
                drawCircle(ChampagneLight.copy(alpha = 0.25f), radius = 13f, center = points.last())
                drawCircle(ChampagneLight, radius = 4.5f, center = points.last())
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
    val dimBar = Brush.verticalGradient(
        listOf(Champagne.copy(alpha = 0.34f), Champagne.copy(alpha = 0.10f)),
    )

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp),
        ) {
            // 목표선
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - goal.toFloat() / maxSteps)
                drawLine(
                    color = Champagne.copy(alpha = 0.30f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f)),
                )
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    val steps = byDay[day]?.steps ?: 0
                    val fraction = (steps.toFloat() / maxSteps).coerceIn(0.02f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (day == today) MetalInk else dimBar),
                    )
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
                    color = if (day == today) Champagne else AshDim,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
