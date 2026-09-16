package com.giwa.strideup.core

import android.content.Context
import androidx.room.Room
import com.giwa.strideup.BuildConfig
import com.giwa.strideup.data.local.AppDatabase
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.data.remote.AttesterClient
import com.giwa.strideup.data.repo.BoostRepository
import com.giwa.strideup.data.repo.ClaimRepository
import com.giwa.strideup.data.repo.CommunityRepository
import com.giwa.strideup.data.repo.CourseRepository
import com.giwa.strideup.data.repo.CrewRepository
import com.giwa.strideup.data.repo.EventRepository
import com.giwa.strideup.data.repo.NotificationRepository
import com.giwa.strideup.data.repo.RewardRepository
import com.giwa.strideup.data.repo.SneakerRepository
import com.giwa.strideup.data.repo.StepRepository
import com.giwa.strideup.sensor.StepTracker
import kotlinx.coroutines.flow.first

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
    lateinit var courseRepository: CourseRepository
        private set
    lateinit var eventRepository: EventRepository
        private set
    lateinit var notificationRepository: NotificationRepository
        private set

    lateinit var claimRepository: ClaimRepository
        private set

    fun init(context: Context) {
        if (this::database.isInitialized) return
        val app = context.applicationContext
        appContext = app
        database = Room.databaseBuilder(app, AppDatabase::class.java, "strideup.db")
            // 버전 7부터는 실제 마이그레이션을 쓴다. 러닝 기록이 SUP 청구의
            // 근거가 되는 순간부터, 스키마를 고쳤다고 사용자 기록을 지우는 것은
            // 개발 편의가 아니라 데이터 손실이다.
            .addMigrations(*AppDatabase.MIGRATIONS)
            // 마이그레이션 경로가 없는 옛 버전(6 미만)에서 올라오는 경우의
            // 안전망. 여기 걸리면 데모 데이터만 다시 만들어진다.
            .fallbackToDestructiveMigration()
            .build()
        userPrefs = UserPrefs(app)
        claimRepository = ClaimRepository(
            sessionDao = database.walkSessionDao(),
            client = AttesterClient(BuildConfig.ATTESTER_URL),
            runnerAddress = { userPrefs.runnerAddress.first() },
        )
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
        communityRepository = CommunityRepository(
            postDao = database.postDao(),
            commentDao = database.commentDao(),
            rewardRepository = rewardRepository,
            appContext = app,
        )
        courseRepository = CourseRepository(
            dao = database.courseDao(),
            prefs = userPrefs,
            rewardRepository = rewardRepository,
        )
        eventRepository = EventRepository(database.claimedEventDao(), rewardRepository)
        notificationRepository = NotificationRepository(database.notificationDao(), rewardRepository)
    }
}
