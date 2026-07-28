package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.NotificationDao
import com.giwa.strideup.data.local.NotificationEntity
import kotlinx.coroutines.flow.Flow

/** 앱 내 알림함 */
class NotificationRepository(private val dao: NotificationDao) {

    fun notifications(limit: Int = 100): Flow<List<NotificationEntity>> = dao.observeAll(limit)

    val unreadCount: Flow<Int> = dao.observeUnreadCount()

    suspend fun markAllRead() = dao.markAllRead()

    suspend fun clear() = dao.clear()
}
