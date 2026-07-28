package com.giwa.strideup.ui.screens.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.data.local.WalkSessionEntity
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.StatCell
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import com.giwa.strideup.ui.theme.VoltDeep
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AnalyticsViewModel(private val stepRepository: StepRepository) : ViewModel() {

    val week: StateFlow<List<DailyStepsEntity>> = stepRepository.observeWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lifetimeSteps: StateFlow<Long> = stepRepository.observeLifetimeSteps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val monthSteps: StateFlow<Long> = stepRepository.observeMonthSteps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val goal: StateFlow<Int> = stepRepository.dailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPrefs.DEFAULT_GOAL)

    val sessions: StateFlow<List<WalkSessionEntity>> = stepRepository.recentSessions(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                AnalyticsViewModel(ServiceLocator.stepRepository)
            }
        }
    }
}

private val sessionTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M.d HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun AnalyticsScreen(
    onBack: () -> Unit = {},
    viewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.Factory),
) {
    val week by viewModel.week.collectAsStateWithLifecycle()
    val lifetimeSteps by viewModel.lifetimeSteps.collectAsStateWithLifecycle()
    val monthSteps by viewModel.monthSteps.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Text(
                    text = stringResource(R.string.analytics_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Snow,
                )
            }
        }

        item { WeekChartCard(week = week, goal = goal) }

        item {
            StatGridCard(
                week = week,
                goal = goal,
                monthSteps = monthSteps,
                lifetimeSteps = lifetimeSteps,
            )
        }

        item { SectionHeader(title = stringResource(R.string.analytics_recent_runs)) }

        item { RecentRunsCard(sessions = sessions) }
    }
}

/** 지난 7일 큰 바 차트 — 볼트 바 + 점선 목표선 + 요일 이니셜 */
@Composable
private fun WeekChartCard(week: List<DailyStepsEntity>, goal: Int) {
    val today = LocalDate.now().toEpochDay()
    val days = (0..6).map { offset -> today - 6 + offset }
    val byDay = week.associateBy { it.epochDay }
    val weekSteps = days.sumOf { (byDay[it]?.steps ?: 0).toLong() }
    val maxValue = maxOf(week.maxOfOrNull { it.steps } ?: 0, goal, 1)

    GlowCard(accent = true, contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Eyebrow(text = stringResource(R.string.analytics_week))
                Text(
                    text = "%,d".format(weekSteps),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.2).sp,
                    color = Snow,
                )
            }
            Text(
                text = stringResource(R.string.home_daily_goal, "%,d".format(goal)),
                fontSize = 10.sp,
                color = Slate,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    val steps = byDay[day]?.steps ?: 0
                    val fraction = (steps.toFloat() / maxValue).coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (day == today) Volt else Volt.copy(alpha = 0.30f)),
                    )
                }
            }
            Canvas(Modifier.matchParentSize()) {
                val y = size.height * (1f - (goal.toFloat() / maxValue).coerceIn(0f, 1f))
                drawLine(
                    color = VoltDeep.copy(alpha = 0.85f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEach { day ->
                val label = LocalDate.ofEpochDay(day).dayOfWeek
                    .getDisplayName(TextStyle.NARROW, Locale.getDefault())
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (day == today) {
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .background(Volt, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = Night, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(label, color = Slate, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/** 주간 평균 · 최고 기록 · 목표 달성률 · 이달 거리 2×2 그리드 */
@Composable
private fun StatGridCard(
    week: List<DailyStepsEntity>,
    goal: Int,
    monthSteps: Long,
    lifetimeSteps: Long,
) {
    val today = LocalDate.now().toEpochDay()
    val days = (0..6).map { offset -> today - 6 + offset }
    val byDay = week.associateBy { it.epochDay }
    val weekSteps = days.sumOf { (byDay[it]?.steps ?: 0).toLong() }
    val dailyAvg = weekSteps / 7
    val bestDay = week.maxOfOrNull { it.steps } ?: 0
    val metDays = days.count { day ->
        byDay[day]?.let { entry ->
            entry.steps >= (if (entry.goal > 0) entry.goal else goal)
        } == true
    }
    val goalRate = metDays * 100 / 7
    val monthKm = monthSteps * RewardEconomy.STRIDE_METERS / 1000

    GlowCard(contentPadding = PaddingValues(vertical = 18.dp, horizontal = 10.dp), spacing = 16.dp) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.Filled.TrendingUp,
                label = stringResource(R.string.analytics_daily_avg),
                value = "%,d".format(dailyAvg),
            )
            StatCell(
                icon = Icons.Filled.EmojiEvents,
                label = stringResource(R.string.analytics_best_day),
                value = "%,d".format(bestDay),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.Filled.CheckCircle,
                label = stringResource(R.string.analytics_goal_rate),
                value = "$goalRate%",
            )
            StatCell(
                icon = Icons.Filled.LocationOn,
                label = stringResource(R.string.stat_distance),
                value = "%.2f km".format(monthKm),
            )
        }
        HairlineDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(text = stringResource(R.string.analytics_all))
            Spacer(Modifier.weight(1f))
            Text(
                text = "%,d".format(lifetimeSteps),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Snow,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.goal_steps_suffix),
                fontSize = 11.sp,
                color = Slate,
            )
        }
    }
}

/** 최근 러닝 세션 최대 5건 — 날짜 · 시간 · 걸음 · 적립 */
@Composable
private fun RecentRunsCard(sessions: List<WalkSessionEntity>) {
    if (sessions.isEmpty()) {
        GlowCard(contentPadding = PaddingValues(26.dp)) {
            Text(
                text = stringResource(R.string.analytics_no_runs),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
            )
        }
        return
    }

    GlowCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), spacing = 0.dp) {
        sessions.take(5).forEachIndexed { index, session ->
            if (index > 0) HairlineDivider()
            SessionRow(session)
        }
    }
}

@Composable
private fun SessionRow(session: WalkSessionEntity) {
    val duration = "%02d:%02d".format(session.durationSec / 60, session.durationSec % 60)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IconSquare(icon = Icons.AutoMirrored.Filled.DirectionsWalk, size = 38.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = sessionTimeFormatter.format(Instant.ofEpochMilli(session.startedAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = Snow,
            )
            Text(
                text = duration + " · " + stringResource(R.string.notification_steps, session.steps),
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
            )
        }
        Text(
            text = "+%,.2f".format(session.pointsEarned),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Volt,
        )
    }
}
