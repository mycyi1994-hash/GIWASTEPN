package com.giwa.strideup.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.giwa.strideup.ui.components.AvatarEmojis
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.LevelAvatar
import com.giwa.strideup.ui.components.ListRow
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.TokenCard
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.rememberCustomAvatar
import com.giwa.strideup.ui.components.VerticalHairline
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.guide.GuideTour
import com.giwa.strideup.ui.guide.guideTarget
import com.giwa.strideup.ui.screens.home.runnerTier
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

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
    onOpenItems: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAvatarPicker by rememberSaveable { mutableStateOf(false) }

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

        item {
            Box(Modifier.guideTarget(GuideTour.Targets.PROFILE_AVATAR)) {
                ProfileHeader(state, onEditAvatar = { showAvatarPicker = true })
            }
        }

        item { StatsRow(state) }

        item {
            Box(Modifier.guideTarget(GuideTour.Targets.PROFILE_ACHIEVEMENTS)) {
                AchievementsCard(state, onOpenAchievements)
            }
        }

        item {
            SneakersCard(
                level = state.sneakerLevel,
                owned = state.ownedSneakers,
                equippedName = state.equippedName,
                onOpenItems = onOpenItems,
            )
        }

        item { OverviewCard(state, onOpenAnalytics) }

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
                    onClick = onOpenNotificationSettings,
                )
                ListRow(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.settings_privacy),
                    onClick = onOpenPrivacy,
                )
                ListRow(
                    icon = Icons.Filled.SupportAgent,
                    title = stringResource(R.string.settings_support),
                    onClick = onOpenSupport,
                )
                ListRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = stringResource(R.string.settings_guide),
                    onClick = onOpenGuide,
                )
                ListRow(
                    icon = Icons.Filled.Link,
                    title = stringResource(R.string.settings_connected),
                    onClick = onOpenConnected,
                )
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 9.dp) {
                AboutRow(label = stringResource(R.string.about_version), value = "StepUp 1.7.0")
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
private fun ProfileHeader(
    state: ProfileViewModel.UiState,
    onEditAvatar: () -> Unit,
) {
    GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Box {
                LevelAvatar(
                    level = state.sneakerLevel,
                    size = 72.dp,
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
                        )
                    }
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

/** 실데이터 기반 업적 미리보기: 마라톤 / 스텝 킹 / 스트릭 / 컬렉터 */
private fun previewAchievements(state: ProfileViewModel.UiState): List<Boolean> = listOf(
    state.lifetimeKm >= 100.0,
    state.lifetimeSteps >= 1_000_000L,
    state.streak >= 30,
    state.ownedSneakers >= 3,
)

@Composable
private fun StatsRow(state: ProfileViewModel.UiState) {
    val unlocked = previewAchievements(state).count { it }
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
                value = "$unlocked / 8",
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

@Composable
private fun AchievementsCard(state: ProfileViewModel.UiState, onViewAll: () -> Unit) {
    val states = previewAchievements(state)
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
            AchPreview(
                name = stringResource(R.string.ach_marathon),
                unlocked = states[0],
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                modifier = Modifier.weight(1f),
            )
            AchPreview(
                name = stringResource(R.string.ach_step_king),
                unlocked = states[1],
                icon = Icons.Filled.MilitaryTech,
                modifier = Modifier.weight(1f),
            )
            AchPreview(
                name = stringResource(R.string.ach_streak_master),
                unlocked = states[2],
                icon = Icons.Filled.Whatshot,
                modifier = Modifier.weight(1f),
            )
            AchPreview(
                name = stringResource(R.string.ach_collector),
                unlocked = states[3],
                icon = Icons.Filled.TrendingUp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AchPreview(
    name: String,
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
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    tint.copy(alpha = if (unlocked) 0.13f else 0.06f),
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(
            text = name,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (unlocked) Snow else Slate,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SneakersCard(
    level: Int,
    owned: Int,
    equippedName: String,
    onOpenItems: () -> Unit,
) {
    GlowCard(
        modifier = Modifier.quietClickable(onOpenItems),
        contentPadding = PaddingValues(18.dp),
        spacing = 12.dp,
    ) {
        SectionHeader(
            title = stringResource(R.string.profile_my_sneakers),
            actionText = stringResource(R.string.common_view_all),
            onAction = onOpenItems,
        )
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
                Text(
                    text = equippedName.ifBlank { stringResource(R.string.common_none) },
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.level_chip, level) + " · " + runnerTier(level),
                    fontSize = 11.sp,
                    color = Silver,
                )
            }
            Text(
                text = stringResource(R.string.profile_owned_count, owned),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
        }
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

@Composable
private fun OverviewCard(state: ProfileViewModel.UiState, onOpenAnalytics: () -> Unit) {
    val tokensEarned = state.monthSteps * RewardEconomy.POINTS_PER_STEP * state.multiplier
    GlowCard(
        modifier = Modifier.quietClickable(onOpenAnalytics),
        contentPadding = PaddingValues(18.dp),
        spacing = 13.dp,
    ) {
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
                    .background(Volt.copy(alpha = 0.10f), RoundedCornerShape(50))
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
