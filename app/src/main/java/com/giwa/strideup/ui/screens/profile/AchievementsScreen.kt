package com.giwa.strideup.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.giwa.strideup.data.local.WalkSessionDao
import com.giwa.strideup.data.repo.CrewRepository
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 전체 업적 목록 — 실데이터(걸음·스트릭·잔액·스니커즈·크루·세션)로 잠금 해제를 판정한다. */
class AchievementsViewModel(
    stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    sneakerRepository: SneakerRepository,
    crewRepository: CrewRepository,
    walkSessionDao: WalkSessionDao,
) : ViewModel() {

    data class AchievementState(
        val titleRes: Int,
        val descRes: Int,
        val unlocked: Boolean,
        val progress: Float,
    )

    val achievements: StateFlow<List<AchievementState>> = combine(
        combine(
            stepRepository.observeLifetimeSteps(),
            stepRepository.streak,
            rewardRepository.balance,
        ) { lifetimeSteps, streak, balance -> Triple(lifetimeSteps, streak, balance) },
        combine(
            sneakerRepository.ownedCount,
            crewRepository.joinedCrewIds,
            walkSessionDao.observeSessionCount(),
        ) { ownedCount, joinedCrewIds, sessionCount -> Triple(ownedCount, joinedCrewIds, sessionCount) },
    ) { (lifetimeSteps, streak, balance), (ownedCount, joinedCrewIds, sessionCount) ->
        buildAchievements(
            lifetimeSteps = lifetimeSteps,
            streak = streak,
            balance = balance,
            ownedCount = ownedCount,
            joinedCrewIds = joinedCrewIds,
            sessionCount = sessionCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildAchievements(
        lifetimeSteps: Long,
        streak: Int,
        balance: Double,
        ownedCount: Int,
        joinedCrewIds: Set<String>,
        sessionCount: Int,
    ): List<AchievementState> {
        val km = lifetimeSteps * RewardEconomy.STRIDE_METERS / 1000.0
        return listOf(
            AchievementState(
                titleRes = R.string.ach_first_run,
                descRes = R.string.ach_first_run_desc,
                unlocked = sessionCount >= 1,
                progress = sessionCount.toFloat().coerceIn(0f, 1f),
            ),
            AchievementState(
                titleRes = R.string.ach_marathon,
                descRes = R.string.ach_marathon_desc,
                unlocked = km >= 100.0,
                progress = (km / 100.0).toFloat().coerceIn(0f, 1f),
            ),
            AchievementState(
                titleRes = R.string.ach_ultra,
                descRes = R.string.ach_ultra_desc,
                unlocked = km >= 500.0,
                progress = (km / 500.0).toFloat().coerceIn(0f, 1f),
            ),
            AchievementState(
                titleRes = R.string.ach_step_king,
                descRes = R.string.ach_step_king_desc,
                unlocked = lifetimeSteps >= 1_000_000L,
                progress = (lifetimeSteps.toFloat() / 1_000_000f).coerceIn(0f, 1f),
            ),
            AchievementState(
                titleRes = R.string.ach_streak_master,
                descRes = R.string.ach_streak_master_desc,
                unlocked = streak >= 30,
                progress = (streak.toFloat() / 30f).coerceIn(0f, 1f),
            ),
            AchievementState(
                titleRes = R.string.ach_collector,
                descRes = R.string.ach_collector_desc,
                unlocked = ownedCount >= 3,
                progress = (ownedCount.toFloat() / 3f).coerceIn(0f, 1f),
            ),
            AchievementState(
                titleRes = R.string.ach_party_animal,
                descRes = R.string.ach_party_animal_desc,
                unlocked = joinedCrewIds.isNotEmpty(),
                progress = if (joinedCrewIds.isEmpty()) 0f else 1f,
            ),
            AchievementState(
                titleRes = R.string.ach_rich,
                descRes = R.string.ach_rich_desc,
                unlocked = balance >= 1000.0,
                progress = (balance / 1000.0).toFloat().coerceIn(0f, 1f),
            ),
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                AchievementsViewModel(
                    stepRepository = ServiceLocator.stepRepository,
                    rewardRepository = ServiceLocator.rewardRepository,
                    sneakerRepository = ServiceLocator.sneakerRepository,
                    crewRepository = ServiceLocator.crewRepository,
                    walkSessionDao = ServiceLocator.database.walkSessionDao(),
                )
            }
        }
    }
}

@Composable
fun AchievementsScreen(
    onBack: () -> Unit = {},
    viewModel: AchievementsViewModel = viewModel(factory = AchievementsViewModel.Factory),
) {
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val unlockedCount = achievements.count { it.unlocked }

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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ach_all_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                    Text(
                        text = stringResource(R.string.ach_progress, unlockedCount, achievements.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                }
            }
        }

        items(achievements.chunked(2)) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                pair.forEach { achievement ->
                    AchievementTile(
                        achievement = achievement,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 업적 카드 — 헥사곤 배지 + 제목 + 설명, 잠금 상태면 진행 바 */
@Composable
private fun AchievementTile(
    achievement: AchievementsViewModel.AchievementState,
    modifier: Modifier = Modifier,
) {
    GlowCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        spacing = 10.dp,
        accent = achievement.unlocked,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            HexAchievementBadge(
                icon = achievementIcon(achievement.titleRes),
                unlocked = achievement.unlocked,
            )
            Text(
                text = stringResource(achievement.titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = if (achievement.unlocked) Snow else Slate,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(achievement.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = if (achievement.unlocked) Silver else Slate,
                textAlign = TextAlign.Center,
            )
            if (!achievement.unlocked) {
                Eyebrow(text = stringResource(R.string.ach_locked), color = Slate)
                BarMeter(
                    fraction = achievement.progress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 6.dp,
                )
            }
        }
    }
}

/** 6각형 배지 — 잠금 해제 시 볼트 스트로크 + 옅은 볼트 채움, 잠금 시 엣지 스트로크 + Lock */
@Composable
private fun HexAchievementBadge(
    icon: ImageVector,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 52.dp,
) {
    Box(modifier = modifier.size(badgeSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = min(size.width, size.height) / 2f * 0.94f
            val hex = Path().apply {
                for (i in 0 until 6) {
                    val angle = (-90f + i * 60f) * (PI / 180.0)
                    val x = cx + r * cos(angle).toFloat()
                    val y = cy + r * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(hex, color = if (unlocked) Volt.copy(alpha = 0.10f) else CarbonHigh)
            drawPath(
                path = hex,
                color = if (unlocked) Volt else Edge,
                style = Stroke(width = 1.6.dp.toPx()),
            )
        }
        Icon(
            imageVector = if (unlocked) icon else Icons.Filled.Lock,
            contentDescription = null,
            tint = if (unlocked) Volt else Slate,
            modifier = Modifier.size(badgeSize * 0.38f),
        )
    }
}

private fun achievementIcon(titleRes: Int): ImageVector = when (titleRes) {
    R.string.ach_first_run -> Icons.AutoMirrored.Filled.DirectionsWalk
    R.string.ach_marathon -> Icons.Filled.EmojiEvents
    R.string.ach_ultra -> Icons.Filled.MilitaryTech
    R.string.ach_step_king -> Icons.Filled.BarChart
    R.string.ach_streak_master -> Icons.Filled.Whatshot
    R.string.ach_collector -> Icons.Filled.CheckCircle
    R.string.ach_party_animal -> Icons.Filled.LocationOn
    else -> Icons.Filled.AccountBalanceWallet
}
