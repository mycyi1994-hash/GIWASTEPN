package com.giwa.strideup.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.BuildConfig
import com.giwa.strideup.R
import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.domain.runnerTitle
import com.giwa.strideup.ui.components.AvatarEmojis
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.LevelAvatar
import com.giwa.strideup.ui.components.NeonRing
import com.giwa.strideup.ui.components.PillChip
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.TokenCard
import com.giwa.strideup.ui.components.VerticalHairline
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.label
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.components.rememberCustomAvatar
import com.giwa.strideup.ui.guide.GuideTour
import com.giwa.strideup.ui.guide.guideTarget
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProfileScreen(
    onOpenGuide: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenConnected: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenItems: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAvatarPicker by rememberSaveable { mutableStateOf(false) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }

    // 갤러리 사진 선택 — 시스템 포토 피커 (권한 불필요)
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setCustomAvatar(uri)
            showAvatarPicker = false
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            selected = state.avatarId,
            onPick = { id ->
                viewModel.setAvatar(id)
                showAvatarPicker = false
            },
            onPickGallery = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showAvatarPicker = false },
        )
    }

    if (showGoalDialog) {
        GoalDialog(
            goal = state.goal,
            onGoalChange = viewModel::setGoal,
            onDismiss = { showGoalDialog = false },
        )
    }

    val pills = listOf(
        SettingsPill(Icons.Filled.Edit, R.string.profile_edit_profile) { showAvatarPicker = true },
        SettingsPill(Icons.Filled.Flag, R.string.profile_set_goal) { showGoalDialog = true },
        SettingsPill(Icons.Filled.Notifications, R.string.settings_notifications, onOpenNotificationSettings),
        SettingsPill(Icons.Filled.Link, R.string.settings_connected, onOpenConnected),
        SettingsPill(Icons.Filled.SupportAgent, R.string.settings_support, onOpenSupport),
        SettingsPill(Icons.Filled.Language, R.string.settings_language, onOpenLanguage),
        SettingsPill(Icons.Filled.AccountBalanceWallet, R.string.settings_wallet, onOpenWallet),
        SettingsPill(Icons.AutoMirrored.Filled.DirectionsWalk, R.string.profile_my_sneakers, onOpenItems),
        SettingsPill(Icons.AutoMirrored.Filled.MenuBook, R.string.settings_guide, onOpenGuide),
        SettingsPill(Icons.Filled.Security, R.string.settings_privacy, onOpenPrivacy),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            Box(Modifier.guideTarget(GuideTour.Targets.PROFILE_AVATAR)) {
                ProfileHeader(
                    state = state,
                    onEditAvatar = { showAvatarPicker = true },
                    onOpenWallet = onOpenWallet,
                )
            }
        }

        item { RecordsCard(state, onOpenAnalytics) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DistanceCard(state, onOpenAnalytics)
                StreakCard(state)
            }
        }

        item { RecentSummaryCard(state, onOpenAnalytics) }

        item {
            Box(Modifier.guideTarget(GuideTour.Targets.PROFILE_ACHIEVEMENTS)) {
                BadgesCard(state, onOpenAchievements)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = stringResource(R.string.profile_account))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pills) { pill ->
                        PillChip(
                            text = stringResource(pill.label),
                            selected = false,
                            onClick = pill.onClick,
                            icon = pill.icon,
                        )
                    }
                }
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 9.dp) {
                AboutRow(
                    label = stringResource(R.string.about_version),
                    value = "StepUp " + BuildConfig.VERSION_NAME,
                )
                AboutRow(
                    label = stringResource(R.string.about_network),
                    value = stringResource(R.string.about_network_value),
                )
            }
        }
    }
}

/** 설정 필 칩 하나 — 아이콘 + 라벨 + 탭 액션 */
private data class SettingsPill(
    val icon: ImageVector,
    val label: Int,
    val onClick: () -> Unit,
)

/** 초 → H:MM:SS */
private fun formatDuration(totalSec: Long): String {
    val h = totalSec / 3600
    val m = totalSec % 3600 / 60
    val s = totalSec % 60
    return "%d:%02d:%02d".format(h, m, s)
}

/** 최근 7일 슬롯(오래된 날 → 오늘). DB에 없는 날은 null */
private fun weekSlots(week: List<DailyStepsEntity>): List<Pair<LocalDate, DailyStepsEntity?>> {
    val byDay = week.associateBy { it.epochDay }
    val today = LocalDate.now()
    return (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        date to byDay[date.toEpochDay()]
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Silver)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Snow)
    }
}

@Composable
private fun ProfileHeader(
    state: ProfileViewModel.UiState,
    onEditAvatar: () -> Unit,
    onOpenWallet: () -> Unit,
) {
    GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box {
                LevelAvatar(
                    level = state.runner.level,
                    size = 62.dp,
                    contentDescription = stringResource(R.string.cd_profile),
                    avatarId = state.avatarId,
                    customBitmap = rememberCustomAvatar(state.avatarRev),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(CarbonHigh)
                        .border(1.dp, Volt.copy(alpha = 0.6f), CircleShape)
                        .quietClickable(onEditAvatar),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.profile_edit_avatar),
                        tint = Volt,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_hello),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
                // 좁은 화면에서도 한 줄로 읽히게 — 줄바꿈을 막고 넘치면 줄임표
                Text(
                    text = stringResource(R.string.greeting_runner),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Snow,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HexEmblem(size = 14.dp, glow = false)
                    Text(
                        text = stringResource(R.string.level_chip, state.runner.level) +
                            " · " + runnerTitle(state.runner.level).label(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Volt,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.runnerUid.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CarbonHigh)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_uid, state.runnerUid),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = Slate,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        // 토큰 카드는 아래 한 줄을 통째로 쓴다 — 인사말 칸을 좁혀 글자가
        // 세로로 접히던 문제를 없앤다.
        TokenCard(
            balance = state.balance,
            onClick = onOpenWallet,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.level_chip, state.runner.level),
                style = MaterialTheme.typography.titleSmall,
                color = Volt,
            )
            Text(
                text = if (state.runner.isMax) {
                    stringResource(R.string.profile_level_max)
                } else {
                    stringResource(
                        R.string.profile_level_progress,
                        "%.1f".format(state.runner.intoLevelKm),
                        "%.0f".format(state.runner.levelSpanKm),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
            )
        }
        BarMeter(fraction = state.runner.progress)
        Text(
            text = if (state.runner.isMax) {
                stringResource(R.string.profile_level_hint_max)
            } else {
                stringResource(R.string.profile_level_hint, "%.1f".format(state.runner.remainingKm))
            },
            fontSize = 10.sp,
            color = Slate,
        )
    }
}

/** 실데이터 기반 업적 미리보기: 마라톤 / 스텝 킹 / 스트릭 / 컬렉터 */
private fun previewAchievements(state: ProfileViewModel.UiState): List<Boolean> = listOf(
    state.lifetimeKm >= 100.0,
    state.lifetimeSteps >= 1_000_000L,
    state.streak >= 30,
    state.ownedSneakers >= 3,
)

/** 나의 기록 — 누적 걸음/거리/칼로리/운동시간 4열 */
@Composable
private fun RecordsCard(state: ProfileViewModel.UiState, onOpenAnalytics: () -> Unit) {
    GlowCard(
        modifier = Modifier.quietClickable(onOpenAnalytics),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
        spacing = 13.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.profile_my_records),
                style = MaterialTheme.typography.titleMedium,
                color = Snow,
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(18.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordCell(
                label = stringResource(R.string.profile_lifetime_steps),
                value = "%,d".format(state.lifetimeSteps),
                unit = stringResource(R.string.stat_steps),
            )
            VerticalHairline(height = 44.dp)
            RecordCell(
                label = stringResource(R.string.profile_total_distance),
                value = "%.2f".format(state.lifetimeKm),
                unit = "km",
            )
            VerticalHairline(height = 44.dp)
            RecordCell(
                label = stringResource(R.string.profile_total_calories),
                value = "%,.0f".format(state.lifetimeCalories),
                unit = "kcal",
            )
            VerticalHairline(height = 44.dp)
            RecordCell(
                label = stringResource(R.string.profile_total_time),
                value = if (state.totalDurationSec > 0) formatDuration(state.totalDurationSec) else "—",
            )
        }
    }
}

@Composable
private fun RowScope.RecordCell(
    label: String,
    value: String,
    unit: String? = null,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = Slate,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Snow,
            textAlign = TextAlign.Center,
        )
        if (unit != null) {
            Text(
                text = unit,
                fontSize = 8.5.sp,
                color = Slate,
            )
        }
    }
}

/** 누적 거리 카드 — 지구 한 바퀴 진행률 + 미니 행성 */
@Composable
private fun RowScope.DistanceCard(state: ProfileViewModel.UiState, onOpenAnalytics: () -> Unit) {
    val earthPercent = state.lifetimeKm / 40075.0 * 100
    GlowCard(
        modifier = Modifier
            .weight(1f)
            .height(210.dp)
            .quietClickable(onOpenAnalytics),
        contentPadding = PaddingValues(16.dp),
        spacing = 7.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.profile_total_distance),
                fontSize = 11.sp,
                color = Silver,
            )
        }
        Text(
            text = "%.2f km".format(state.lifetimeKm),
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = Volt,
        )
        Text(
            text = stringResource(R.string.profile_earth, "%.3f".format(earthPercent)),
            fontSize = 9.sp,
            lineHeight = 13.sp,
            color = Slate,
        )
        Spacer(Modifier.weight(1f))
        // 미니 행성 — 방사형 볼트 그라데이션 원 + 점선 궤도 + 위성 점
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(70.dp),
        ) {
            val r = size.minDimension / 2f * 0.58f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Volt.copy(alpha = 0.95f),
                        Volt.copy(alpha = 0.30f),
                        Color.Transparent,
                    ),
                    center = Offset(center.x - r * 0.3f, center.y - r * 0.3f),
                    radius = r * 1.7f,
                ),
                radius = r,
                center = center,
            )
            drawOval(
                color = Volt.copy(alpha = 0.45f),
                topLeft = Offset(0f, center.y - r * 0.55f),
                size = Size(size.width, r * 1.1f),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                ),
            )
            drawCircle(
                color = Snow,
                radius = 2.dp.toPx(),
                center = Offset(center.x + size.width * 0.37f, center.y + r * 0.33f),
            )
        }
    }
}

/** 연속 활동 카드 — 스트릭 일수 + 최근 7일 목표 달성 도트 */
@Composable
private fun RowScope.StreakCard(state: ProfileViewModel.UiState) {
    GlowCard(
        modifier = Modifier
            .weight(1f)
            .height(210.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
        spacing = 7.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.profile_streak_title),
                fontSize = 11.sp,
                color = Silver,
            )
        }
        Text(
            text = stringResource(R.string.days_count, state.streak),
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = Snow,
        )
        Text(
            text = stringResource(R.string.profile_streak_great),
            fontSize = 9.sp,
            lineHeight = 13.sp,
            color = Slate,
        )
        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth()) {
            weekSlots(state.week).forEachIndexed { index, (date, day) ->
                val met = day != null && day.steps >= day.goal
                val isToday = index == 6
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (met) Volt else CarbonHigh)
                            .then(
                                // 오늘 도트는 볼트 링으로 강조
                                if (isToday) Modifier.border(1.5.dp, Volt, CircleShape) else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (met) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Night,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        fontSize = 8.5.sp,
                        color = if (isToday) Volt else Slate,
                    )
                }
            }
        }
    }
}

/** 최근 활동 요약 — 주간 링 + 최고 기록 + 미니 바 그래프 */
@Composable
private fun RecentSummaryCard(state: ProfileViewModel.UiState, onOpenAnalytics: () -> Unit) {
    val activeDays = state.week.count { it.steps > 0 }
    val bestDaySteps = state.week.maxOfOrNull { it.steps } ?: 0
    val bestDayKm = bestDaySteps * RewardEconomy.STRIDE_METERS / 1000
    val pace = if (state.avgPaceSecPerKm > 0) {
        "%d'%02d\"".format(state.avgPaceSecPerKm / 60, state.avgPaceSecPerKm % 60)
    } else {
        "—"
    }
    GlowCard(
        modifier = Modifier.quietClickable(onOpenAnalytics),
        contentPadding = PaddingValues(18.dp),
        spacing = 14.dp,
    ) {
        SectionHeader(
            title = stringResource(R.string.profile_recent_summary),
            actionText = stringResource(R.string.common_view_all),
            onAction = onOpenAnalytics,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeonRing(
                progress = activeDays / 7f,
                modifier = Modifier.size(116.dp),
                ringWidth = 10.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_week_runs),
                        fontSize = 9.sp,
                        color = Slate,
                    )
                    Text(
                        text = stringResource(R.string.profile_times_unit, activeDays),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Snow,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SummaryStat(
                    label = stringResource(R.string.profile_longest_distance),
                    value = "%.2f km".format(bestDayKm),
                )
                SummaryStat(
                    label = stringResource(R.string.profile_longest_time),
                    value = if (state.longestSessionSec > 0) formatDuration(state.longestSessionSec) else "—",
                )
                SummaryStat(
                    label = stringResource(R.string.profile_avg_pace),
                    value = pace,
                )
            }
            // 주간 미니 바 — 걸음 수 비례, 오늘만 볼트
            val maxSteps = bestDaySteps.coerceAtLeast(1)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                weekSlots(state.week).forEachIndexed { index, (_, day) ->
                    val frac = (day?.steps ?: 0).toFloat() / maxSteps
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(6.dp + 40.dp * frac)
                            .clip(RoundedCornerShape(50))
                            .background(if (index == 6) Volt else Slate.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, fontSize = 9.sp, color = Slate)
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Snow,
        )
    }
}

/** 러너 배지 — 육각 엠블럼 6칸 (해금 4 + 잠금 2), 탭하면 업적 전체 */
@Composable
private fun BadgesCard(state: ProfileViewModel.UiState, onOpenAchievements: () -> Unit) {
    val states = previewAchievements(state)
    val unlocked = states.count { it }
    GlowCard(
        modifier = Modifier.quietClickable(onOpenAchievements),
        contentPadding = PaddingValues(18.dp),
        spacing = 14.dp,
    ) {
        SectionHeader(
            title = stringResource(R.string.profile_badges),
            actionText = "$unlocked / 100",
            onAction = onOpenAchievements,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BadgeCell(
                name = stringResource(R.string.ach_marathon),
                unlocked = states[0],
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            )
            BadgeCell(
                name = stringResource(R.string.ach_step_king),
                unlocked = states[1],
                icon = Icons.Filled.MilitaryTech,
            )
            BadgeCell(
                name = stringResource(R.string.ach_streak_master),
                unlocked = states[2],
                icon = Icons.Filled.Whatshot,
            )
            BadgeCell(
                name = stringResource(R.string.ach_collector),
                unlocked = states[3],
                icon = Icons.Filled.TrendingUp,
            )
            BadgeCell(
                name = stringResource(R.string.ach_locked),
                unlocked = false,
                icon = Icons.Filled.Lock,
            )
            BadgeCell(
                name = stringResource(R.string.ach_locked),
                unlocked = false,
                icon = Icons.Filled.Lock,
            )
        }
    }
}

@Composable
private fun RowScope.BadgeCell(
    name: String,
    unlocked: Boolean,
    icon: ImageVector,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (unlocked) {
                HexEmblem(size = 42.dp)
                Icon(icon, contentDescription = null, tint = Snow, modifier = Modifier.size(16.dp))
            } else {
                HexEmblem(modifier = Modifier.alpha(0.25f), size = 42.dp, glow = false)
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Slate,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = name,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (unlocked) Snow else Slate,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 아바타 선택 — 이모지 4×4 그리드 */
@Composable
private fun AvatarPickerDialog(
    selected: Int,
    onPick: (Int) -> Unit,
    onPickGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_close),
                    color = Volt,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.profile_edit_avatar),
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VoltButton(
                    text = stringResource(R.string.profile_avatar_gallery),
                    onClick = onPickGallery,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.profile_avatar_or_emoji),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
                AvatarEmojis.chunked(4).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEachIndexed { colIndex, emoji ->
                            val id = rowIndex * 4 + colIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(CarbonHigh)
                                    .border(
                                        width = if (selected == id) 2.dp else 1.dp,
                                        color = if (selected == id) Volt else Edge,
                                        shape = CircleShape,
                                    )
                                    .quietClickable { onPick(id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        },
    )
}

/** 목표 설정 다이얼로그 — 기존 목표 카드의 슬라이더 UI를 그대로 품는다 */
@Composable
private fun GoalDialog(
    goal: Int,
    onGoalChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var sliderValue by remember(goal) { mutableFloatStateOf(goal.toFloat()) }
    val steps = sliderValue.toInt()
    val distanceKm = RewardEconomy.distanceMeters(steps) / 1000

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_close),
                    color = Volt,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.goal_title),
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%,d".format(steps),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                            color = Snow,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.goal_steps_suffix),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.goal_about_km, "%.1f".format(distanceKm)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onGoalChange(sliderValue.toInt()) },
                    valueRange = UserPrefs.MIN_GOAL.toFloat()..UserPrefs.MAX_GOAL.toFloat(),
                    steps = (UserPrefs.MAX_GOAL - UserPrefs.MIN_GOAL) / 500 - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = Volt,
                        activeTrackColor = Volt,
                        inactiveTrackColor = Color.White.copy(alpha = 0.08f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                )
                Text(
                    text = stringResource(R.string.goal_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
        },
    )
}
