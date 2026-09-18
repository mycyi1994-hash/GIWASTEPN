package com.stepup.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        CourseEntity::class,
        NotificationEntity::class,
    ],
    version = 10,
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
    abstract fun courseDao(): CourseDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        /**
         * 러닝 세션에 GPS 경로와 정산 시점 값을 더한다.
         *
         * 여기서부터는 스키마가 바뀌어도 기록을 버리지 않는다. 지금까지는
         * 데모 데이터뿐이라 새로 만들어도 그만이었지만, 이 열에 담기는 경로는
         * 곧 SUP 청구의 근거가 된다 — 사용자의 러닝 기록을 개발 편의로
         * 지우는 일은 여기서 끝낸다.
         *
         * 기존 행은 경로를 알 수 없으므로 빈 값으로 남는다. 청구 대상이
         * 아니었던 세션이니 그것이 사실에 맞다.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN track TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN boostBps INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN partySize INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * 세션에 서버 업로드 상태를 더한다.
         *
         * 기존 행은 PENDING 이 아니라 REJECTED 로 둔다. 그 세션들에는 GPS
         * 경로가 없어서(마이그레이션 6→7 참고) 서버가 판정할 수 없다. PENDING
         * 으로 두면 업로드 일꾼이 영원히 거절당할 요청을 계속 보낸다.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE walk_sessions ADD COLUMN uploadState TEXT NOT NULL DEFAULT 'PENDING'",
                )
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN uploadAttemptedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN uploadAttempts INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN uploadError TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN verdict TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN claimSignature TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN claimSessionHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN claimAmount TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN claimDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN claimDeadline INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE walk_sessions
                       SET uploadState = 'REJECTED',
                           uploadError = '경로가 기록되기 전의 세션입니다'
                     WHERE track = ''
                    """.trimIndent(),
                )
            }
        }

        /**
         * 세션에 정산 시점 종족을 더한다.
         *
         * 기존 행은 빈 값으로 남는다. 그 세션들이 어느 신발로 달린 것인지는
         * 이제 알 수 없고, 모르는 것을 아무 종족에나 얹으면 종족 순위가
         * 지어낸 숫자가 된다.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN faction TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * 세션에 크루 러닝의 크루 id 를 더한다.
         *
         * 기존 행은 빈 값으로 남는다. 파티런이었는지는 partySize 로 알 수 있지만
         * **어느 크루였는지**는 남아 있지 않다. 모르는 것을 아무 크루에나 얹으면
         * 크루 순위가 지어낸 숫자가 되므로, 크루 순위는 이 열이 생긴 뒤의
         * 러닝부터 센다.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN crewId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
    }
}
