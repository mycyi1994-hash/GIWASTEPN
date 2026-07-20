package com.giwa.strideup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyStepsEntity::class, WalkSessionEntity::class, RewardEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao
    abstract fun walkSessionDao(): WalkSessionDao
    abstract fun rewardDao(): RewardDao
}
