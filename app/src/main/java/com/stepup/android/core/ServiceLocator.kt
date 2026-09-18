package com.stepup.android.core

import android.content.Context
import androidx.room.Room
import com.stepup.android.BuildConfig
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.GoogleSignIn
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.BoostRepository
import com.stepup.android.data.repo.ClaimRepository
import com.stepup.android.data.repo.PrefsAuthSessionStore
import com.stepup.android.data.repo.RankingRepository
import com.stepup.android.data.repo.ServerSessionRecorder
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.repo.CourseRepository
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.EventRepository
import com.stepup.android.data.repo.NotificationRepository
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.data.repo.StepRepository
import com.stepup.android.sensor.StepTracker

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

    /** 순위표 — 유일하게 남의 기록이 필요한 화면이라 서버가 계산해 준다 */
    lateinit var rankingRepository: RankingRepository
        private set

    /** 지금 누구로 로그인해 있는지. 화면이 로그인 상태를 물을 때 쓴다. */
    lateinit var sessionHolder: SessionHolder
        private set

    /** 구글 계정을 받아 오는 쪽. 로그인 화면이 쓴다. */
    lateinit var googleSignIn: GoogleSignIn
        private set

    fun init(context: Context) {
        if (this::database.isInitialized) return
        val app = context.applicationContext
        appContext = app
        // 파일 이름은 옛 이름 그대로 둔다. 패키지는 바꿔도 이 이름을 바꾸면
        // 안드로이드가 다른 파일을 찾게 되어, 이미 설치된 기기의 기록이 통째로
        // 사라진다. 눈에 거슬려도 건드리지 않는다.
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
        googleSignIn = GoogleSignIn(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        sessionHolder = SessionHolder(
            auth = SupabaseAuth(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY),
            store = PrefsAuthSessionStore(userPrefs),
        )
        val server = StepUpServer(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY, sessionHolder)
        claimRepository = ClaimRepository(
            sessionDao = database.walkSessionDao(),
            recorder = ServerSessionRecorder(server),
        )
        rankingRepository = RankingRepository(server)
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
            prefs = userPrefs,
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
