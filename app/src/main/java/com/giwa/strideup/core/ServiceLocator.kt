package com.giwa.strideup.core

import android.content.Context
import androidx.room.Room
import com.giwa.strideup.data.local.AppDatabase
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.sensor.StepTracker

/** 간단한 수동 DI 컨테이너. Application.onCreate에서 [init]을 호출한다. */
object ServiceLocator {

    lateinit var database: AppDatabase
        private set
    lateinit var userPrefs: UserPrefs
        private set
    lateinit var stepTracker: StepTracker
        private set
    lateinit var rewardRepository: RewardRepository
        private set
    lateinit var stepRepository: StepRepository
        private set

    fun init(context: Context) {
        if (this::database.isInitialized) return
        val app = context.applicationContext
        database = Room.databaseBuilder(app, AppDatabase::class.java, "strideup.db").build()
        userPrefs = UserPrefs(app)
        stepTracker = StepTracker(app, userPrefs)
        rewardRepository = RewardRepository(database.rewardDao(), userPrefs)
        stepRepository = StepRepository(
            stepDao = database.stepDao(),
            walkSessionDao = database.walkSessionDao(),
            prefs = userPrefs,
            tracker = stepTracker,
            rewardRepository = rewardRepository,
        )
    }
}
