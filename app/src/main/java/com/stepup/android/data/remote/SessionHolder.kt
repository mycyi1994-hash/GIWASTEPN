package com.stepup.android.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 저장된 로그인 세션을 읽고 쓰는 곳.
 *
 * 실제로는 기기 저장소([com.stepup.android.data.prefs.UserPrefs])지만,
 * 테스트에서 갈아끼울 수 있게 인터페이스로 둔다.
 */
interface AuthSessionStore {
    suspend fun load(): AuthSession?
    suspend fun save(session: AuthSession)
    suspend fun clear()
}

/**
 * 지금 누구로 로그인해 있는지를 관리한다.
 *
 * 하는 일은 하나다 — **요청할 때마다 쓸 수 있는 출입증을 내준다.**
 *
 * 로그인 수단은 구글 하나뿐이라 계정을 대신 만들어 줄 방법이 없다. 세션이
 * 없으면 사용자가 직접 로그인해야 한다.
 *
 * 갱신 실패를 네트워크 문제와 자격 만료로 갈라 두는 것이 중요하다. 지하철에서
 * 끊긴 것을 로그아웃으로 처리하면, 밖에 나왔을 때 이유도 모른 채 로그인
 * 화면을 다시 보게 된다.
 */
class SessionHolder(
    private val auth: SupabaseAuth,
    private val store: AuthSessionStore,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    // 여러 화면이 동시에 요청하면 갱신이 겹친다. 겹치면 한쪽의 refresh token 이
    // 무효가 되어 그 요청부터 로그아웃된다. 한 번에 하나만 들어가게 한다.
    private val mutex = Mutex()

    /** 쓸 수 있는 출입증. 만료가 가까우면 갱신한다. */
    suspend fun accessToken(): TokenResult = mutex.withLock {
        val current = store.load()
            ?: return@withLock TokenResult.SignInRequired("로그인이 필요합니다")

        if (!current.needsRefresh(now())) {
            return@withLock TokenResult.Ok(current.accessToken, current.user?.id)
        }

        when (val refreshed = auth.refresh(current.refreshToken)) {
            is AuthResult.Ok -> {
                // 갱신 응답에 user 가 빠져 오는 경우가 있다. 이전 값을 잃지 않게 이어 붙인다.
                val merged = refreshed.session.copy(
                    user = refreshed.session.user ?: current.user,
                )
                store.save(merged)
                TokenResult.Ok(merged.accessToken, merged.user?.id)
            }

            // 자격이 만료됐다. 다시 로그인하게 한다. 저장된 세션은 지우지
            // 않는다 — 네트워크가 잠깐 이상해 400 이 온 경우까지 지워 버리면
            // 멀쩡한 세션을 잃는다. 로그인에 성공하면 어차피 덮어써진다.
            is AuthResult.Rejected -> TokenResult.SignInRequired(refreshed.reason)

            is AuthResult.Retry -> TokenResult.Unavailable(refreshed.reason)
        }
    }

    /** 구글로 로그인한다. */
    suspend fun signInWithGoogle(idToken: String, nonce: String? = null): TokenResult = mutex.withLock {
        when (val result = auth.signInWithGoogle(idToken, nonce)) {
            is AuthResult.Ok -> {
                val session = result.session
                store.save(session)
                TokenResult.Ok(session.accessToken, session.user?.id)
            }
            is AuthResult.Rejected -> TokenResult.SignInRequired(result.reason)
            is AuthResult.Retry -> TokenResult.Unavailable(result.reason)
        }
    }

    suspend fun currentUserId(): String? = store.load()?.user?.id

    /** 로그인한 적이 있는가. 첫 화면을 로그인으로 띄울지 정하는 근거다. */
    suspend fun isSignedIn(): Boolean = store.load() != null

    /** 로그아웃. 저장된 세션을 지운다. */
    suspend fun signOut() = mutex.withLock { store.clear() }
}

sealed interface TokenResult {
    data class Ok(val accessToken: String, val userId: String?) : TokenResult

    /** 지금은 안 되지만 나중에는 된다 — 네트워크·서버 문제 */
    data class Unavailable(val reason: String) : TokenResult

    /** 사용자가 직접 다시 로그인해야 한다 */
    data class SignInRequired(val reason: String) : TokenResult
}
