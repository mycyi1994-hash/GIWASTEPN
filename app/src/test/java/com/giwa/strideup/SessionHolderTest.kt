package com.giwa.strideup

import com.giwa.strideup.data.remote.AuthSession
import com.giwa.strideup.data.remote.AuthSessionStore
import com.giwa.strideup.data.remote.AuthUser
import com.giwa.strideup.data.remote.HttpPoster
import com.giwa.strideup.data.remote.HttpResponse
import com.giwa.strideup.data.remote.SessionHolder
import com.giwa.strideup.data.remote.SupabaseAuth
import com.giwa.strideup.data.remote.TokenResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 로그인 상태 관리.
 *
 * 여기가 틀리면 사용자가 이유도 모른 채 로그아웃되고, 서버에 있던 기록에
 * 다시 닿지 못한다. 실기기에서 재현하려면 한 시간을 기다리거나 시계를 돌려야
 * 하는 종류라, 규칙 자체를 여기서 못 박는다.
 */
class SessionHolderTest {

    // ── 도구 ────────────────────────────────────────────────────────────

    private class MemoryStore(var session: AuthSession? = null) : AuthSessionStore {
        var cleared = 0
        override suspend fun load() = session
        override suspend fun save(session: AuthSession) { this.session = session }
        override suspend fun clear() { session = null; cleared++ }
    }

    /** 요청 URL 별로 답을 정해 두는 가짜 서버 */
    private class FakeHttp(private val answers: Map<String, HttpResponse>) : HttpPoster {
        val calls = mutableListOf<String>()

        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            calls += url
            return answers.entries.firstOrNull { url.contains(it.key) }?.value
                ?: HttpResponse(404, """{"msg":"준비되지 않은 요청: $url"}""")
        }

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse =
            post(url, "", headers)
    }

    private fun sessionJson(
        access: String,
        refresh: String = "refresh-token",
        userId: String = "user-1",
        anonymous: Boolean = true,
    ) = """
        {"access_token":"$access","refresh_token":"$refresh","expires_in":3600,
         "user":{"id":"$userId","is_anonymous":$anonymous}}
    """.trimIndent()

    private fun holder(
        store: MemoryStore,
        http: FakeHttp,
        nowSeconds: Long = 1_000_000L,
    ) = SessionHolder(
        auth = SupabaseAuth(
            baseUrl = "https://test.supabase.co",
            apiKey = "sb_publishable_test",
            http = http,
            now = { nowSeconds },
        ),
        store = store,
        now = { nowSeconds },
    )

    private fun stored(
        access: String,
        expiresAt: Long,
        anonymous: Boolean = true,
    ) = AuthSession(
        accessToken = access,
        refreshToken = "refresh-token",
        expiresIn = 3600,
        expiresAt = expiresAt,
        user = AuthUser(id = "user-1", isAnonymous = anonymous),
    )

    // ── 처음 여는 사람 ───────────────────────────────────────────────────

    @Test
    fun `계정이 없으면 익명 계정을 만들어 준다`() = runBlocking {
        // 게스트가 곧 익명 계정이다. 처음 여는 순간 서버에 계정이 생겨야
        // 그때부터의 기록이 서버에 남는다.
        val store = MemoryStore()
        val http = FakeHttp(mapOf("/signup" to HttpResponse(200, sessionJson("new-token"))))

        val result = holder(store, http).accessToken()

        assertTrue(result is TokenResult.Ok)
        assertEquals("new-token", (result as TokenResult.Ok).accessToken)
        assertEquals("user-1", result.userId)
        assertTrue("저장해 둬야 앱을 껐다 켜도 유지된다", store.session != null)
    }

    @Test
    fun `익명 로그인이 꺼져 있으면 사용자에게 알린다`() = runBlocking {
        // 대시보드에서 익명 로그인을 안 켜면 여기로 온다. 조용히 실패하면
        // "왜 아무것도 저장이 안 되지"가 된다.
        val store = MemoryStore()
        val http = FakeHttp(
            mapOf("/signup" to HttpResponse(422, """{"msg":"Anonymous sign-ins are disabled"}""")),
        )

        val result = holder(store, http).accessToken()

        assertTrue(result is TokenResult.SignInRequired)
        assertTrue(
            "이유를 그대로 전해야 한다",
            (result as TokenResult.SignInRequired).reason.contains("Anonymous"),
        )
    }

    // ── 이미 로그인한 사람 ───────────────────────────────────────────────

    @Test
    fun `아직 멀쩡한 출입증은 그대로 쓴다`() = runBlocking {
        val store = MemoryStore(stored("good-token", expiresAt = 1_009_999L))
        val http = FakeHttp(emptyMap())

        val result = holder(store, http, nowSeconds = 1_000_000L).accessToken()

        assertEquals("good-token", (result as TokenResult.Ok).accessToken)
        assertTrue("서버를 부를 이유가 없다", http.calls.isEmpty())
    }

    @Test
    fun `만료가 가까우면 미리 갱신한다`() = runBlocking {
        // 딱 만료 시각에 맞춰 갱신하면 요청이 날아가는 중에 만료될 수 있다.
        val store = MemoryStore(stored("old-token", expiresAt = 1_000_060L)) // 60초 남음
        val http = FakeHttp(mapOf("/token" to HttpResponse(200, sessionJson("fresh-token"))))

        val result = holder(store, http, nowSeconds = 1_000_000L).accessToken()

        assertEquals("fresh-token", (result as TokenResult.Ok).accessToken)
        assertEquals("fresh-token", store.session?.accessToken)
    }

    @Test
    fun `갱신 응답에 사용자 정보가 빠져도 잃지 않는다`() = runBlocking {
        // 갱신은 토큰만 돌려주는 경우가 있다. 그대로 덮으면 익명 여부를 잊고,
        // 다음 갱신이 실패했을 때 구글 계정을 익명으로 오인해 지워 버린다.
        val store = MemoryStore(stored("old", expiresAt = 0L, anonymous = false))
        val http = FakeHttp(
            mapOf(
                "/token" to HttpResponse(
                    200,
                    """{"access_token":"fresh","refresh_token":"r2","expires_in":3600}""",
                ),
            ),
        )

        holder(store, http).accessToken()

        assertEquals("user-1", store.session?.user?.id)
        assertEquals(false, store.session?.user?.isAnonymous)
    }

    @Test
    fun `만료 시각이 안 오면 받은 시점 기준으로 채운다`() = runBlocking {
        val store = MemoryStore()
        val http = FakeHttp(
            mapOf(
                "/signup" to HttpResponse(
                    200,
                    """{"access_token":"t","refresh_token":"r","expires_in":3600}""",
                ),
            ),
        )

        holder(store, http, nowSeconds = 1_000_000L).accessToken()

        // 채우지 않으면 앱이 언제 갱신해야 하는지 알 수 없어 매번 갱신한다.
        assertEquals(1_003_600L, store.session?.expiresAt)
    }

    // ── 갱신이 실패했을 때 — 여기가 가장 중요하다 ────────────────────────

    @Test
    fun `익명 계정의 갱신이 거절되면 새 익명 계정을 만든다`() = runBlocking {
        // 익명 계정은 오래 안 쓰면 만료된다. 그때 그냥 오류를 내면 사용자는
        // 아무것도 못 하게 된다. 잃을 계정 정보도 없으니 새로 만들어 준다.
        val store = MemoryStore(stored("old", expiresAt = 0L, anonymous = true))
        val http = FakeHttp(
            mapOf(
                "/token" to HttpResponse(400, """{"msg":"Invalid Refresh Token"}"""),
                "/signup" to HttpResponse(200, sessionJson("brand-new", userId = "user-2")),
            ),
        )

        val result = holder(store, http).accessToken()

        assertEquals("brand-new", (result as TokenResult.Ok).accessToken)
        assertEquals("user-2", result.userId)
        assertEquals("헌 세션은 지워야 한다", 1, store.cleared)
    }

    @Test
    fun `구글 계정의 갱신이 거절되면 새 계정을 만들지 않는다`() = runBlocking {
        // 여기서 익명 계정을 만들어 주면, 사용자는 로그인돼 있다고 믿으면서
        // 남의 기록처럼 텅 빈 화면을 보게 된다. 다시 로그인하게 해야 한다.
        val store = MemoryStore(stored("old", expiresAt = 0L, anonymous = false))
        val http = FakeHttp(
            mapOf(
                "/token" to HttpResponse(400, """{"msg":"Invalid Refresh Token"}"""),
                "/signup" to HttpResponse(200, sessionJson("should-not-be-used")),
            ),
        )

        val result = holder(store, http).accessToken()

        assertTrue(result is TokenResult.SignInRequired)
        assertTrue("익명 가입을 시도하면 안 된다", http.calls.none { it.contains("/signup") })
        assertEquals("세션을 지우면 안 된다", 0, store.cleared)
    }

    @Test
    fun `네트워크가 끊겼을 때는 계정을 건드리지 않는다`() = runBlocking {
        // 지하철에서 갱신에 실패했다고 계정을 새로 만들면, 밖에 나왔을 때
        // 원래 계정이 아닌 빈 계정을 보게 된다.
        val store = MemoryStore(stored("old", expiresAt = 0L, anonymous = true))
        val http = FakeHttp(mapOf("/token" to HttpResponse(0, "통신 실패")))

        val result = holder(store, http).accessToken()

        assertTrue(result is TokenResult.Unavailable)
        assertEquals("세션을 지우면 안 된다", 0, store.cleared)
        assertEquals("old", store.session?.accessToken)
    }

    // ── 구글 붙이기 ─────────────────────────────────────────────────────

    @Test
    fun `구글을 붙이면 같은 계정이 정식 계정이 된다`() = runBlocking {
        // 따로 옮기는 과정이 없다는 것이 요점이다. 옮기지 않으니 옮기다 잃을 일도 없다.
        val store = MemoryStore(stored("anon", expiresAt = 1_009_999L, anonymous = true))
        val http = FakeHttp(
            mapOf(
                "/token" to HttpResponse(
                    200,
                    sessionJson("google-token", userId = "user-1", anonymous = false),
                ),
            ),
        )

        val result = holder(store, http).linkGoogle("google-id-token")

        assertEquals("google-token", (result as TokenResult.Ok).accessToken)
        assertEquals("계정이 바뀌면 안 된다", "user-1", result.userId)
        assertEquals(false, store.session?.user?.isAnonymous)
    }

    @Test
    fun `구글 토큰이 거절되면 원래 세션을 유지한다`() = runBlocking {
        val store = MemoryStore(stored("anon", expiresAt = 1_009_999L, anonymous = true))
        val http = FakeHttp(
            mapOf("/token" to HttpResponse(400, """{"error_description":"Bad ID token"}""")),
        )

        val result = holder(store, http).linkGoogle("stale-token")

        assertTrue(result is TokenResult.SignInRequired)
        assertEquals("붙이기 실패가 로그아웃이 되면 안 된다", "anon", store.session?.accessToken)
    }
}
