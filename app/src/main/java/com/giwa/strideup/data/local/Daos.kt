package com.giwa.strideup.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Upsert
    suspend fun upsert(day: DailyStepsEntity)

    @Query("SELECT * FROM daily_steps WHERE epochDay >= :fromDay ORDER BY epochDay ASC")
    fun observeSince(fromDay: Long): Flow<List<DailyStepsEntity>>

    @Query("SELECT * FROM daily_steps WHERE epochDay = :day")
    suspend fun byDay(day: Long): DailyStepsEntity?

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_steps")
    fun observeTotalSteps(): Flow<Long>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_steps WHERE epochDay >= :fromDay")
    fun observeStepsSince(fromDay: Long): Flow<Long>
}

@Dao
interface WalkSessionDao {

    @Insert
    suspend fun insert(session: WalkSessionEntity)

    @Query("SELECT * FROM walk_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WalkSessionEntity>>

    @Query("SELECT COUNT(*) FROM walk_sessions")
    fun observeSessionCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationSec), 0) FROM walk_sessions WHERE startedAt >= :fromMillis")
    fun observeDurationSince(fromMillis: Long): Flow<Long>
}

@Dao
interface RewardDao {

    @Insert
    suspend fun insert(reward: RewardEntity)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM rewards")
    fun observeBalance(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM rewards")
    suspend fun balanceNow(): Double

    @Query("SELECT * FROM rewards ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun observeLedger(limit: Int): Flow<List<RewardEntity>>

    @Query("SELECT COUNT(*) FROM rewards WHERE type = :type")
    fun observeCountByType(type: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM rewards WHERE amount > 0")
    fun observeEarnedTotal(): Flow<Double>
}

@Dao
interface SneakerDao {

    @Insert
    suspend fun insert(sneaker: SneakerEntity): Long

    @Update
    suspend fun update(sneaker: SneakerEntity)

    @Delete
    suspend fun delete(sneaker: SneakerEntity)

    /**
     * rarity는 TEXT라 그냥 정렬하면 사전순(RARE > LEGENDARY > EPIC > COMMON)이 된다.
     * 도감은 등급이 높은 순으로 보여야 하므로 정렬 키를 따로 만든다.
     */
    @Query(
        """
        SELECT * FROM sneakers
        ORDER BY equipped DESC,
            CASE rarity
                WHEN 'LEGENDARY' THEN 3
                WHEN 'EPIC' THEN 2
                WHEN 'RARE' THEN 1
                ELSE 0
            END DESC,
            acquiredAt DESC
        """
    )
    fun observeAll(): Flow<List<SneakerEntity>>

    @Query("SELECT * FROM sneakers WHERE equipped = 1 LIMIT 1")
    fun observeEquipped(): Flow<SneakerEntity?>

    @Query("SELECT * FROM sneakers WHERE equipped = 1 LIMIT 1")
    suspend fun equippedNow(): SneakerEntity?

    @Query("SELECT * FROM sneakers WHERE id = :id")
    suspend fun byId(id: Long): SneakerEntity?

    @Query("UPDATE sneakers SET equipped = 0")
    suspend fun clearEquipped()

    @Query("SELECT COUNT(*) FROM sneakers")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM sneakers")
    fun observeCount(): Flow<Int>

    @Query("SELECT COALESCE(MAX(mintNumber), 0) FROM sneakers")
    suspend fun maxMintNumber(): Int
}

@Dao
interface BoostDao {

    @Insert
    suspend fun insert(boost: BoostEntity)

    @Query("SELECT * FROM boosts WHERE expiresAt > :now ORDER BY expiresAt ASC")
    fun observeActive(now: Long): Flow<List<BoostEntity>>

    @Query("SELECT * FROM boosts WHERE type = :type AND expiresAt > :now LIMIT 1")
    suspend fun activeOf(type: String, now: Long): BoostEntity?

    @Query("DELETE FROM boosts WHERE expiresAt <= :now")
    suspend fun purgeExpired(now: Long)
}

@Dao
interface ClaimedEventDao {

    @Insert
    suspend fun insert(entity: ClaimedEventEntity)

    @Query("SELECT * FROM claimed_events")
    fun observeAll(): Flow<List<ClaimedEventEntity>>

    @Query("SELECT * FROM claimed_events WHERE eventId = :id")
    suspend fun byId(id: String): ClaimedEventEntity?
}

@Dao
interface CrewDao {

    @Insert
    suspend fun insert(entity: CrewMembershipEntity)

    @Query("SELECT * FROM crew_memberships")
    fun observeAll(): Flow<List<CrewMembershipEntity>>

    @Query("DELETE FROM crew_memberships WHERE crewId = :id")
    suspend fun leave(id: String)
}

@Dao
interface CrewInfoDao {

    @Upsert
    suspend fun upsert(entity: CrewEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CrewEntity>)

    @Query("SELECT * FROM crews ORDER BY kmAway ASC")
    fun observeAll(): Flow<List<CrewEntity>>

    @Query("SELECT * FROM crews WHERE id = :id")
    suspend fun byId(id: String): CrewEntity?

    @Query("SELECT COUNT(*) FROM crews")
    suspend fun count(): Int
}

@Dao
interface PostDao {

    @Insert
    suspend fun insert(entity: PostEntity): Long

    @Insert
    suspend fun insertAll(entities: List<PostEntity>): List<Long>

    @Update
    suspend fun update(entity: PostEntity)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun byId(id: Long): PostEntity?

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM posts WHERE mine = 1")
    fun observeMineCount(): Flow<Int>
}

@Dao
interface CommentDao {

    @Insert
    suspend fun insert(entity: CommentEntity): Long

    @Insert
    suspend fun insertAll(entities: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :id OR parentId = :id")
    suspend fun deleteWithReplies(id: Long)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteForPost(postId: Long)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeForPost(postId: Long): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE id = :id")
    suspend fun byId(id: Long): CommentEntity?

    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    suspend fun countForPost(postId: Long): Int

    @Query("SELECT COUNT(*) FROM comments WHERE parentId = :commentId")
    suspend fun replyCount(commentId: Long): Int

    @Query(
        "SELECT * FROM comments WHERE postId = :postId AND parentId = 0 AND mine = 1 " +
            "ORDER BY createdAt DESC LIMIT 1"
    )
    suspend fun latestMineTopLevel(postId: Long): CommentEntity?

    @Query("SELECT COUNT(*) FROM comments")
    suspend fun count(): Int
}

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(entity: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    fun observeAll(limit: Int): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE notifications SET read = 1")
    suspend fun markAllRead()

    @Query("UPDATE notifications SET actioned = 1, read = 1 WHERE id = :id")
    suspend fun markActioned(id: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int

    @Query("DELETE FROM notifications")
    suspend fun clear()

    /**
     * "모두 읽음" 청소 — 아직 처리하지 않은 액션형 알림(초대·미수령 보상)은 남긴다.
     * 나머지는 전부 지운다.
     */
    @Query(
        """
        DELETE FROM notifications
        WHERE actioned = 1
           OR type NOT IN ('CREW_INVITE', 'PARTY_INVITE', 'EVENT_REWARD')
        """
    )
    suspend fun clearExceptPendingActions()
}
