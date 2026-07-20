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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.components.LineProgress
import com.giwa.strideup.ui.components.ProfileBadge
import com.giwa.strideup.ui.components.ProgressRing
import com.giwa.strideup.ui.components.StartWalkButton
import com.giwa.strideup.ui.components.StrideCard
import com.giwa.strideup.ui.components.StrideUpWordmark
import com.giwa.strideup.ui.components.TokenBadge
import com.giwa.strideup.ui.theme.NeonGreen
import com.giwa.strideup.ui.theme.NeonGreenSoft
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeHeader(level = state.sneakerLevel) }

        if (!hasPermission) {
            item {
                StrideCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("걸음 측정 권한이 필요해요", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "오늘의 걸음 수를 세고 SUP를 적립하려면 신체 활동 권한을 허용해 주세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { permissionLauncher.launch(StepPermissions.missing(context)) }) {
                            Text("권한 허용하기")
                        }
                    }
                }
            }
        }

        item { TodayCard(state) }

        item { RouteCard() }

        item { EnergyTokenRow(state) }

        item {
            StartWalkButton(text = "START WALK", onClick = onStartWalk)
        }

        item {
            StrideCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("최근 7일", style = MaterialTheme.typography.titleMedium)
                    WeeklyChart(week = state.week, goal = state.goal)
                }
            }
        }

        if (!state.sensorAvailable) {
            item {
                StrideCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("걸음 센서를 찾을 수 없어요", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "이 기기(또는 에뮬레이터)에는 걸음 센서가 없습니다. 워킹 탭의 시뮬레이션 버튼으로 기능을 체험해 보세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StrideUpWordmark(fontSize = 26.sp)
        ProfileBadge(level = level)
    }
}

@Composable
private fun TodayCard(state: HomeViewModel.UiState) {
    val fraction = if (state.goal > 0) state.todaySteps.toFloat() / state.goal else 0f
    StrideCard {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "TODAY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        "%,d".format(state.todaySteps),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "STEPS",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp,
                    )
                }
                ProgressRing(
                    progress = fraction,
                    modifier = Modifier.size(96.dp),
                    ringWidth = 10.dp,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            LineProgress(fraction = fraction)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "${state.goalPercent}% OF GOAL",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "%,d".format(state.goal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** YOUR ROUTE — 발광 루트가 그려진 장식용 지도 카드 */
@Composable
private fun RouteCard() {
    StrideCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("YOUR ROUTE", style = MaterialTheme.typography.titleMedium, letterSpacing = 1.sp)
                Icon(Icons.Filled.Navigation, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0C0A)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // 격자
                    val gridColor = Color.White.copy(alpha = 0.04f)
                    val cols = 6
                    val rows = 4
                    for (c in 1 until cols) {
                        val x = w * c / cols
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    }
                    for (r in 1 until rows) {
                        val y = h * r / rows
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }
                    // 루트 경로
                    val pts = listOf(
                        0.08f to 0.72f, 0.22f to 0.56f, 0.34f to 0.64f, 0.48f to 0.40f,
                        0.62f to 0.52f, 0.78f to 0.34f, 0.92f to 0.26f,
                    ).map { Offset(w * it.first, h * it.second) }
                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                    }
                    drawPath(path, NeonGreen.copy(alpha = 0.20f), style = Stroke(width = 12f, cap = StrokeCap.Round))
                    drawPath(path, NeonGreen, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    // 시작/끝 마커
                    drawCircle(NeonGreen.copy(alpha = 0.3f), radius = 12f, center = pts.first())
                    drawCircle(Color.White, radius = 5f, center = pts.first())
                    drawCircle(NeonGreen.copy(alpha = 0.3f), radius = 14f, center = pts.last())
                    drawCircle(NeonGreenSoft, radius = 6f, center = pts.last())
                }
            }
        }
    }
}

@Composable
private fun EnergyTokenRow(state: HomeViewModel.UiState) {
    StrideCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProgressRing(
                    progress = state.energyPercent / 100f,
                    modifier = Modifier.size(52.dp),
                    ringWidth = 6.dp,
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "ENERGY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${state.energyPercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 44.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TokenBadge(size = 44.dp)
                Column {
                    Text(
                        "SUP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "%,.0f".format(state.balance),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEach { day ->
            val steps = byDay[day]?.steps ?: 0
            val fraction = (steps.toFloat() / maxSteps).coerceIn(0.02f, 1f)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (day == today) {
                                    Brush.verticalGradient(listOf(NeonGreenSoft, NeonGreen))
                                } else {
                                    Brush.verticalGradient(
                                        listOf(NeonGreen.copy(alpha = 0.35f), NeonGreen.copy(alpha = 0.25f))
                                    )
                                }
                            )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    LocalDate.ofEpochDay(day).dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (day == today) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
