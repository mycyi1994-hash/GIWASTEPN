package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.BoostDao
import com.giwa.strideup.data.local.BoostEntity
import com.giwa.strideup.data.local.NotificationType
import com.giwa.strideup.data.local.RewardType
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.domain.BoostType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ActiveBoost(val type: BoostType, val expiresAt: Long)

/** 부스트 구매 및 효과 적용 */
class BoostRepository(
    private val boostDao: BoostDao,
    private val rewardRepository: RewardRepository,
    private val prefs: UserPrefs,
) {

    /**
     * 지속형 활성 부스트. 만료 시각이 지나면 자동으로 목록에서 빠진다.
     * (조회 시점의 now를 쓰므로 화면이 재구독될 때 갱신된다.)
     */
    val active: Flow<List<ActiveBoost>> =
        boostDao.observeActive(System.currentTimeMillis()).map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.expiresAt > now }
                .map { ActiveBoost(BoostType.of(it.type), it.expiresAt) }
        }

    suspend fun isActive(type: BoostType): Boolean =
        boostDao.activeOf(type.id, System.currentTimeMillis()) != null

    /**
     * 부스트 구매. 잔액이 부족하거나 이미 같은 지속형 부스트가 활성이면 실패한다.
     *
     * @return 실패 사유. null이면 성공.
     */
    suspend fun purchase(type: BoostType): PurchaseError? {
        val now = System.currentTimeMillis()
        if (!type.isInstant && boostDao.activeOf(type.id, now) != null) {
            return PurchaseError.ALREADY_ACTIVE
        }
        if (!rewardRepository.spend(RewardType.SPEND_BOOST, type.cost, "부스트 구매: ${type.id}")) {
            return PurchaseError.NOT_ENOUGH_BALANCE
        }

        when (type) {
            BoostType.ENERGY_CELL -> {
                // 즉시형 — 에너지 2칸 회복
                prefs.restoreEnergy(LocalDate.now().toEpochDay(), 2.0)
                boostDao.insert(BoostEntity(type = type.id, activatedAt = now, expiresAt = now))
            }
            else -> {
                boostDao.insert(
                    BoostEntity(
                        type = type.id,
                        activatedAt = now,
                        expiresAt = now + type.durationMillis,
                    )
                )
            }
        }
        rewardRepository.notify(NotificationType.BOOST_ACTIVATED, type.id, type.cost)
        return null
    }

    suspend fun purgeExpired() {
        boostDao.purgeExpired(System.currentTimeMillis())
    }
}

enum class PurchaseError { NOT_ENOUGH_BALANCE, ALREADY_ACTIVE }
