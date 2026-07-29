package com.giwa.strideup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyStepsEntity::class,
        WalkSessionEntity::class,
        RewardEntity::class,
        SneakerEntity::class,
        BoostEntity::class,
        ClaimedEventEntity::class,
        CrewMembershipEntity::class,
        CrewEntity::class,
        PostEntity::class,
        CommentEntity::class,
        NotificationEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao
    abstract fun walkSessionDao(): WalkSessionDao
    abstract fun rewardDao(): RewardDao
    abstract fun sneakerDao(): SneakerDao
    abstract fun boostDao(): BoostDao
    abstract fun claimedEventDao(): ClaimedEventDao
    abstract fun crewDao(): CrewDao
    abstract fun crewInfoDao(): CrewInfoDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun notificationDao(): NotificationDao
}
