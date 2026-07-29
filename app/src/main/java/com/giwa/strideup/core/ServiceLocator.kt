package com.giwa.strideup.core

import android.content.Context
import androidx.room.Room
import com.giwa.strideup.data.local.AppDatabase
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.repo.BoostRepository
import com.giwa.strideup.data.repo.CommunityRepository
import com.giwa.strideup.data.repo.CrewRepository
import com.giwa.strideup.data.repo.EventRepository
import com.giwa.strideup.data.repo.NotificationRepository
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.sensor.StepTracker

/** 간단한 수동 DI 컨테이너. Application.onCreate에서 [init]을 호출한다. */
object ServiceLocator {

    lateinit var appContext: Context
        private set
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
    lateinit var sneakerRepository: SneakerRepository
        private set
    lateinit var boostRepository: BoostRepository
        private set
    lateinit var crewRepository: CrewRepository
        private set
    lateinit var communityRepository: CommunityRepository
        private set
    lateinit var eventRepository: EventRepository
        private set
    lateinit var notificationRepository: NotificationRepository
        private set

    fun init(context: Context) {
        if (this::database.isInitialized) return
        val app = context.applicationContext
        appContext = app
        database = Room.databaseBuilder(app, AppDatabase::class.java, "strideup.db")
            // 스키마가 확장되는 개발 단계 — 마이그레이션 실패로 앱이 죽는 것보다
            // 로컬 데모 데이터를 다시 만드는 편이 안전하다.
            .fallbackToDestructiveMigration()
            .build()
        userPrefs = UserPrefs(app)
        stepTracker = StepTracker(app, userPrefs) { day ->
            database.stepDao().byDay(day)?.steps ?: 0
        }
        rewardRepository = RewardRepository(
            rewardDao = database.rewardDao(),
            sneakerDao = database.sneakerDao(),
            boostDao = database.boostDao(),
            notificationDao = database.notificationDao(),
            prefs = userPrefs,
        )
        stepRepository = StepRepository(
            stepDao = database.stepDao(),
            walkSessionDao = database.walkSessionDao(),
            prefs = userPrefs,
            tracker = stepTracker,
            rewardRepository = rewardRepository,
        )
        sneakerRepository = SneakerRepository(database.sneakerDao(), rewardRepository)
        boostRepository = BoostRepository(database.boostDao(), rewardRepository, userPrefs)
        crewRepository = CrewRepository(
            crewDao = database.crewDao(),
            crewInfoDao = database.crewInfoDao(),
            rewardRepository = rewardRepository,
            appContext = app,
        )
        communityRepository = CommunityRepository(database.postDao(), app)
        eventRepository = EventRepository(database.claimedEventDao(), rewardRepository)
        notificationRepository = NotificationRepository(database.notificationDao(), rewardRepository)
    }
}
