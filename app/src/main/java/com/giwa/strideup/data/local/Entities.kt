package com.giwa.strideup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 일별 걸음 수 기록 (epochDay = LocalDate.toEpochDay) */
@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val epochDay: Long,
    val steps: Int,
    val goal: Int,
    val updatedAt: Long,
)

/** 워킹 세션 기록 */
@Entity(tableName = "walk_sessions")
data class WalkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val steps: Int,
    val durationSec: Long,
    val distanceMeters: Double,
    val calories: Double,
    val pointsEarned: Double,
)

/** SUP 포인트 적립/사용 원장. amount 양수 = 적립, 음수 = 사용 */
@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val amount: Double,
    val description: String,
)

object RewardType {
    const val EARN_WALK = "EARN_WALK"
    const val BONUS_GOAL = "BONUS_GOAL"
    const val SPEND_UPGRADE = "SPEND_UPGRADE"
    const val SPEND_MINT = "SPEND_MINT"
    const val SPEND_BOOST = "SPEND_BOOST"
    const val EARN_EVENT = "EARN_EVENT"
    const val EARN_PARTY = "EARN_PARTY"
}

/** 보유 스니커즈 NFT */
@Entity(tableName = "sneakers")
data class SneakerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: String,
    val colorwayId: String,
    val rarity: String,
    val level: Int,
    val mintNumber: Int,
    val efficiency: Double,
    val luck: Double,
    val comfort: Double,
    val durability: Int,
    val equipped: Boolean,
    val acquiredAt: Long,
)

/** 구매한 부스트. 즉시형은 만들자마자 consumed=true */
@Entity(tableName = "boosts")
data class BoostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val activatedAt: Long,
    val expiresAt: Long,
)

/** 수령 완료한 이벤트 보상 */
@Entity(tableName = "claimed_events")
data class ClaimedEventEntity(
    @PrimaryKey val eventId: String,
    val claimedAt: Long,
    val amount: Double,
)

/** 가입한 크루 */
@Entity(tableName = "crew_memberships")
data class CrewMembershipEntity(
    @PrimaryKey val crewId: String,
    val joinedAt: Long,
)

/** 앱 내 알림. 본문은 type + 인자로 표시 시점에 현지화한다. */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val argText: String,
    val argAmount: Double,
    val read: Boolean,
)

object NotificationType {
    const val REWARD_EARNED = "REWARD_EARNED"
    const val GOAL_REACHED = "GOAL_REACHED"
    const val SNEAKER_MINTED = "SNEAKER_MINTED"
    const val SNEAKER_UPGRADED = "SNEAKER_UPGRADED"
    const val BOOST_ACTIVATED = "BOOST_ACTIVATED"
    const val CREW_JOINED = "CREW_JOINED"
    const val PARTY_FINISHED = "PARTY_FINISHED"
    const val EVENT_CLAIMED = "EVENT_CLAIMED"
}
