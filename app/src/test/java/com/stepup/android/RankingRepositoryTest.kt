package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.StepUpServer
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.repo.FactionRankingState
import com.stepup.android.data.repo.RankingProblem
import com.stepup.android.data.repo.RankingRepository
import com.stepup.android.data.repo.RankingState
import com.stepup.android.domain.Faction
import com.stepup.android.domain.RankBoard
import com.stepup.android.domain.RankPeriod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 서버가 보낸 순위표를 화면이 쓰는 모양으로 옮기는 부분.
 *
 * 여기서 조용히 어긋나기 쉬운 것은 이름이다. 서버는 `top_speed_kmh` 로
 * 보내는데 앱이 `topSpeedKmh` 로 읽으면, 오류 없이 **0** 이 들어온다. 그러면
 * 속도 랭킹이 전부 0.0 km/h 로 나오고, 원인은 화면이 아니라 여기에 있다.
 * 같은 종류의 어긋남을 이 프로젝트에서 이미 두 번 겪었다.
 */
class RankingRepositoryTest {

    private class FakeHttp(private val answer: HttpResponse) : HttpPoster {
        var lastBody: String = ""
        var lastUrl: String = ""

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            lastUrl = url
            lastBody = body
            return answer
        }

        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }

    private class LoggedIn : AuthSessionStore {
        override suspend fun load() = AuthSession(
            accessToken = "token",
            refreshToken = "refresh",
            expiresIn = 3600,
            // 넉넉히 살아 있는 출입증. 갱신 경로를 타지 않게 한다.
            expiresAt = 9_999_999_999L,
            user = AuthUser(id = "me", isAnonymous = false),
        )

        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private fun repo(http: FakeHttp) = RankingRepository(
        StepUpServer(
            baseUrl = "https://test.supabase.co",
            apiKey = "sb_publishable_test",
            sessions = SessionHolder(
                auth = SupabaseAuth("https://test.supabase.co", "sb_publishable_test", http),
                store = LoggedIn(),
                now = { 1_000L },
            ),
            http = http,
        ),
    )

    /** 서버가 실제로 내려주는 모양 그대로 */
    private val twoRows = """
        [
          {"rank":1,"user_id":"other","name":"Maya C.","monogram":"MC",
           "top_speed_kmh":16.4,"active_sec":42000,"sup":1820.5,"is_me":false,"total":37},
          {"rank":12,"user_id":"me","name":"Ara Kim","monogram":"AK",
           "top_speed_kmh":11.8,"active_sec":9000,"sup":310.25,"is_me":true,"total":37}
        ]
    """.trimIndent()

    @Test
    fun `서버가 보낸 줄을 그대로 옮긴다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, twoRows))

        val state = repo(http).personal(RankBoard.TOP_SPEED, RankPeriod.ALL, meLabel = "나")
        val ready = state as RankingState.Ready

        assertEquals(2, ready.entries.size)
        val top = ready.entries.first()
        assertEquals(1, top.rank)
        assertEquals("Maya C.", top.name)
        // 이 값이 0.0 이면 이름이 어긋난 것이다 — 랭킹이 통째로 0 이 된다.
        assertEquals(16.4, top.topSpeedKmh, 0.001)
        assertEquals(42_000L, top.activeSec)
        assertEquals(1820.5, top.sup, 0.001)
    }

    @Test
    fun `전체 인원은 받은 줄 수가 아니라 서버가 센 수다`() = runBlocking {
        // 두 줄만 받았지만 순위에 오른 사람은 37명이다. 받은 줄을 세면
        // "2명 중 12등"이라는 말이 된다.
        val ready = repo(FakeHttp(HttpResponse(200, twoRows)))
            .personal(RankBoard.TOP_SPEED, RankPeriod.ALL, meLabel = "나") as RankingState.Ready

        assertEquals(37, ready.totalRunners)
    }

    @Test
    fun `내 줄은 내 이름표로 바꿔 단다`() = runBlocking {
        val ready = repo(FakeHttp(HttpResponse(200, twoRows)))
            .personal(RankBoard.TOP_SPEED, RankPeriod.ALL, meLabel = "나") as RankingState.Ready

        val me = ready.me!!
        assertEquals(12, me.rank)
        // 자기 이름을 목록에서 찾는 것은 생각보다 느리다.
        assertEquals("나", me.name)
        assertEquals("ME", me.monogram)
    }

    @Test
    fun `아직 안 뛰었으면 내 줄이 없다`() = runBlocking {
        val onlyOthers = """
            [{"rank":1,"user_id":"other","name":"Maya C.","monogram":"MC",
              "top_speed_kmh":16.4,"active_sec":42000,"sup":1820.5,"is_me":false,"total":1}]
        """.trimIndent()

        val ready = repo(FakeHttp(HttpResponse(200, onlyOthers)))
            .personal(RankBoard.TOP_SPEED, RankPeriod.ALL, meLabel = "나") as RankingState.Ready

        // 0 으로 채운 줄을 만들어 "1등"이라고 하지 않는다.
        assertNull(ready.me)
    }

    @Test
    fun `부문 이름을 서버가 아는 말로 보낸다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "[]"))
        repo(http).personal(RankBoard.LONGEST_TIME, RankPeriod.WEEK, meLabel = "나")

        assertTrue(http.lastUrl.endsWith("/rpc/leaderboard"))
        assertTrue(http.lastBody, http.lastBody.contains(""""p_board":"LONGEST_TIME""""))
        // 기간 이름이 어긋나면 서버는 조용히 전체기간으로 답한다 — 주간 탭이
        // 전체기간을 보여 주는데 아무도 오류를 못 본다.
        assertTrue(http.lastBody, http.lastBody.contains(""""p_period":"WEEK""""))
    }

    @Test
    fun `종족 순위에도 기간이 함께 나간다`() = runBlocking {
        val http = FakeHttp(HttpResponse(200, "[]"))
        repo(http).factions(null, RankPeriod.MONTH)

        assertTrue(http.lastUrl.endsWith("/rpc/faction_leaderboard"))
        assertTrue(http.lastBody, http.lastBody.contains(""""p_period":"MONTH""""))
    }

    @Test
    fun `연결이 없으면 지어내지 않고 못 가져왔다고 한다`() = runBlocking {
        // status 0 은 요청이 나가지도 못한 경우다.
        val state = repo(FakeHttp(HttpResponse(0, "연결 없음")))
            .personal(RankBoard.TOP_SPEED, RankPeriod.ALL, meLabel = "나")

        assertEquals(RankingState.Failed(RankingProblem.OFFLINE), state)
    }

    @Test
    fun `서버가 바쁘면 나중에 다시 물을 일로 본다`() = runBlocking {
        val state = repo(FakeHttp(HttpResponse(503, """{"message":"바쁨"}""")))
            .personal(RankBoard.TOP_SPEED, RankPeriod.ALL, meLabel = "나")

        assertEquals(RankingState.Failed(RankingProblem.OFFLINE), state)
    }

    // ── 종족 ────────────────────────────────────────────────────────────

    @Test
    fun `종족 순위는 거리순으로 번호를 매긴다`() = runBlocking {
        val rows = """
            [
              {"faction":"FIRE","km":120.5,"my_km":12.0,"runners":8},
              {"faction":"LIGHTNING","km":340.25,"my_km":0,"runners":11},
              {"faction":"WATER","km":0,"my_km":0,"runners":0},
              {"faction":"WIND","km":88.0,"my_km":0,"runners":3}
            ]
        """.trimIndent()

        val ready = repo(FakeHttp(HttpResponse(200, rows)))
            .factions(Faction.FIRE, RankPeriod.ALL) as FactionRankingState.Ready

        assertEquals(
            listOf(Faction.LIGHTNING, Faction.FIRE, Faction.WIND, Faction.WATER),
            ready.rows.map { it.faction },
        )
        assertEquals(listOf(1, 2, 3, 4), ready.rows.map { it.rank })

        val fire = ready.rows.first { it.faction == Faction.FIRE }
        assertEquals(120.5, fire.km, 0.001)
        assertEquals(12.0, fire.myKm, 0.001)
        // 지금 신고 있는 신발의 종족이 "우리 편"이다.
        assertTrue(fire.isMine)
        assertTrue(ready.rows.count { it.isMine } == 1)
    }

    @Test
    fun `모르는 종족이 와도 죽지 않는다`() = runBlocking {
        // 서버에 새 종족이 먼저 생기고 앱이 나중에 따라가는 경우가 있다.
        val rows = """
            [{"faction":"PLASMA","km":10,"my_km":0,"runners":1},
             {"faction":"FIRE","km":5,"my_km":5,"runners":1}]
        """.trimIndent()

        val ready = repo(FakeHttp(HttpResponse(200, rows)))
            .factions(null, RankPeriod.ALL) as FactionRankingState.Ready

        assertEquals(listOf(Faction.FIRE), ready.rows.map { it.faction })
        // 모르는 종족을 걸러 낸 자리가 비어 1등이 사라지면 안 된다.
        assertEquals(listOf(1), ready.rows.map { it.rank })
    }
}
