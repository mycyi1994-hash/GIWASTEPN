package com.giwa.strideup.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.local.DailyStepsEntity
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.domain.RunnerLevels
import com.giwa.strideup.domain.RunnerProgress
import com.giwa.strideup.domain.Sneaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val stepRepository: StepRepository,
    rewardRepository: RewardRepository,
    sneakerRepository: SneakerRepository,
    private val prefs: UserPrefs,
) : ViewModel() {

    data class UiState(
        val goal: Int = UserPrefs.DEFAULT_GOAL,
        val sneakerLevel: Int = 1,
        val balance: Double = 0.0,
        val streak: Int = 0,
        val lifetimeSteps: Long = 0,
        val monthSteps: Long = 0,
        val ownedSneakers: Int = 0,
        val runnerUid: String = "",
        val avatarId: Int = 0,
        val avatarRev: Int = 0,
        val equipped: Sneaker? = null,
        val week: List<DailyStepsEntity> = emptyList(),
        val totalDurationSec: Long = 0,
        val longestSessionSec: Long = 0,
        val avgPaceSecPerKm: Int = 0,
    ) {
        /** 러너 레벨 — 누적으로 걸은 거리가 곧 경험치다 */
        val runner: RunnerProgress get() = RunnerLevels.of(lifetimeKm)
        val multiplier: Double get() = RewardEconomy.sneakerMultiplier(sneakerLevel)
        val upgradeCost: Double get() = RewardEconomy.upgradeCost(sneakerLevel)
        val lifetimeKm: Double get() = lifetimeSteps * RewardEconomy.STRIDE_METERS / 1000
        val monthKm: Double get() = monthSteps * RewardEconomy.STRIDE_METERS / 1000
        val monthCalories: Double get() = monthSteps * RewardEconomy.KCAL_PER_STEP
        val lifetimeCalories: Double get() = lifetimeSteps * RewardEconomy.KCAL_PER_STEP
    }

    val uiState: StateFlow<UiState> = combine(
        combine(
            stepRepository.dailyGoal,
            rewardRepository.sneakerLevel,
            rewardRepository.balance,
        ) { goal, level, balance -> Triple(goal, level, balance) },
        combine(
            stepRepository.streak,
            stepRepository.observeLifetimeSteps(),
            stepRepository.observeMonthSteps(),
        ) { streak, lifetime, month -> Triple(streak, lifetime, month) },
        combine(
            sneakerRepository.ownedCount,
            stepRepository.observeWeek(),
        ) { owned, week -> owned to week },
        combine(
            stepRepository.observeTotalDurationSec(),
            stepRepository.recentSessions(30),
        ) { totalSec, sessions -> totalSec to sessions },
        combine(
            prefs.runnerUid,
            prefs.avatarId,
            prefs.avatarRev,
            sneakerRepository.equipped,
        ) { uid, avatar, rev, equipped -> Ident(uid, avatar, rev, equipped) },
    ) { (goal, level, balance), (streak, lifetime, month), (owned, week), (totalSec, sessions), ident ->
        // 최근 세션 기반 요약 — 세션 기록이 없으면 0으로 두고 화면에서 "—" 처리
        val sessionSec = sessions.sumOf { it.durationSec }
        val sessionKm = sessions.sumOf { it.distanceMeters } / 1000.0
        UiState(
            goal = goal,
            sneakerLevel = ident.equipped?.level ?: level,
            balance = balance,
            streak = streak,
            lifetimeSteps = lifetime,
            monthSteps = month,
            ownedSneakers = owned,
            runnerUid = ident.uid,
            avatarId = ident.avatarId,
            avatarRev = ident.avatarRev,
            equipped = ident.equipped,
            week = week,
            totalDurationSec = totalSec,
            longestSessionSec = sessions.maxOfOrNull { it.durationSec } ?: 0L,
            avgPaceSecPerKm = if (sessionKm > 0.01) (sessionSec / sessionKm).toInt() else 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setGoal(goal: Int) {
        viewModelScope.launch { stepRepository.setDailyGoal(goal) }
    }

    fun setAvatar(id: Int) {
        viewModelScope.launch { prefs.setAvatarId(id) }
    }

    /**
     * 갤러리에서 고른 사진을 아바타로 저장한다.
     * 내부 저장소에 512px 이하 JPEG로 축소 보관하고, 리비전을 올려 UI가 다시 읽게 한다.
     */
    fun setCustomAvatar(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = ServiceLocator.appContext
            val saved = runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    val raw = android.graphics.BitmapFactory.decodeStream(input)
                        ?: return@use false
                    val maxSide = 512f
                    val scale = minOf(maxSide / raw.width, maxSide / raw.height, 1f)
                    val bmp = if (scale < 1f) {
                        android.graphics.Bitmap.createScaledBitmap(
                            raw,
                            (raw.width * scale).toInt().coerceAtLeast(1),
                            (raw.height * scale).toInt().coerceAtLeast(1),
                            true,
                        )
                    } else {
                        raw
                    }
                    java.io.File(ctx.filesDir, UserPrefs.AVATAR_FILE).outputStream().use { out ->
                        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    true
                } ?: false
            }.getOrDefault(false)
            if (saved) {
                prefs.setAvatarId(UserPrefs.AVATAR_CUSTOM)
                prefs.bumpAvatarRev()
            }
        }
    }

    /** combine 5개 값 묶음 */
    private data class Ident(
        val uid: String,
        val avatarId: Int,
        val avatarRev: Int,
        val equipped: com.giwa.strideup.domain.Sneaker?,
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    ServiceLocator.stepRepository,
                    ServiceLocator.rewardRepository,
                    ServiceLocator.sneakerRepository,
                    ServiceLocator.userPrefs,
                )
            }
        }
    }
}
