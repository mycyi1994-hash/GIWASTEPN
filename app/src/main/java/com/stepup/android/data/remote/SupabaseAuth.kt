package com.stepup.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 로그인 세션. 서버에 요청할 때 쓰는 출입증이다.
 *
 * [accessToken] 은 한 시간쯤 살고, 만료되면 [refreshToken] 으로 새로 받는다.
 * 둘 다 기기에 저장해야 앱을 껐다 켜도 로그인이 유지된다.
 */
@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    /** 남은 유효 시간(초) */
    @SerialName("expires_in") val expiresIn: Long = 3600,
    /** 만료 시각(epoch 초). 서버가 안 주면 발급 시각 + expiresIn 으로 채운다. */
    @SerialName("expires_at") val expiresAt: Long = 0,
    val user: AuthUser? = null,
) {
    /**
     * 곧 만료되는가.
     *
     * 딱 만료 시각에 맞춰 갱신하면 요청이 날아가는 중에 만료될 수 있다.
     * 미리 갱신해 그 틈을 없앤다.
     */
    fun needsRefresh(nowSeconds: Long, marginSeconds: Long = 120): Boolean =
        expiresAt <= 0 || nowSeconds >= expiresAt - marginSeconds
}

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
    /** 익명 계정인가. 구글 로그인을 붙이면 false 가 된다. */
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
)

/** 인증 요청의 결말 */
sealed interface AuthResult {
    data class Ok(val session: AuthSession) : AuthResult

    /** 설정이 틀렸거나 자격이 거절됐다. 다시 시도해도 같다. */
    data class Rejected(val reason: String) : AuthResult

    /** 네트워크나 서버 문제. 나중에 다시. */
    data class Retry(val reason: String) : AuthResult
}

/**
 * Supabase 인증.
 *
 * 라이브러리(supabase-kt)를 쓰지 않는다. 그쪽은 Kotlin 2.3 이상으로 빌드돼
 * 있어 이 프로젝트(2.0.21)의 컴파일러가 읽지 못한다. 툴체인 전체를 올리는
 * 것은 이 단계에서 감당할 위험이 아니고, 필요한 호출은 둘뿐이다.
 *
 *  - 구글 로그인   안드로이드가 준 ID 토큰을 세션으로 바꾼다
 *  - 토큰 갱신     한 시간마다
 *
 * **로그인 수단은 구글 하나다.** 익명 계정을 두지 않는 이유는 신원을 하나로
 * 묶어 두기 위해서다. 지갑(WalletConnect)은 v2 에서 "출금할 곳"으로 들어오지
 * 신원으로 들어오지 않는다 — 지갑이 곧 신원이면 지갑을 잃는 순간 계정도 잃는다.
 */
class SupabaseAuth(
    private val baseUrl: String,
    private val apiKey: String,
    private val http: HttpPoster = UrlConnectionPoster(),
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    private val authUrl get() = "${baseUrl.trimEnd('/')}/auth/v1"

    /** 만료된 출입증을 새로 받는다. */
    suspend fun refresh(refreshToken: String): AuthResult =
        post(
            "$authUrl/token?grant_type=refresh_token",
            """{"refresh_token":${refreshToken.asJsonString()}}""",
        )

    /**
     * 구글로 로그인한다.
     *
     * 브라우저를 띄우는 방식이 아니라 안드로이드가 준 ID 토큰을 그대로 보내는
     * 방식이다. 앱을 벗어나지 않으므로 로그인 도중에 이탈하는 사람이 적다.
     */
    suspend fun signInWithGoogle(idToken: String, nonce: String? = null): AuthResult {
        val body = buildString {
            append("""{"provider":"google","id_token":""")
            append(idToken.asJsonString())
            if (!nonce.isNullOrBlank()) {
                append(""","nonce":""")
                append(nonce.asJsonString())
            }
            append("}")
        }
        return post("$authUrl/token?grant_type=id_token", body)
    }

    private suspend fun post(url: String, body: String): AuthResult {
        if (!isConfigured) return AuthResult.Retry("서버 주소가 설정되지 않았습니다")

        val response = http.post(
            url = url,
            body = body,
            headers = mapOf(
                "apikey" to apiKey,
                "Content-Type" to "application/json",
            ),
        )

        return when {
            response.status in 200..299 -> {
                val session = runCatching { json.decodeFromString<AuthSession>(response.body) }
                    .getOrNull()
                    ?: return AuthResult.Retry("응답을 이해할 수 없습니다")
                AuthResult.Ok(session.withExpiryFilled(now()))
            }

            // 400 은 대개 설정 문제다 — 익명 로그인이 꺼져 있거나 토큰이 틀렸다.
            // 401·403 은 키가 틀렸다. 어느 쪽도 재시도로 풀리지 않는다.
            response.status in 400..499 && response.status != 429 ->
                AuthResult.Rejected(response.errorMessage())

            response.status == 0 -> AuthResult.Retry(response.body)

            else -> AuthResult.Retry("서버 오류 (${response.status})")
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * 서버가 만료 시각을 안 줬으면 채워 넣는다.
 *
 * `expires_in`(남은 초)만 오는 경우가 있는데, 그대로 두면 앱이 언제 갱신해야
 * 하는지 알 수 없다. 받은 시점을 기준으로 절대 시각을 만들어 둔다.
 */
internal fun AuthSession.withExpiryFilled(nowSeconds: Long): AuthSession =
    if (expiresAt > 0) this else copy(expiresAt = nowSeconds + expiresIn)

/** JSON 문자열 리터럴로 감싼다 — 토큰에 따옴표나 역슬래시가 들어가도 깨지지 않게. */
internal fun String.asJsonString(): String =
    Json.encodeToString(String.serializer(), this)

private fun HttpResponse.errorMessage(): String {
    // Supabase 는 실패 이유를 본문에 담아 보낸다. 흘리면 "왜 안 되는지 모르는" 상태가 된다.
    val obj = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject
    val message = listOf("error_description", "msg", "message", "error")
        .firstNotNullOfOrNull { key -> (obj?.get(key) as? JsonPrimitive)?.contentOrNull }
    return message ?: "요청이 거절되었습니다 ($status)"
}
