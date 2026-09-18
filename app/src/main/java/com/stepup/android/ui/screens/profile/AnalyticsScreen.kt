package com.stepup.android.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.DailyStepsEntity
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.StepRepository
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HairlineDivider
import com.stepup.android.ui.components.IconSquare
import com.stepup.android.ui.components.SectionHeader
import com.stepup.android.ui.components.StatCell
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.screens.community.SegmentedTabs
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import com.stepup.android.ui.theme.VoltDeep
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

    /** 지난 90일. 과거 탭의 재료다. */
    val quarter: StateFlow<List<DailyStepsEntity>> = stepRepository.observeDays(QUARTER_DAYS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 과거 탭의 러닝 통계는 더 많은 세션이 필요하다 */
    val allSessions: StateFlow<List<WalkSessionEntity>> = stepRepository.recentSessions(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        /** 과거 탭이 보는 기간. 3개월. */
        const val QUARTER_DAYS = 90

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
    val quarter by viewModel.quarter.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    // 주간과 3개월은 보는 눈이 다르다. 주간은 "어제보다 오늘", 3개월은
    // "요즘 늘고 있나". 한 화면에 다 쌓으면 둘 다 흐려진다.
    var tab by rememberSaveable { mutableIntStateOf(0) }

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

        item {
            SegmentedTabs(
                labels = listOf(
                    stringResource(R.string.analytics_tab_week),
                    stringResource(R.string.analytics_tab_quarter),
                ),
                selected = tab,
                onSelect = { tab = it },
            )
        }

        if (tab == 0) {
            item { WeekChartCard(week = week, goal = goal) }

            item { WeekSummaryCard(week = week, goal = goal) }

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
        } else {
            item { QuarterChartCard(days = quarter, goal = goal) }

            item { QuarterSummaryCard(days = quarter, goal = goal) }

            item { SectionHeader(title = stringResource(R.string.analytics_months)) }

            item { MonthlyBreakdownCard(days = quarter) }

            item { SectionHeader(title = stringResource(R.string.analytics_run_stats)) }

            item { RunStatsCard(sessions = allSessions) }
        }
    }
}

/**
 * 지난 7일 큰 바 차트 — 볼트 바 + 점선 목표선 + 요일 이니셜.
 *
 * 막대를 누르면 그날의 기록이 작은 창으로 뜬다. 막대 높이만으로는 "수요일이
 * 목요일보다 조금 높다"까지만 읽히고, 정작 궁금한 "그래서 몇 보 걸었나"는
 * 알 수 없다. 누르는 동작 하나로 그 답을 준다.
 */
@Composable
private fun WeekChartCard(week: List<DailyStepsEntity>, goal: Int) {
    val today = LocalDate.now().toEpochDay()
    val days = (0..6).map { offset -> today - 6 + offset }
    val byDay = week.associateBy { it.epochDay }
    val weekSteps = days.sumOf { (byDay[it]?.steps ?: 0).toLong() }
    val maxValue = maxOf(week.maxOfOrNull { it.steps } ?: 0, goal, 1)

    // 고른 날. 화면을 나갔다 와도 유지된다.
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }

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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_daily_goal, "%,d".format(goal)),
                    fontSize = 10.sp,
                    color = Slate,
                )
                // 누를 수 있다는 것을 모르면 없는 기능이나 같다.
                Text(
                    text = stringResource(R.string.analytics_tap_hint),
                    fontSize = 9.sp,
                    color = Volt.copy(alpha = 0.75f),
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            val chartWidth = maxWidth
            val gap = 8.dp
            val slot = (chartWidth - gap * (days.size - 1)) / days.size

            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    val steps = byDay[day]?.steps ?: 0
                    val fraction = (steps.toFloat() / maxValue).coerceIn(0.04f, 1f)
                    val selected = day == selectedDay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            // 막대만 누르면 손가락보다 얇아 자꾸 빗나간다.
                            // 칸 전체(빈 위쪽 포함)를 누를 수 있게 한다.
                            .fillMaxHeight()
                            .quietClickable {
                                selectedDay = if (selected) null else day
                            },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        selected -> Volt
                                        day == today -> Volt.copy(alpha = 0.75f)
                                        else -> Volt.copy(alpha = 0.30f)
                                    },
                                ),
                        )
                    }
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

            // 고른 막대 위에 뜨는 작은 창.
            //
            // 카드 아래에 붙이지 않고 차트 안에 띄우는 이유는, 창이 생길 때마다
            // 카드가 늘어나면 아래 내용이 밀려 내려가 눈이 따라가야 하기 때문이다.
            // 막대 쪽으로 붙여 두면 어느 날 것인지도 따로 읽을 필요가 없다.
            selectedDay?.let { day ->
                val index = days.indexOf(day)
                if (index >= 0) {
                    val panelWidth = 132.dp
                    val center = slot * index + slot / 2 + gap * index
                    val x = (center - panelWidth / 2)
                        .coerceIn(0.dp, (chartWidth - panelWidth).coerceAtLeast(0.dp))
                    DayCallout(
                        day = day,
                        steps = byDay[day]?.steps ?: 0,
                        goal = goal,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = x)
                            .width(panelWidth),
                    )
                }
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
                    if (day == today || day == selectedDay) {
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

/**
 * 막대 하나를 눌렀을 때 뜨는 작은 창.
 *
 * 걸음 수만 있으면 "5,200보"가 어느 정도인지 감이 안 온다. 거리와 칼로리를
 * 같이 보여주면 같은 숫자가 몸으로 읽힌다 — 그래서 셋을 함께 둔다.
 */
@Composable
private fun DayCallout(
    day: Long,
    steps: Int,
    goal: Int,
    modifier: Modifier = Modifier,
) {
    val date = LocalDate.ofEpochDay(day)
    val km = RewardEconomy.distanceMeters(steps) / 1000.0
    val kcal = RewardEconomy.calories(steps)
    val rate = if (goal > 0) (steps * 100 / goal).coerceAtMost(999) else 0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Night.copy(alpha = 0.95f))
            .border(1.dp, Volt.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date.format(calloutDateFormatter),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
            Text(
                text = "$rate%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                // 목표를 넘긴 날은 색으로 먼저 보인다.
                color = if (steps >= goal) Volt else Slate,
            )
        }

        CalloutRow(stringResource(R.string.stat_steps), "%,d".format(steps))
        CalloutRow(stringResource(R.string.stat_distance), "%.2f km".format(km))
        CalloutRow(stringResource(R.string.stat_calories), "%,.0f kcal".format(kcal))
    }
}

@Composable
private fun CalloutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 10.sp, color = Slate)
        Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Snow)
    }
}

private val calloutDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M.d (E)", Locale.getDefault())

// ═══════════════════════════════════════════════════════════════════
//  주간 탭 — 이번 주 한눈에
// ═══════════════════════════════════════════════════════════════════

/**
 * 이번 주 합계.
 *
 * 막대 차트는 "어느 날이 높았나"를 보여 주지만 "그래서 이번 주에 얼마나
 * 움직였나"는 더해 봐야 안다. 그 덧셈을 대신한다.
 */
@Composable
private fun WeekSummaryCard(week: List<DailyStepsEntity>, goal: Int) {
    val today = LocalDate.now().toEpochDay()
    val days = (0..6).map { offset -> today - 6 + offset }
    val byDay = week.associateBy { it.epochDay }
    val steps = days.sumOf { (byDay[it]?.steps ?: 0).toLong() }
    val metDays = days.count { day ->
        byDay[day]?.let { it.steps >= (if (it.goal > 0) it.goal else goal) } == true
    }
    val activeDays = days.count { (byDay[it]?.steps ?: 0) > 0 }

    GlowCard(contentPadding = PaddingValues(vertical = 18.dp, horizontal = 10.dp), spacing = 16.dp) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = stringResource(R.string.stat_steps),
                value = "%,d".format(steps),
            )
            StatCell(
                icon = Icons.Filled.LocationOn,
                label = stringResource(R.string.stat_distance),
                value = "%.1f km".format(steps * RewardEconomy.STRIDE_METERS / 1000),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.Filled.LocalFireDepartment,
                label = stringResource(R.string.stat_calories),
                value = "%,.0f".format(steps * RewardEconomy.KCAL_PER_STEP),
            )
            StatCell(
                icon = Icons.Filled.CheckCircle,
                label = stringResource(R.string.analytics_goal_days),
                value = stringResource(R.string.analytics_days_of, metDays, activeDays),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  과거 탭 — 3개월
// ═══════════════════════════════════════════════════════════════════

/** 한 주 묶음 — 과거 차트의 막대 하나 */
private data class WeekBucket(
    val start: LocalDate,
    val end: LocalDate,
    val steps: Long,
    val activeDays: Int,
)

/**
 * 90일을 주 단위로 묶는다.
 *
 * 90개 막대를 그대로 세우면 폰 화면에서 한 막대가 2dp 남짓이라 아무것도
 * 읽히지 않는다. 주 단위로 묶으면 13개가 되고, 그 정도가 "요즘 늘고 있나"를
 * 보기에 맞다.
 */
private fun weekBuckets(days: List<DailyStepsEntity>, weeks: Int = 13): List<WeekBucket> {
    val byDay = days.associateBy { it.epochDay }
    val today = LocalDate.now()
    // 이번 주 월요일을 기준으로 뒤로 센다.
    val thisMonday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
    return (weeks - 1 downTo 0).map { back ->
        val start = thisMonday.minusWeeks(back.toLong())
        val end = start.plusDays(6)
        val range = (0..6).map { start.plusDays(it.toLong()).toEpochDay() }
        WeekBucket(
            start = start,
            end = end,
            steps = range.sumOf { (byDay[it]?.steps ?: 0).toLong() },
            activeDays = range.count { (byDay[it]?.steps ?: 0) > 0 },
        )
    }
}

/** 3개월 주간 막대 — 막대를 누르면 그 주 기록 */
@Composable
private fun QuarterChartCard(days: List<DailyStepsEntity>, goal: Int) {
    val buckets = remember(days) { weekBuckets(days) }
    val maxValue = maxOf(buckets.maxOfOrNull { it.steps } ?: 0L, 1L)
    val total = buckets.sumOf { it.steps }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }

    GlowCard(accent = true, contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Eyebrow(text = stringResource(R.string.analytics_tab_quarter))
                Text(
                    text = "%,d".format(total),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.2).sp,
                    color = Snow,
                )
            }
            Text(
                text = stringResource(R.string.analytics_tap_hint_week),
                fontSize = 9.sp,
                color = Volt.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            val chartWidth = maxWidth
            val gap = 3.dp
            val slot = (chartWidth - gap * (buckets.size - 1)) / buckets.size

            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Bottom,
            ) {
                buckets.forEachIndexed { index, bucket ->
                    val fraction = (bucket.steps.toFloat() / maxValue).coerceIn(0.03f, 1f)
                    val isSelected = index == selected
                    val isThisWeek = index == buckets.lastIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .quietClickable { selected = if (isSelected) null else index },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isSelected -> Volt
                                        isThisWeek -> Volt.copy(alpha = 0.75f)
                                        else -> Volt.copy(alpha = 0.28f)
                                    },
                                ),
                        )
                    }
                }
            }

            // 주 평균 목표선 — 하루 목표 × 7
            Canvas(Modifier.matchParentSize()) {
                val weekGoal = (goal.toLong() * 7).toFloat()
                val y = size.height * (1f - (weekGoal / maxValue).coerceIn(0f, 1f))
                drawLine(
                    color = VoltDeep.copy(alpha = 0.85f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                )
            }

            selected?.let { index ->
                val bucket = buckets[index]
                val panelWidth = 150.dp
                val center = slot * index + slot / 2 + gap * index
                val x = (center - panelWidth / 2)
                    .coerceIn(0.dp, (chartWidth - panelWidth).coerceAtLeast(0.dp))
                WeekCallout(
                    bucket = bucket,
                    goal = goal,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = x)
                        .width(panelWidth),
                )
            }
        }

        // 맨 왼쪽 주와 이번 주만 적는다. 13개를 다 적으면 글자가 겹친다.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = buckets.firstOrNull()?.start?.format(monthDayFormatter).orEmpty(),
                fontSize = 9.sp,
                color = Slate,
            )
            Text(
                text = stringResource(R.string.analytics_this_week),
                fontSize = 9.sp,
                color = Slate,
            )
        }
    }
}

/** 주 막대를 눌렀을 때 뜨는 작은 창 */
@Composable
private fun WeekCallout(bucket: WeekBucket, goal: Int, modifier: Modifier = Modifier) {
    val km = bucket.steps * RewardEconomy.STRIDE_METERS / 1000
    val dailyAvg = if (bucket.activeDays > 0) bucket.steps / bucket.activeDays else 0L
    val rate = if (goal > 0) (bucket.steps * 100 / (goal.toLong() * 7)).toInt() else 0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Night.copy(alpha = 0.95f))
            .border(1.dp, Volt.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bucket.start.format(monthDayFormatter) + " – " +
                    bucket.end.format(monthDayFormatter),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
            )
            Text(
                text = "$rate%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (rate >= 100) Volt else Slate,
            )
        }
        CalloutRow(stringResource(R.string.stat_steps), "%,d".format(bucket.steps))
        CalloutRow(stringResource(R.string.stat_distance), "%.1f km".format(km))
        CalloutRow(stringResource(R.string.analytics_daily_avg), "%,d".format(dailyAvg))
        CalloutRow(
            stringResource(R.string.analytics_active_days),
            stringResource(R.string.analytics_days_count, bucket.activeDays),
        )
    }
}

/** 3개월 합계 */
@Composable
private fun QuarterSummaryCard(days: List<DailyStepsEntity>, goal: Int) {
    val steps = days.sumOf { it.steps.toLong() }
    val activeDays = days.count { it.steps > 0 }
    val metDays = days.count { it.steps >= (if (it.goal > 0) it.goal else goal) }
    val best = days.maxByOrNull { it.steps }
    // 평균은 "걸은 날" 기준이다. 앱을 안 켠 날까지 나누면 실제보다 낮게 나오고,
    // 그러면 열심히 뛴 사람이 자기 기록을 못 믿게 된다.
    val dailyAvg = if (activeDays > 0) steps / activeDays else 0L

    GlowCard(contentPadding = PaddingValues(vertical = 18.dp, horizontal = 10.dp), spacing = 16.dp) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = stringResource(R.string.stat_steps),
                value = "%,d".format(steps),
            )
            StatCell(
                icon = Icons.Filled.LocationOn,
                label = stringResource(R.string.stat_distance),
                value = "%.0f km".format(steps * RewardEconomy.STRIDE_METERS / 1000),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.Filled.TrendingUp,
                label = stringResource(R.string.analytics_daily_avg),
                value = "%,d".format(dailyAvg),
            )
            StatCell(
                icon = Icons.Filled.LocalFireDepartment,
                label = stringResource(R.string.stat_calories),
                value = "%,.0f".format(steps * RewardEconomy.KCAL_PER_STEP),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.Filled.CheckCircle,
                label = stringResource(R.string.analytics_goal_days),
                value = stringResource(R.string.analytics_days_of, metDays, activeDays),
            )
            StatCell(
                icon = Icons.Filled.EmojiEvents,
                label = stringResource(R.string.analytics_best_day),
                value = "%,d".format(best?.steps ?: 0),
            )
        }
        if (best != null && best.steps > 0) {
            HairlineDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Eyebrow(text = stringResource(R.string.analytics_best_day))
                Spacer(Modifier.weight(1f))
                Text(
                    text = LocalDate.ofEpochDay(best.epochDay).format(calloutDateFormatter),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                )
            }
        }
    }
}

/** 최근 3개월을 달별로 */
@Composable
private fun MonthlyBreakdownCard(days: List<DailyStepsEntity>) {
    val byMonth = remember(days) {
        days.groupBy { LocalDate.ofEpochDay(it.epochDay).withDayOfMonth(1) }
            .toSortedMap(compareByDescending { it })
    }
    val maxSteps = byMonth.values.maxOfOrNull { list -> list.sumOf { it.steps.toLong() } } ?: 1L

    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 13.dp) {
        if (byMonth.isEmpty()) {
            Text(
                text = stringResource(R.string.analytics_no_data),
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
            )
        } else {
            byMonth.forEach { (month, entries) ->
                val steps = entries.sumOf { it.steps.toLong() }
                val km = steps * RewardEconomy.STRIDE_METERS / 1000
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = month.format(monthFormatter),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Snow,
                        )
                        Text(
                            text = "%,d".format(steps) + " · " + "%.0f km".format(km),
                            fontSize = 11.sp,
                            color = Silver,
                        )
                    }
                    BarMeter(
                        fraction = (steps.toFloat() / maxSteps).coerceIn(0f, 1f),
                        height = 6.dp,
                    )
                }
            }
        }
    }
}

/** 러닝 세션 통계 — 걸음 기록과 달리 "뛴 것"만 센다 */
@Composable
private fun RunStatsCard(sessions: List<WalkSessionEntity>) {
    val totalSec = sessions.sumOf { it.durationSec }
    val totalKm = sessions.sumOf { it.distanceMeters } / 1000.0
    val longest = sessions.maxOfOrNull { it.durationSec } ?: 0L
    // 페이스는 거리가 있어야 뜻이 있다. 0km 세션이 섞이면 평균이 무너진다.
    val pace = if (totalKm > 0.05) (totalSec / totalKm).toInt() else 0
    // 최고 속도가 아니라 평균 속도다. 구간 최고 속도는 서버가 GPS 경로에서
    // 재는 값이고, 폰에 저장된 세션에는 그 값이 없다. 없는 값을 있는 척
    // 그리는 대신 가진 것으로 낼 수 있는 값을 낸다.
    val avgSpeed = if (totalSec > 0) totalKm / (totalSec / 3600.0) else 0.0

    GlowCard(contentPadding = PaddingValues(vertical = 18.dp, horizontal = 10.dp), spacing = 16.dp) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = stringResource(R.string.analytics_run_count),
                value = stringResource(R.string.analytics_times, sessions.size),
            )
            StatCell(
                icon = Icons.Filled.TrendingUp,
                label = stringResource(R.string.analytics_total_time),
                value = formatDurationShort(totalSec),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.Filled.LocationOn,
                label = stringResource(R.string.analytics_avg_pace),
                value = if (pace > 0) "%d:%02d/km".format(pace / 60, pace % 60) else "—",
            )
            StatCell(
                icon = Icons.Filled.EmojiEvents,
                label = stringResource(R.string.analytics_avg_speed),
                value = if (avgSpeed > 0) "%.1f km/h".format(avgSpeed) else "—",
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                icon = Icons.Filled.CheckCircle,
                label = stringResource(R.string.analytics_longest_run),
                value = formatDurationShort(longest),
            )
            StatCell(
                icon = Icons.Filled.LocalFireDepartment,
                label = stringResource(R.string.stat_distance),
                value = "%.1f km".format(totalKm),
            )
        }
    }
}

/** 초 → "2시간 14분" · "38분" */
@Composable
private fun formatDurationShort(totalSec: Long): String {
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    return when {
        totalSec <= 0 -> "—"
        hours > 0 -> stringResource(R.string.analytics_hours_minutes, hours, minutes)
        else -> stringResource(R.string.analytics_minutes, minutes)
    }
}

private val monthDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M.d", Locale.getDefault())

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.M", Locale.getDefault())
