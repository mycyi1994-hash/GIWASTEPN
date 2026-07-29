package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.NotificationDao
import com.giwa.strideup.data.local.NotificationEntity
import com.giwa.strideup.data.local.NotificationType
import com.giwa.strideup.data.local.RewardType
import kotlinx.coroutines.flow.Flow

/** 앱 내 알림함 — 읽음 처리 · 액션형(초대 수락, 보상 받기) 알림 처리 */
class NotificationRepository(
    private val dao: NotificationDao,
    private val rewardRepository: RewardRepository,
) {

    fun notifications(limit: Int = 100): Flow<List<NotificationEntity>> = dao.observeAll(limit)

    val unreadCount: Flow<Int> = dao.observeUnreadCount()

    suspend fun markAllRead() = dao.markAllRead()

    /**
     * "모두 읽음" — 알림함을 비운다.
     * 단, 아직 수령/응답하지 않은 액션형 알림(크루·파티 초대, 이벤트 보상)은 남긴다.
     */
    suspend fun clearAll() {
        dao.clearExceptPendingActions()
        dao.markAllRead()
    }

    /** 크루 초대 수락 — 크루 가입 후 알림을 처리 상태로 바꾼다 */
    suspend fun acceptCrewInvite(entity: NotificationEntity, crewRepository: CrewRepository) {
        if (entity.actioned || entity.type != NotificationType.CREW_INVITE) return
        if (entity.argExtra.isNotBlank()) crewRepository.join(entity.argExtra)
        dao.markActioned(entity.id)
    }

    /** 초대 거절 — 알림만 지운다 */
    suspend fun decline(entity: NotificationEntity) = dao.delete(entity.id)

    /** 파티런 초대 수락 표시 (로비 이동은 화면에서) */
    suspend fun acceptPartyInvite(entity: NotificationEntity) {
        if (entity.type == NotificationType.PARTY_INVITE) dao.markActioned(entity.id)
    }

    /** 이벤트 보상 수령 — 실제 SUP 적립 */
    suspend fun claimEventReward(entity: NotificationEntity) {
        if (entity.actioned || entity.type != NotificationType.EVENT_REWARD) return
        if (entity.argAmount > 0) {
            rewardRepository.credit(
                RewardType.EARN_EVENT,
                entity.argAmount,
                "이벤트 보상: ${entity.argText}",
            )
        }
        dao.markActioned(entity.id)
    }

    /**
     * 첫 실행 웰컴 알림 — 액션형 알림을 바로 체험할 수 있게
     * 크루 초대 · 파티런 초대 · 이벤트 보상을 하나씩 넣어 둔다.
     */
    suspend fun seedWelcome() {
        if (dao.count() > 0) return
        val now = System.currentTimeMillis()
        dao.insert(
            NotificationEntity(
                timestamp = now - 40_000,
                type = NotificationType.EVENT_REWARD,
                argText = "Welcome Runner",
                argAmount = 30.0,
                argExtra = "welcome",
                read = false,
                actioned = false,
            )
        )
        dao.insert(
            NotificationEntity(
                timestamp = now - 25_000,
                type = NotificationType.CREW_INVITE,
                argText = "Night Runners",
                argAmount = 0.0,
                argExtra = "night_runners",
                read = false,
                actioned = false,
            )
        )
        dao.insert(
            NotificationEntity(
                timestamp = now - 10_000,
                type = NotificationType.PARTY_INVITE,
                argText = "Trailblazer Crew",
                argAmount = 0.0,
                argExtra = "trailblazer",
                read = false,
                actioned = false,
            )
        )
    }
}
