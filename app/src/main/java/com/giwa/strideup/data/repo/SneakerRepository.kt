package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.NotificationType
import com.giwa.strideup.data.local.RewardType
import com.giwa.strideup.data.local.SneakerDao
import com.giwa.strideup.domain.Faction
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.domain.Sneaker
import com.giwa.strideup.domain.SneakerMint
import com.giwa.strideup.domain.TOTAL_COLLECTION
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 스니커즈 NFT 인벤토리 — 민팅 · 착용 · 강화 */
class SneakerRepository(
    private val sneakerDao: SneakerDao,
    private val rewardRepository: RewardRepository,
) {

    val inventory: Flow<List<Sneaker>> =
        sneakerDao.observeAll().map { list -> list.map { it.toDomain() } }

    val equipped: Flow<Sneaker?> =
        sneakerDao.observeEquipped().map { it?.toDomain() }

    val ownedCount: Flow<Int> = sneakerDao.observeCount()

    /** 첫 실행 시 스타터 스니커즈를 지급한다. 이미 있으면 아무것도 하지 않는다. */
    suspend fun ensureStarter() {
        if (sneakerDao.count() > 0) return
        sneakerDao.insert(SneakerMint.starter().toEntity())
    }

    suspend fun equip(id: Long) {
        val target = sneakerDao.byId(id) ?: return
        sneakerDao.clearEquipped()
        sneakerDao.update(target.copy(equipped = true))
    }

    /** 강화. 잔액 부족이거나 최대 레벨이면 null. */
    suspend fun upgrade(id: Long): Sneaker? {
        val entity = sneakerDao.byId(id) ?: return null
        val current = entity.toDomain()
        if (!current.canUpgrade) return null
        val cost = current.upgradeCost
        val ok = rewardRepository.spend(
            RewardType.SPEND_UPGRADE,
            cost,
            "${current.displayName} Lv.${current.level} → Lv.${current.level + 1}",
        )
        if (!ok) return null
        val upgraded = entity.copy(
            level = entity.level + 1,
            // 강화하면 스탯도 소폭 상승한다
            comfort = entity.comfort + 0.02,
            luck = entity.luck + 0.02,
        )
        sneakerDao.update(upgraded)
        rewardRepository.notify(
            NotificationType.SNEAKER_UPGRADED,
            current.slotKey,
            (entity.level + 1).toDouble(),
        )
        return upgraded.toDomain()
    }

    /**
     * 새 스니커즈 민팅. 착용 중인 스니커즈의 행운 스탯이 상위 희귀도 확률을 올린다.
     * 잔액 부족이면 null.
     */
    suspend fun mint(): Sneaker? {
        val cost = RewardEconomy.MINT_COST
        val luck = sneakerDao.equippedNow()?.luck?.minus(1.0)?.coerceAtLeast(0.0) ?: 0.0
        if (!rewardRepository.spend(RewardType.SPEND_MINT, cost, "스니커즈 민팅")) return null

        val mintNumber = sneakerDao.maxMintNumber() + 1
        val minted = SneakerMint.mint(
            random = Random(System.nanoTime()),
            mintNumber = mintNumber,
            luck = luck,
        )
        val newId = sneakerDao.insert(minted.toEntity())
        rewardRepository.notify(
            NotificationType.SNEAKER_MINTED,
            minted.slotKey,
            minted.rarity.ordinal.toDouble(),
        )
        return minted.copy(id = newId)
    }

    /** 도감 진행도 — 보유한 (속성 × 등급 × 변형) 조합 수 */
    val collectionProgress: Flow<Pair<Int, Int>> = inventory.map { list ->
        list.map { it.slotKey }.toSet().size to TOTAL_COLLECTION
    }

    /** 속성별 수집 현황 — 도감 탭에서 "불 3/11" 처럼 쓴다 */
    val factionProgress: Flow<Map<Faction, Int>> = inventory.map { list ->
        Faction.entries.associateWith { f ->
            list.filter { it.faction == f }.map { it.slotKey }.toSet().size
        }
    }
}
