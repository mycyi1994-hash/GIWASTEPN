package com.giwa.strideup.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.components.EnergyBar
import com.giwa.strideup.ui.components.IconBadge
import com.giwa.strideup.ui.components.ProgressRing
import com.giwa.strideup.ui.components.StatItem
import com.giwa.strideup.ui.components.StrideCard
import com.giwa.strideup.ui.theme.NeonAmber
import com.giwa.strideup.ui.theme.NeonCyan
import com.giwa.strideup.ui.theme.NeonGreen
import com.giwa.strideup.ui.theme.NeonPurple
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HomeHeader(streak = state.streak)
        }

        if (!hasPermission) {
            item {
                StrideCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "걸음 측정 권한이 필요해요",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "오늘의 걸음 수를 세고 SUP 포인트를 적립하려면 신체 활동 권한을 허용해 주세요.",
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
                        progress = if (state.goal > 0) state.todaySteps.toFloat() / state.goal else 0f,
                        modifier = Modifier.size(220.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "%,d".format(state.todaySteps),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "목표 %,d 걸음".format(state.goal),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(
                            icon = Icons.Filled.Straighten,
                            tint = NeonCyan,
                            label = "거리",
                            value = "%.2f km".format(RewardEconomy.distanceMeters(state.todaySteps) / 1000),
                        )
                        StatItem(
                            icon = Icons.Filled.LocalFireDepartment,
                            tint = NeonAmber,
                            label = "칼로리",
                            value = "%.0f kcal".format(RewardEconomy.calories(state.todaySteps)),
                        )
                        StatItem(
                            icon = Icons.Filled.TrendingUp,
                            tint = NeonGreen,
                            label = "달성률",
                            value = "%d%%".format(
                                if (state.goal > 0) (state.todaySteps * 100 / state.goal) else 0
                            ),
                        )
                    }
                }
            }
        }

        item {
            StrideCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("에너지", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "%.1f / %.1f".format(state.energy, state.maxEnergy),
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen,
                        )
                    }
                    EnergyBar(current = state.energy, max = state.maxEnergy)
                    Text(
                        "에너지가 있는 동안 워킹 세션에서 SUP가 적립돼요. 매일 자정에 리필됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            StrideCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconBadge(icon = Icons.Filled.AccountBalanceWallet, tint = NeonPurple)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "SUP 포인트",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "%,.2f SUP".format(state.balance),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
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
private fun HomeHeader(streak: Int) {
    val today = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                today,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("안녕하세요, 러너님 👋", style = MaterialTheme.typography.titleLarge)
        }
        if (streak > 0) {
            Row(
                modifier = Modifier
                    .background(NeonAmber.copy(alpha = 0.15f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "연속 ${streak}일",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonAmber,
                )
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
                            .background(
                                if (day == today) NeonGreen else NeonGreen.copy(alpha = 0.35f),
                                RoundedCornerShape(6.dp),
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
