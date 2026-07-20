package com.giwa.strideup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
}

@Dao
interface WalkSessionDao {

    @Insert
    suspend fun insert(session: WalkSessionEntity)

    @Query("SELECT * FROM walk_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WalkSessionEntity>>
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
}
