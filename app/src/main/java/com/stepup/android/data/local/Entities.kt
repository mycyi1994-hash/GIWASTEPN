package com.stepup.android.data.local

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
    /**
     * 이번 세션에 실제로 지나간 GPS 경로. `RunTrack.encode`가 만든 문자열이고,
     * 좌표마다 시각이 붙어 있다.
     *
     * 합계(거리·칼로리)만으로는 "사람이 뛴 것인가"를 판정할 수 없다. 구간
     * 속도를 다시 계산하려면 원본 좌표와 시각이 있어야 하고, 판정할 수 없는
     * 세션은 온체인으로 청구할 수 없다. GPS 권한이 없거나 신호를 못 받은
     * 세션은 빈 문자열이다.
     */
    val track: String = "",
    /**
     * 정산 시점 스니커즈 부스트(bps). 1780 = +17.8%.
     *
     * 나중에 다시 계산할 수 없어서 같이 남긴다 — 사용자는 신발을 갈아신고
     * 강화하고 팔기도 한다. 청구서에 적힐 값은 **그때** 신고 있던 신발의 것이다.
     */
    val boostBps: Int = 0,
    /**
     * 정산 시점 파티 인원. 같은 이유로 지금 남겨야 한다 — 크루 상태는 변한다.
     */
    val partySize: Int = 1,
    /**
     * 정산 시점에 신고 있던 신발의 종족 (FIRE · WATER · LIGHTNING · WIND).
     *
     * 종족 랭킹은 "이 거리가 어느 편에 쌓이는가"인데, 그 답은 달릴 때
     * 신고 있던 신발이 정한다. 나중에 신발을 갈아신으면 알 수 없게 되므로
     * 부스트·파티 인원과 같은 이유로 여기 남긴다. 맨발이면 빈 문자열이다.
     */
    val faction: String = "",
    /**
     * 크루 러닝이었다면 그 크루의 id. 혼자 달렸거나 번개러닝이면 빈 문자열이다.
     *
     * 크루 순위가 세는 것이 이 열이다. 크루원이 혼자 달린 거리까지 크루에
     * 얹으면 사람 많은 크루가 자동으로 1등이 되고, 그러면 크루 러닝을 여는
     * 이유가 사라진다. 여기 이름이 적힌 세션만 크루의 몫이다.
     *
     * 부스트·파티 인원과 같은 이유로 정산 시점에 못 박는다 — 나중에 크루를
     * 탈퇴해도 그날 같이 달린 사실은 변하지 않는다.
     */
    val crewId: String = "",
    /**
     * 서버 업로드 상태. [UploadState] 의 이름 문자열이 들어간다.
     *
     * 러닝은 지하철이나 산에서도 끝난다. 그 자리에서 못 올린 세션을 버리지
     * 않고 여기에 표시해 두었다가 연결이 돌아오면 다시 시도한다.
     */
    val uploadState: String = UploadState.PENDING.name,
    /** 마지막 시도 시각. 다시 시도할 때까지 얼마나 기다릴지 정하는 근거다. */
    val uploadAttemptedAt: Long = 0,
    /** 지금까지 시도한 횟수 */
    val uploadAttempts: Int = 0,
    /** 실패했다면 그 이유 — 사용자에게 보여주고, 버그를 쫓을 때 읽는다 */
    val uploadError: String = "",
    /** 서버가 내린 판정 (CLEAN · FLAGGED · VOID) */
    val verdict: String = "",
    /** 서버가 서명한 청구서 — 체인에 제출할 때 그대로 쓴다 */
    val claimSignature: String = "",
    val claimSessionHash: String = "",
    /** 지급액(wei 문자열). 18자리라 Long 에 담기지 않는다. */
    val claimAmount: String = "",
    val claimDay: Long = 0,
    /** 이 서명이 유효한 마지막 시각 (epoch 초) */
    val claimDeadline: Long = 0,
)

/**
 * 세션 하나가 서버까지 가는 길.
 *
 * [REJECTED] 와 [FAILED] 를 나눠 두는 것이 핵심이다. 서버가 "이건 러닝이
 * 아니다"라고 판정한 것과, 지하철이라 못 보낸 것은 전혀 다른 일이다. 앞의
 * 것을 계속 재시도하면 배터리를 태우고, 뒤의 것을 포기하면 사용자가 정당하게
 * 뛴 기록을 잃는다.
 */
enum class UploadState {
    /** 아직 안 올렸다 */
    PENDING,

    /** 서명을 받았다. 체인 제출만 남았다. */
    SIGNED,

    /** 서버가 거절했다. 다시 보내도 같은 답이 온다. */
    REJECTED,

    /** 보내다 실패했다. 나중에 다시 시도한다. */
    FAILED,
}

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
 * 러닝 코스. 좌표는 "lat,lng;lat,lng;…" 한 문자열로 눌러 담는다.
 */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val area: String,
    val distanceKm: Double,
    val elevationM: Int,
    /** 인코딩된 GPS 경로 */
    val track: String,
    val author: String,
    val mine: Boolean,
    /** 코스 게시판에 공유했는지 */
    val shared: Boolean,
    val likes: Int,
    val liked: Boolean,
    val runCount: Int,
    val createdAt: Long,
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
    const val COURSE_COMPLETE = "COURSE_COMPLETE"

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
