package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.ClaimedEventDao
import com.giwa.strideup.data.local.ClaimedEventEntity
import com.giwa.strideup.data.local.NotificationType
import com.giwa.strideup.data.local.RewardType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class EventKind { CHALLENGE, CAMPAIGN, MISSION }

/** 진행형 이벤트 정의 (보상 금액과 목표는 코드로 고정) */
data class EventDef(
    val id: String,
    val kind: EventKind,
    val reward: Double,
    val target: Double,
    /** 목표 달성 없이도 수령 가능한지 (캠페인 등) */
    val claimableWithoutTarget: Boolean = false,
)

object Events {
    val STEP_SURGE = EventDef("step_surge", EventKind.CHALLENGE, 250.0, 80_000.0)
    val REFER = EventDef("refer", EventKind.MISSION, 500.0, 3.0)
    val NIGHT_QUEST = EventDef("night_quest", EventKind.CHALLENGE, 300.0, 20.0)
    val NEON_HORIZON = EventDef("neon_horizon", EventKind.CAMPAIGN, 15_000.0, 1.0)

    val ALL = listOf(STEP_SURGE, REFER, NIGHT_QUEST, NEON_HORIZON)
}

/** 이벤트 보상 수령 상태 */
class EventRepository(
    private val dao: ClaimedEventDao,
    private val rewardRepository: RewardRepository,
) {

    val claimedIds: Flow<Set<String>> =
        dao.observeAll().map { list -> list.map { it.eventId }.toSet() }

    suspend fun isClaimed(id: String): Boolean = dao.byId(id) != null

    /**
     * 이벤트 보상 수령.
     *
     * @param progress 0.0~1.0 진행도
     * @return 실제로 적립된 금액. 수령 불가면 null.
     */
    suspend fun claim(def: EventDef, progress: Float): Double? {
        if (dao.byId(def.id) != null) return null
        if (!def.claimableWithoutTarget && progress < 1f) return null
        dao.insert(ClaimedEventEntity(def.id, System.currentTimeMillis(), def.reward))
        rewardRepository.credit(RewardType.EARN_EVENT, def.reward, "이벤트 보상: ${def.id}")
        rewardRepository.notify(NotificationType.EVENT_CLAIMED, def.id, def.reward)
        return def.reward
    }
}
