package com.stepup.android.data.repo

import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.AuthSession
import com.stepup.android.data.remote.AuthSessionStore
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * 로그인 세션을 기기 저장소에 담아 둔다.
 *
 * 통째로 JSON 한 덩어리로 저장한다. 필드를 하나씩 나눠 두면 저장하다 중간에
 * 죽었을 때 토큰은 새 것인데 만료 시각은 옛 것인 상태가 만들어진다. 한 덩어리면
 * 있거나 없거나 둘 중 하나다.
 */
class PrefsAuthSessionStore(private val prefs: UserPrefs) : AuthSessionStore {

    override suspend fun load(): AuthSession? {
        val raw = prefs.authSessionJson()
        if (raw.isBlank()) return null
        // 저장된 모양이 옛 버전이라 못 읽으면 없는 것으로 친다. 여기서 예외를
        // 던지면 앱이 시작조차 못 한다 — 다시 로그인하는 편이 낫다.
        return runCatching { json.decodeFromString<AuthSession>(raw) }.getOrNull()
    }

    override suspend fun save(session: AuthSession) {
        prefs.setAuthSessionJson(json.encodeToString(AuthSession.serializer(), session))
    }

    override suspend fun clear() {
        prefs.clearAuthSession()
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
