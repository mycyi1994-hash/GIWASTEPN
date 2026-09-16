package com.giwa.strideup

import com.giwa.strideup.data.local.UploadState
import com.giwa.strideup.data.local.WalkSessionDao
import com.giwa.strideup.data.local.WalkSessionEntity
import com.giwa.strideup.data.remote.ClaimRequest
import com.giwa.strideup.data.remote.ClaimResult
import com.giwa.strideup.data.remote.ClaimSubmitter
import com.giwa.strideup.data.remote.SignedClaim
import com.giwa.strideup.data.repo.ClaimRepository
import com.giwa.strideup.domain.RunTrack
import com.giwa.strideup.domain.TrackPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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

        private fun pendingCount() = rows.values.count {
            it.uploadState in setOf(UploadState.PENDING.name, UploadState.FAILED.name) && it.track.isNotEmpty()
        }
    }

    private class FakeSubmitter(
        override val isConfigured: Boolean = true,
        private val answer: (ClaimRequest) -> ClaimResult,
    ) : ClaimSubmitter {
        val seen = mutableListOf<ClaimRequest>()
        override suspend fun submit(request: ClaimRequest): ClaimResult {
            seen += request
            return answer(request)
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

    private val signedAnswer: (ClaimRequest) -> ClaimResult = {
        ClaimResult.Signed(
            claim = SignedClaim(
                runner = it.runner,
                sessionHash = "0xabc",
                amount = "20000000000000000000",
                day = 7,
                deadline = 1_700_000_900L,
            ),
            signature = "0xsig",
            verdict = "CLEAN",
        )
    }

    private fun repo(
        dao: FakeDao,
        submitter: ClaimSubmitter,
        runner: String = "0x1111111111111111111111111111111111111111",
    ) = ClaimRepository(dao, submitter, runnerAddress = { runner }, now = { 42L })

    // ── 시작도 못 하는 경우 ──────────────────────────────────────────────

    @Test
    fun `서버 주소가 없으면 시도하지 않고 세션을 그대로 둔다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val run = repo(dao, FakeSubmitter(isConfigured = false) { signedAnswer(it) }).uploadPending()

        assertNotNull("이유를 말해 줘야 한다", run.blockedBy)
        assertEquals(UploadState.PENDING.name, dao.rows.getValue(1L).uploadState)
        assertEquals("시도 횟수를 축내면 안 된다", 0, dao.rows.getValue(1L).uploadAttempts)
    }

    @Test
    fun `지갑이 없으면 세션을 쌓아 둔다 - 나중에 지갑을 만들면 살아 있어야 한다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val submitter = FakeSubmitter { signedAnswer(it) }
        val run = repo(dao, submitter, runner = "").uploadPending()

        assertNotNull(run.blockedBy)
        assertTrue("보내지 않아야 한다", submitter.seen.isEmpty())
        assertEquals(UploadState.PENDING.name, dao.rows.getValue(1L).uploadState)
    }

    // ── 정상 경로 ───────────────────────────────────────────────────────

    @Test
    fun `서명을 받으면 청구서를 통째로 보관한다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val run = repo(dao, FakeSubmitter { signedAnswer(it) }).uploadPending()

        assertEquals(1, run.signed)
        val row = dao.rows.getValue(1L)
        assertEquals(UploadState.SIGNED.name, row.uploadState)
        assertEquals("0xsig", row.claimSignature)
        assertEquals("0xabc", row.claimSessionHash)
        // 지급액은 18자리라 문자열로 온다. 숫자로 바꾸면 값이 깨진다.
        assertEquals("20000000000000000000", row.claimAmount)
        assertEquals(7L, row.claimDay)
        assertEquals(1_700_000_900L, row.claimDeadline)
        assertEquals("CLEAN", row.verdict)
        assertEquals("", row.uploadError)
    }

    @Test
    fun `정산 시점에 저장해 둔 부스트와 파티 인원을 그대로 보낸다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val submitter = FakeSubmitter { signedAnswer(it) }
        repo(dao, submitter).uploadPending()

        val sent = submitter.seen.single()
        // 지금 신고 있는 신발이 아니라 그때 신고 있던 신발의 값이어야 한다.
        assertEquals(1_200, sent.boostBps)
        assertEquals(2, sent.partySize)
        assertEquals(2_000, sent.steps)
        assertEquals(2, sent.track.size)
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
        val submitter = FakeSubmitter { signedAnswer(it) }
        repo(dao, submitter).uploadPending()

        assertEquals(
            listOf(1_000_000_000_000L, 2_000_000_000_000L, 3_000_000_000_000L),
            submitter.seen.map { it.startedAt },
        )
    }

    // ── 갈림길: 다시 시도할 일과 포기할 일 ───────────────────────────────

    @Test
    fun `서버가 거절하면 다시 보내지 않는다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val run = repo(dao, FakeSubmitter { ClaimResult.Rejected("러닝으로 확인되지 않았습니다", "VOID") })
            .uploadPending()

        assertEquals(1, run.rejected)
        val row = dao.rows.getValue(1L)
        assertEquals(UploadState.REJECTED.name, row.uploadState)
        assertEquals("VOID", row.verdict)
        assertEquals("러닝으로 확인되지 않았습니다", row.uploadError)
        assertTrue("거절된 세션은 대기열에서 빠져야 한다", dao.pendingUploads(10).isEmpty())
    }

    @Test
    fun `통신이 끊기면 다시 시도할 수 있게 남겨 둔다`() = runBlocking {
        val dao = FakeDao(listOf(session(1)))
        val run = repo(dao, FakeSubmitter { ClaimResult.Retry("통신 실패") }).uploadPending()

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
        val submitter = FakeSubmitter { ClaimResult.Retry("통신 실패") }
        val run = repo(dao, submitter).uploadPending()

        assertEquals("한 번만 시도해야 한다", 1, submitter.seen.size)
        assertEquals(1, run.failed)
        assertEquals(4, dao.rows.values.count { it.uploadState == UploadState.PENDING.name })
    }

    @Test
    fun `경로가 모자란 세션은 보내지 않고 거절로 끝낸다`() = runBlocking {
        // 서버는 좌표 2개 이상을 요구한다. 보내봐야 400이다.
        val dao = FakeDao(listOf(session(1, track = RunTrack.encode(listOf(TrackPoint(37.5, 127.0, 1L))))))
        val submitter = FakeSubmitter { signedAnswer(it) }
        val run = repo(dao, submitter).uploadPending()

        assertTrue("보내지 않아야 한다", submitter.seen.isEmpty())
        assertEquals(1, run.rejected)
        assertEquals(UploadState.REJECTED.name, dao.rows.getValue(1L).uploadState)
    }

    @Test
    fun `이미 서명받은 세션은 다시 보내지 않는다`() = runBlocking {
        val dao = FakeDao(listOf(session(1, state = UploadState.SIGNED)))
        val submitter = FakeSubmitter { signedAnswer(it) }
        val run = repo(dao, submitter).uploadPending()

        assertTrue(submitter.seen.isEmpty())
        assertEquals(0, run.signed)
    }

    @Test
    fun `실패했던 세션은 다음 차례에 다시 집는다`() = runBlocking {
        val dao = FakeDao(listOf(session(1, state = UploadState.FAILED)))
        val run = repo(dao, FakeSubmitter { signedAnswer(it) }).uploadPending()

        assertEquals(1, run.signed)
        assertEquals(UploadState.SIGNED.name, dao.rows.getValue(1L).uploadState)
    }

    // ── 전송 형식 ───────────────────────────────────────────────────────

    @Test
    fun `보내는 JSON의 이름이 서버가 읽는 이름과 같다`() = runBlocking {
        // 서버(attester/src/economy.js)는 좌표의 시각을 t 로 읽는다. 앱 안에서는
        // at 이라 부르므로, 직렬화가 이름을 바꿔 주지 않으면 모든 구간의 시각이
        // 0이 되고 속도 계산이 통째로 틀어진다.
        val request = ClaimRequest(
            runner = "0x1111111111111111111111111111111111111111",
            startedAt = 1_700_000_000_000L,
            endedAt = 1_700_000_600_000L,
            steps = 2_000,
            boostBps = 1_200,
            partySize = 2,
            track = RunTrack.decode(track).map { com.giwa.strideup.data.remote.TrackPointDto(it.lat, it.lng, it.at) },
        )
        val encoded = Json.encodeToString(request)

        assertTrue("좌표 시각은 t 여야 한다: $encoded", encoded.contains("\"t\":1700000000000"))
        assertTrue(encoded.contains("\"runner\":"))
        assertTrue(encoded.contains("\"startedAt\":"))
        assertTrue(encoded.contains("\"endedAt\":"))
        assertTrue(encoded.contains("\"boostBps\":"))
        assertTrue(encoded.contains("\"partySize\":"))
        assertTrue(encoded.contains("\"lat\":"))
        assertTrue(encoded.contains("\"lng\":"))
        // 앱 내부 이름이 새어 나가면 서버는 그 필드를 무시하고 기본값을 쓴다.
        assertTrue("내부 이름 at 이 나가면 안 된다: $encoded", !encoded.contains("\"at\":"))
    }

    @Test
    fun `서버가 모르는 필드를 더 붙여도 앱이 죽지 않는다`() {
        // 서버는 앱보다 자주 바뀐다. 필드가 하나 늘었다고 이미 설치된 앱이
        // 응답을 못 읽으면, 그 사용자의 세션은 영영 올라가지 않는다.
        val json = Json { ignoreUnknownKeys = true }
        val body = """
            {"ok":true,"verdict":"CLEAN","newFieldFromFuture":123,
             "claim":{"runner":"0x1","sessionHash":"0x2","amount":"5","day":1,"deadline":2},
             "signature":"0x3"}
        """.trimIndent()
        val parsed = json.decodeFromString<com.giwa.strideup.data.remote.ClaimResponse>(body)

        assertTrue(parsed.ok)
        assertEquals("0x3", parsed.signature)
        assertEquals("5", parsed.claim?.amount)
        assertNull(parsed.error)
    }
}
