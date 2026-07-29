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

/** 보유 스니커즈 NFT — 속성(Faction) × 등급(Rarity) × 변형(variant) */
@Entity(tableName = "sneakers")
data class SneakerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val factionId: String,
    val rarity: String,
    val variant: Int,
    val level: Int,
    val mintNumber: Int,
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

/**
 * 크루 정보. 기본 제공 크루는 첫 실행 시 시드되고, 사용자가 만든 모임도 같은 표에 들어간다.
 * roster는 "이름|이름|이름" 형태로 직렬화한다.
 */
@Entity(tableName = "crews")
data class CrewEntity(
    @PrimaryKey val id: String,
    val name: String,
    val monogram: String,
    val tagline: String,
    val area: String,
    val kmAway: Double,
    val memberCount: Int,
    val roster: String,
    val createdAt: Long,
    /** 내가 만든 모임 */
    val owned: Boolean,
)

/**
 * 커뮤니티 게시글.
 * crewId가 비어 있으면 전체 게시판, 값이 있으면 해당 크루 전용 게시판이다.
 * 번개러닝(FLASH) 글만 place/meetAt/capacity를 쓴다.
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val crewId: String,
    val author: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val likes: Int,
    val liked: Boolean,
    val commentCount: Int,
    val mine: Boolean,
    val place: String,
    val distanceKm: Double,
    val meetAt: Long,
    val capacity: Int,
    val joinedCount: Int,
    val joined: Boolean,
)

/**
 * 게시글 댓글. parentId가 0이면 최상위 댓글, 아니면 그 댓글에 달린 답글이다.
 */
@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    /** 0이면 최상위 댓글, 그 외에는 부모 댓글 id */
    val parentId: Long,
    val author: String,
    val body: String,
    val createdAt: Long,
    /** 내가 쓴 댓글 */
    val mine: Boolean,
)

/**
 * 앱 내 알림. 본문은 type + 인자로 표시 시점에 현지화한다.
 * 액션형 알림(초대 수락, 보상 받기)은 argExtra에 대상 ID를 담고
 * actioned로 처리 여부를 기록한다.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val argText: String,
    val argAmount: Double,
    /** 액션 대상 — 크루 ID, 이벤트 ID 등 */
    val argExtra: String,
    val read: Boolean,
    /** 액션형 알림을 처리(수락/수령)했는지 */
    val actioned: Boolean,
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
    const val PARTY_MEMBER_LEFT = "PARTY_MEMBER_LEFT"

    // ── 액션형 ──
    /** 크루 초대 — 수락하면 해당 크루에 가입 (argExtra = crewId) */
    const val CREW_INVITE = "CREW_INVITE"

    /** 내 댓글에 답글이 달림 (argExtra = postId) */
    const val COMMENT_REPLY = "COMMENT_REPLY"

    /** 파티런 초대 — 수락하면 로비로 이동 (argExtra = crewId) */
    const val PARTY_INVITE = "PARTY_INVITE"

    /** 이벤트 보상 — 받기를 누르면 SUP 적립 (argAmount = 금액) */
    const val EVENT_REWARD = "EVENT_REWARD"
}
