package com.stepup.android

import com.stepup.android.data.local.UploadState
import com.stepup.android.data.local.CrewDistance
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionRecorded
import com.stepup.android.data.repo.ClaimRepository
import com.stepup.android.data.repo.SessionRecorder
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.TrackPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 세션을 서버로 올리는 규칙.
 *
 * 여기서 가장 중요한 것은 "다시 시도할 일"과 "포기할 일"을 가르는 선이다.
 * 지하철에서 못 보낸 러닝을 포기하면 사용자가 정당하게 뛴 기록을 잃고,
 * 서버가 거절한 세션을 계속 보내면 배터리를 태운다. 둘 다 실기기에서만
 * 드러나는 종류의 버그라, 규칙 자체를 여기서 못 박는다.
 */
class ClaimUploadTest {

    // ── 도구 ────────────────────────────────────────────────────────────

    private class FakeDao(sessions: List<WalkSessionEntity>) : WalkSessionDao {
        val rows = sessions.associateBy { it.id }.toMutableMap()

        override suspend fun insert(session: WalkSessionEntity) {
            rows[session.id] = session
        }

        override suspend fun update(session: WalkSessionEntity) {
            rows[session.id] = session
        }

        override suspend fun pendingUploads(limit: Int): List<WalkSessionEntity> =
            rows.values
                .filter { it.uploadState in setOf(UploadState.PENDING.name, UploadState.FAILED.name) }
                .filter { it.track.isNotEmpty() && it.steps > 0 }
                .sortedBy { it.startedAt }
                .take(limit)

        override fun observeRecent(limit: Int): Flow<List<WalkSessionEntity>> = flowOf(rows.values.toList())
        override fun observeSessionCount(): Flow<Int> = flowOf(rows.size)
        override fun observeDurationSince(fromMillis: Long): Flow<Long> = flowOf(0L)
        override fun observePendingUploadCount(): Flow<Int> = flowOf(pendingCount())

        override suspend fun crewDistances(fromMillis: Long): List<CrewDistance> =
            rows.values
                .filter { it.crewId.isNotEmpty() && it.startedAt >= fromMillis }
                .groupBy { it.crewId }
                .map { (crewId, runs) ->
                    CrewDistance(crewId, runs.sumOf { it.distanceMeters }, runs.size)
                }

        private fun pendingCount() = rows.values.count {
            it.uploadState in setOf(UploadState.PENDING.name, UploadState.FAILED.name) && it.track.isNotEmpty()
        }
    }

    private class FakeRecorder(
        override val isConfigured: Boolean = true,
        private val answer: (WalkSessionEntity) -> ServerResult<SessionRecorded>,
    ) : SessionRecorder {
        val seen = mutableListOf<WalkSessionEntity>()
        override suspend fun record(session: WalkSessionEntity): ServerResult<SessionRecorded> {
            seen += session
            return answer(session)
        }
    }

    /**
     * 5초 간격 15m — 시속 10.8km 의 조깅.
     *
     * 이 좌표는 어테스터의 `app-payload.test.js` 가 같은 값으로 들고 있다.
     * 두 파일이 같은 본문을 보고 있어야 "앱이 보내는 것을 서버가 읽는다"가
     * 검증된다. 한쪽을 고치면 다른 쪽도 고친다.
     */
    private val track = RunTrack.encode(
        listOf(
            TrackPoint(37.526_312, 126.930_145, 1_700_000_000_000L),
            TrackPoint(37.526_447, 126.930_145, 1_700_000_005_000L),
        ),
    )

    private fun session(
        id: Long,
        startedAt: Long = 1_700_000_000_000L,
        steps: Int = 2_000,
        track: String = this.track,
        state: UploadState = UploadState.PENDING,
    ) = WalkSessionEntity(
        id = id,
        startedAt = startedAt,
        endedAt = startedAt + 600_000L,
        steps = steps,
        durationSec = 600,
        distanceMeters = 1_500.0,
        calories = 90.0,
        pointsEarned = 20.0,
        track = track,
        boostBps = 1_200,
        partySize = 2,
        uploadState = state.name,
    )

    private val okAnswer: (WalkSessionEntity) -> ServerResult<SessionRecorded> = {
        ServerResult.Ok(
            SessionRecorded(
                sessionId = 7,
                verdict = "CLEAN",
                pointsAwarded = 24.64,
                balance = 24.64,
            ),
        )
    }

    private fun repo(dao: FakeDao, recorder: SessionRecorder) =
        ClaimRepository(dao, recorder, now = { 42L })

    // ── 시작도 못 하는 경우 ──────────────────────────────────────────────

    @Test
    fun `서버 주소가 없으면 시도하지 않고 세션을 그대로 둔다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val run = repo(dao, FakeRecorder(isConfigured = false) { okAnswer(it) }).uploadPending()

        assertNotNull("이유를 말해 줘야 한다", run.blockedBy)
        assertEquals(UploadState.PENDING.name, dao.rows.getValue(1L).uploadState)
        assertEquals("시도 횟수를 축내면 안 된다", 0, dao.rows.getValue(1L).uploadAttempts)
    }

    @Test
    fun `다시 로그인이 필요하면 대기열을 건드리지 않고 물러난다`() = runBlocking {
        // 여기서 세션을 실패로 찍으면 시도 횟수만 오른다. 사용자가 로그인하면
        // 그대로 다시 올라가야 한다.
        val dao = FakeDao(listOf(session(1)))
        val run = repo(dao, FakeRecorder { ServerResult.SignInRequired("로그인이 만료되었습니다") })
            .uploadPending()

        assertNotNull(run.blockedBy)
        assertEquals(UploadState.PENDING.name, dao.rows.getValue(1L).uploadState)
        assertEquals(0, dao.rows.getValue(1L).uploadAttempts)
    }

    // ── 정상 경로 ───────────────────────────────────────────────────────

    @Test
    fun `서버가 기록하면 서버가 정한 금액으로 덮는다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val run = repo(dao, FakeRecorder { okAnswer(it) }).uploadPending()

        assertEquals(1, run.signed)
        val row = dao.rows.getValue(1L)
        assertEquals(UploadState.SIGNED.name, row.uploadState)
        assertEquals("CLEAN", row.verdict)
        // 앱이 화면에 보여준 값이 아니라 서버가 계산한 값이 남아야 한다.
        assertEquals("24.64", row.claimAmount)
        assertEquals("", row.uploadError)
    }

    @Test
    fun `정산 시점에 저장해 둔 부스트와 파티 인원을 그대로 보낸다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val recorder = FakeRecorder { okAnswer(it) }
        repo(dao, recorder).uploadPending()

        val sent = recorder.seen.single()
        // 지금 신고 있는 신발이 아니라 그때 신고 있던 신발의 값이어야 한다.
        assertEquals(1_200, sent.boostBps)
        assertEquals(2, sent.partySize)
        assertEquals(2_000, sent.steps)
    }

    @Test
    fun `오래된 세션부터 보낸다 - 청구 창이 7일이라 순서가 곧 손실이다`() = runBlocking {
        val dao = FakeDao(
            listOf(
                session(1, startedAt = 3_000_000_000_000L),
                session(2, startedAt = 1_000_000_000_000L),
                session(3, startedAt = 2_000_000_000_000L),
            ),
        )
        val recorder = FakeRecorder { okAnswer(it) }
        repo(dao, recorder).uploadPending()

        assertEquals(
            listOf(1_000_000_000_000L, 2_000_000_000_000L, 3_000_000_000_000L),
            recorder.seen.map { it.startedAt },
        )
    }

    // ── 갈림길: 다시 시도할 일과 포기할 일 ───────────────────────────────

    @Test
    fun `서버가 거절하면 다시 보내지 않는다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val run = repo(dao, FakeRecorder { ServerResult.Rejected("러닝으로 확인되지 않았습니다") })
            .uploadPending()

        assertEquals(1, run.rejected)
        val row = dao.rows.getValue(1L)
        assertEquals(UploadState.REJECTED.name, row.uploadState)
        assertEquals("러닝으로 확인되지 않았습니다", row.uploadError)
        assertTrue("거절된 세션은 대기열에서 빠져야 한다", dao.pendingUploads(10).isEmpty())
    }

    @Test
    fun `통신이 끊기면 다시 시도할 수 있게 남겨 둔다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val run = repo(dao, FakeRecorder { ServerResult.Retry("통신 실패") }).uploadPending()

        assertEquals(1, run.failed)
        assertTrue("일꾼이 다시 깨어나야 한다", run.shouldRetry)
        val row = dao.rows.getValue(1L)
        assertEquals(UploadState.FAILED.name, row.uploadState)
        assertEquals(1, row.uploadAttempts)
        assertTrue("다음 차례에 다시 집어야 한다", dao.pendingUploads(10).any { it.id == 1L })
    }

    @Test
    fun `한 번 끊기면 남은 세션은 건드리지 않는다`() = runBlocking {
        // 연달아 보내봐야 다 실패한다. 시도 횟수만 축내고 배터리를 태운다.
        val dao = FakeDao((1L..5L).map { session(it, startedAt = 1_000_000_000_000L + it) })
        val recorder = FakeRecorder { ServerResult.Retry("통신 실패") }
        val run = repo(dao, recorder).uploadPending()

        assertEquals("한 번만 시도해야 한다", 1, recorder.seen.size)
        assertEquals(1, run.failed)
        assertEquals(4, dao.rows.values.count { it.uploadState == UploadState.PENDING.name })
    }

    @Test
    fun `이미 서명받은 세션은 다시 보내지 않는다`() = runBlocking {
        val dao = FakeDao(listOf(session(1, state = UploadState.SIGNED)))
        val recorder = FakeRecorder { okAnswer(it) }
        val run = repo(dao, recorder).uploadPending()

        assertTrue(recorder.seen.isEmpty())
        assertEquals(0, run.signed)
    }

    @Test
    fun `실패했던 세션은 다음 차례에 다시 집는다`() = runBlocking {
        val dao = FakeDao(listOf(session(1, state = UploadState.FAILED)))
        val run = repo(dao, FakeRecorder { okAnswer(it) }).uploadPending()

        assertEquals(1, run.signed)
        assertEquals(UploadState.SIGNED.name, dao.rows.getValue(1L).uploadState)
    }
}
