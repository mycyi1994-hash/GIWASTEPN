package com.giwa.strideup.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.LevelAvatar
import com.giwa.strideup.ui.components.ListRow
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.TokenCard
import com.giwa.strideup.ui.components.VerticalHairline
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.screens.home.runnerTier
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileScreen(
    onOpenWallet: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val comingSoon = stringResource(R.string.toast_coming_soon)
    val showComingSoon = { Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(fontSize = 22.sp)
                TokenCard(balance = state.balance, onClick = onOpenWallet)
            }
        }

        item { ProfileHeader(state) }

        item { StatsRow(state) }

        item { AchievementsCard(state, showComingSoon) }

        item { SneakersCard(state.sneakerLevel) }

        item { OverviewCard(state) }

        item { GoalCard(goal = state.goal, onGoalChange = viewModel::setGoal) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = stringResource(R.string.profile_account))
                ListRow(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = stringResource(R.string.settings_wallet),
                    onClick = onOpenWallet,
                )
                ListRow(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.settings_notifications),
                    onClick = showComingSoon,
                )
                ListRow(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.settings_privacy),
                    onClick = showComingSoon,
                )
                ListRow(
                    icon = Icons.Filled.SupportAgent,
                    title = stringResource(R.string.settings_support),
                    onClick = showComingSoon,
                )
                ListRow(
                    icon = Icons.Filled.Link,
                    title = stringResource(R.string.settings_connected),
                    onClick = showComingSoon,
                )
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 9.dp) {
                AboutRow(label = stringResource(R.string.about_version), value = "StrideUp 1.2.0")
                AboutRow(
                    label = stringResource(R.string.about_network),
                    value = stringResource(R.string.about_network_value),
                )
            }
        }
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
private fun ProfileHeader(state: ProfileViewModel.UiState) {
    GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            LevelAvatar(
                level = state.sneakerLevel,
                size = 72.dp,
                contentDescription = stringResource(R.string.cd_profile),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_hello),
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HexEmblem(size = 14.dp, glow = false)
                    Text(
                        text = runnerTier(state.sneakerLevel),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Volt,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.level_chip, state.sneakerLevel),
                style = MaterialTheme.typography.titleSmall,
                color = Volt,
            )
            Text(
                text = stringResource(
                    R.string.profile_next_level,
                    "%,.0f".format(state.balance.coerceAtMost(state.upgradeCost)),
                    "%,.0f".format(state.upgradeCost),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
            )
        }
        BarMeter(
            fraction = if (state.upgradeCost > 0) {
                (state.balance / state.upgradeCost).coerceIn(0.0, 1.0).toFloat()
            } else {
                0f
            },
        )
    }
}

@Composable
private fun StatsRow(state: ProfileViewModel.UiState) {
    val unlocked = achievementStates(state).count { it }
    GlowCard(contentPadding = PaddingValues(vertical = 17.dp, horizontal = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniStatCell(
                icon = Icons.Filled.LocationOn,
                label = stringResource(R.string.profile_total_distance),
                value = "%.2f km".format(state.lifetimeKm),
            )
            VerticalHairline(height = 44.dp)
            MiniStatCell(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = stringResource(R.string.profile_lifetime_steps),
                value = "%,d".format(state.lifetimeSteps),
            )
            VerticalHairline(height = 44.dp)
            MiniStatCell(
                icon = Icons.Filled.Whatshot,
                label = stringResource(R.string.profile_current_streak),
                value = stringResource(R.string.days_count, state.streak),
            )
            VerticalHairline(height = 44.dp)
            MiniStatCell(
                icon = Icons.Filled.EmojiEvents,
                label = stringResource(R.string.profile_achievements),
                value = "$unlocked / 4",
            )
        }
    }
}

@Composable
private fun RowScope.MiniStatCell(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Volt, modifier = Modifier.size(17.dp))
        Text(
            text = label,
            fontSize = 9.5.sp,
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
    }
}

/** 실데이터 기반 업적 해금 여부: 마라톤 / 스텝 킹 / 스트릭 마스터 / 울트라(잠김) */
private fun achievementStates(state: ProfileViewModel.UiState): List<Boolean> = listOf(
    state.lifetimeKm >= 100.0,
    state.lifetimeSteps >= 1_000_000L,
    state.streak >= 30,
    false,
)

@Composable
private fun AchievementsCard(state: ProfileViewModel.UiState, onViewAll: () -> Unit) {
    val states = achievementStates(state)
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 14.dp) {
        SectionHeader(
            title = stringResource(R.string.profile_achievements),
            actionText = stringResource(R.string.common_view_all),
            onAction = onViewAll,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            AchBadge(
                name = stringResource(R.string.ach_marathon),
                desc = stringResource(R.string.ach_marathon_desc),
                unlocked = states[0],
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                modifier = Modifier.weight(1f),
            )
            AchBadge(
                name = stringResource(R.string.ach_step_king),
                desc = stringResource(R.string.ach_step_king_desc),
                unlocked = states[1],
                icon = Icons.Filled.MilitaryTech,
                modifier = Modifier.weight(1f),
            )
            AchBadge(
                name = stringResource(R.string.ach_streak_master),
                desc = stringResource(R.string.ach_streak_master_desc),
                unlocked = states[2],
                icon = Icons.Filled.Whatshot,
                modifier = Modifier.weight(1f),
            )
            AchBadge(
                name = stringResource(R.string.ach_ultra),
                desc = stringResource(R.string.ach_locked),
                unlocked = states[3],
                icon = Icons.Filled.Lock,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AchBadge(
    name: String,
    desc: String,
    unlocked: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val tint = if (unlocked) Volt else Slate
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension / 2f * 0.92f
                val path = Path().apply {
                    for (i in 0 until 6) {
                        val a = (-90f + i * 60f) * (PI / 180.0)
                        val x = cx + r * cos(a).toFloat()
                        val y = cy + r * sin(a).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path, color = if (unlocked) Volt.copy(alpha = 0.10f) else Color.Transparent)
                drawPath(
                    path,
                    color = if (unlocked) Volt else Edge,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(
            text = name,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (unlocked) Snow else Slate,
            textAlign = TextAlign.Center,
        )
        Text(text = desc, fontSize = 9.sp, color = Slate, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SneakersCard(level: Int) {
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        SectionHeader(title = stringResource(R.string.profile_my_sneakers))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                HexEmblem(size = 54.dp)
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = Snow,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Apex Runner", style = MaterialTheme.typography.titleSmall, color = Snow)
                Text(
                    text = stringResource(R.string.level_chip, level) + " · " + runnerTier(level),
                    fontSize = 11.sp,
                    color = Silver,
                )
            }
            Text(
                text = stringResource(R.string.profile_owned_count, 1),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
        }
    }
}

@Composable
private fun OverviewCard(state: ProfileViewModel.UiState) {
    val tokensEarned = state.monthSteps * RewardEconomy.POINTS_PER_STEP * state.multiplier
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 13.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.profile_overview),
                    style = MaterialTheme.typography.titleMedium,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.profile_overview_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Volt.copy(alpha = 0.10f))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_this_month),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Volt,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OverviewStat(
                label = stringResource(R.string.stat_steps),
                value = "%,d".format(state.monthSteps),
            )
            OverviewStat(
                label = stringResource(R.string.stat_distance),
                value = "%.2f km".format(state.monthKm),
            )
            OverviewStat(
                label = stringResource(R.string.stat_calories),
                value = "%,.0f".format(state.monthCalories),
            )
            OverviewStat(
                label = stringResource(R.string.stat_tokens_earned),
                value = "%,.1f".format(tokensEarned),
                volt = true,
            )
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: String, volt: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 10.sp, color = Slate)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (volt) Volt else Snow,
        )
    }
}

@Composable
private fun GoalCard(goal: Int, onGoalChange: (Int) -> Unit) {
    var sliderValue by remember(goal) { mutableFloatStateOf(goal.toFloat()) }
    val steps = sliderValue.toInt()
    val distanceKm = RewardEconomy.distanceMeters(steps) / 1000

    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 11.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = stringResource(R.string.goal_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Snow,
                )
            }
            Text(
                text = stringResource(R.string.goal_about_km, "%.1f".format(distanceKm)),
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
            )
        }
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
}
