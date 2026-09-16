package com.stepup.android

import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import com.stepup.android.data.remote.AuthUser
import com.stepup.android.data.remote.HttpPoster
import com.stepup.android.data.remote.HttpResponse
import com.stepup.android.data.remote.SessionHolder
import com.stepup.android.data.remote.SupabaseAuth
import com.stepup.android.data.remote.TokenResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 로그인 상태 관리.
 *
 * 로그인 수단은 구글 하나뿐이다. 여기가 틀리면 사용자가 이유도 모른 채
 * 로그아웃되고, 서버에 있던 기록에 다시 닿지 못한다. 실기기에서 재현하려면
 * 한 시간을 기다리거나 시계를 돌려야 하는 종류라, 규칙 자체를 여기서 못 박는다.
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
    fun `로그인한 적이 없으면 로그인을 요구한다`() = runBlocking {
        // 계정을 대신 만들어 줄 방법이 없다. 조용히 실패하면 사용자는
        // "왜 아무것도 저장이 안 되지"만 겪는다.
        val store = MemoryStore()
        val http = FakeHttp(emptyMap())

        val result = holder(store, http).accessToken()

        assertTrue(result is TokenResult.SignInRequired)
        assertTrue("서버를 부를 이유가 없다", http.calls.isEmpty())
    }

    @Test
    fun `로그인 여부를 저장된 세션으로 판단한다`() = runBlocking {
        // 첫 화면을 로그인으로 띄울지 정하는 근거다.
        val empty = SessionHolder(
            auth = SupabaseAuth("https://t.supabase.co", "k", FakeHttp(emptyMap())),
            store = MemoryStore(),
        )
        assertEquals(false, empty.isSignedIn())

        val signedIn = SessionHolder(
            auth = SupabaseAuth("https://t.supabase.co", "k", FakeHttp(emptyMap())),
            store = MemoryStore(stored("t", expiresAt = 9_999_999L, anonymous = false)),
        )
        assertEquals(true, signedIn.isSignedIn())
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
                "/token" to HttpResponse(
                    200,
                    """{"access_token":"t","refresh_token":"r","expires_in":3600}""",
                ),
            ),
        )

        holder(store, http, nowSeconds = 1_000_000L).signInWithGoogle("id-token")

        // 채우지 않으면 앱이 언제 갱신해야 하는지 알 수 없어 매번 갱신한다.
        assertEquals(1_003_600L, store.session?.expiresAt)
    }

    // ── 갱신이 실패했을 때 — 여기가 가장 중요하다 ────────────────────────

    @Test
    fun `자격이 만료되면 다시 로그인하게 한다`() = runBlocking {
        val store = MemoryStore(stored("old", expiresAt = 0L, anonymous = false))
        val http = FakeHttp(
            mapOf("/token" to HttpResponse(400, """{"msg":"Invalid Refresh Token"}""")),
        )

        val result = holder(store, http).accessToken()

        assertTrue(result is TokenResult.SignInRequired)
        // 네트워크가 잠깐 이상해 400 이 올 수도 있다. 그때까지 세션을 지워
        // 버리면 멀쩡한 계정을 잃는다. 로그인에 성공하면 어차피 덮어써진다.
        assertEquals("세션을 지우면 안 된다", 0, store.cleared)
    }

    @Test
    fun `네트워크가 끊겼을 때는 로그아웃시키지 않는다`() = runBlocking {
        // 지하철에서 갱신에 실패했다고 로그인 화면을 띄우면, 밖에 나왔을 때
        // 이유도 모른 채 다시 로그인하게 된다.
        val store = MemoryStore(stored("old", expiresAt = 0L, anonymous = false))
        val http = FakeHttp(mapOf("/token" to HttpResponse(0, "통신 실패")))

        val result = holder(store, http).accessToken()

        assertTrue(result is TokenResult.Unavailable)
        assertEquals("세션을 지우면 안 된다", 0, store.cleared)
        assertEquals("old", store.session?.accessToken)
    }

    // ── 구글 로그인 ─────────────────────────────────────────────────────

    @Test
    fun `구글로 로그인하면 세션이 저장된다`() = runBlocking {
        val store = MemoryStore()
        val http = FakeHttp(
            mapOf(
                "/token" to HttpResponse(
                    200,
                    sessionJson("google-token", userId = "user-1", anonymous = false),
                ),
            ),
        )

        val result = holder(store, http).signInWithGoogle("google-id-token")

        assertEquals("google-token", (result as TokenResult.Ok).accessToken)
        assertEquals("user-1", result.userId)
        assertTrue("저장해 둬야 앱을 껐다 켜도 유지된다", store.session != null)
    }

    @Test
    fun `구글 토큰이 거절되면 원래 세션을 유지한다`() = runBlocking {
        // 이미 로그인한 사람이 재로그인을 시도하다 실패한 경우다.
        // 실패가 로그아웃이 되면 멀쩡한 세션을 잃는다.
        val store = MemoryStore(stored("existing", expiresAt = 1_009_999L, anonymous = false))
        val http = FakeHttp(
            mapOf("/token" to HttpResponse(400, """{"error_description":"Bad ID token"}""")),
        )

        val result = holder(store, http).signInWithGoogle("stale-token")

        assertTrue(result is TokenResult.SignInRequired)
        assertEquals("existing", store.session?.accessToken)
    }

    @Test
    fun `로그아웃하면 세션이 지워진다`() = runBlocking {
        val store = MemoryStore(stored("t", expiresAt = 9_999_999L, anonymous = false))
        holder(store, FakeHttp(emptyMap())).signOut()

        assertEquals(null, store.session)
    }
}
