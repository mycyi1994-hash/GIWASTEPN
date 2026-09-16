package com.giwa.strideup.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 저장된 로그인 세션을 읽고 쓰는 곳.
 *
 * 실제로는 기기 저장소([com.giwa.strideup.data.prefs.UserPrefs])지만,
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
 * 그 과정에서 세 가지를 알아서 처리한다.
 *
 *  1. 아직 계정이 없으면 익명 계정을 만든다
 *  2. 출입증이 곧 만료되면 미리 갱신한다
 *  3. 갱신마저 거절되면 (예: 오래 안 써서 만료) 익명 계정을 다시 만든다
 *
 * 3번이 중요하다. 갱신 실패를 그냥 오류로 넘기면 사용자는 이유도 모른 채
 * 로그아웃되고, 서버에 있던 기록에 다시 닿지 못한다. 다만 **구글을 붙인
 * 계정은 다시 만들지 않는다** — 그건 진짜 로그아웃이고, 익명 계정을 새로
 * 만들어 주면 남의 기록처럼 텅 빈 화면을 보게 된다.
 */
class SessionHolder(
    private val auth: SupabaseAuth,
    private val store: AuthSessionStore,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    // 여러 화면이 동시에 요청하면 갱신이 겹친다. 겹치면 한쪽의 refresh token 이
    // 무효가 되어 그 요청부터 로그아웃된다. 한 번에 하나만 들어가게 한다.
    private val mutex = Mutex()

    /** 쓸 수 있는 출입증. 없으면 만들고, 만료가 가까우면 갱신한다. */
    suspend fun accessToken(): TokenResult = mutex.withLock {
        val current = store.load()

        if (current == null) {
            return@withLock createAnonymous()
        }
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

            is AuthResult.Rejected -> {
                if (current.user?.isAnonymous == false) {
                    // 구글을 붙인 계정이다. 다시 로그인하게 해야 한다 —
                    // 새 익명 계정을 만들어 주면 남의 기록처럼 빈 화면을 본다.
                    TokenResult.SignInRequired(refreshed.reason)
                } else {
                    store.clear()
                    createAnonymous()
                }
            }

            is AuthResult.Retry -> TokenResult.Unavailable(refreshed.reason)
        }
    }

    /** 구글 계정을 붙인다. 성공하면 계정은 그대로고 신원만 추가된다. */
    suspend fun linkGoogle(idToken: String, nonce: String? = null): TokenResult = mutex.withLock {
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

    suspend fun isAnonymous(): Boolean = store.load()?.user?.isAnonymous ?: true

    private suspend fun createAnonymous(): TokenResult =
        when (val created = auth.signInAnonymously()) {
            is AuthResult.Ok -> {
                store.save(created.session)
                TokenResult.Ok(created.session.accessToken, created.session.user?.id)
            }
            // 익명 로그인이 대시보드에서 꺼져 있으면 여기로 온다.
            is AuthResult.Rejected -> TokenResult.SignInRequired(created.reason)
            is AuthResult.Retry -> TokenResult.Unavailable(created.reason)
        }
}

sealed interface TokenResult {
    data class Ok(val accessToken: String, val userId: String?) : TokenResult

    /** 지금은 안 되지만 나중에는 된다 — 네트워크·서버 문제 */
    data class Unavailable(val reason: String) : TokenResult

    /** 사용자가 직접 다시 로그인해야 한다 */
    data class SignInRequired(val reason: String) : TokenResult
}
