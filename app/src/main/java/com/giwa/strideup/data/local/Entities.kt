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
}
