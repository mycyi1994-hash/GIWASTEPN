package com.giwa.strideup.data.remote

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
 * 것은 이 단계에서 감당할 위험이 아니고, 필요한 호출은 아래 셋뿐이다.
 *
 *  - 익명 가입    처음 앱을 연 사람에게 계정을 만들어 준다
 *  - 토큰 갱신    한 시간마다
 *  - 구글 연결    익명 계정에 구글을 붙여 정식 계정으로 만든다
 *
 * **익명 계정이 곧 게스트다.** 지금까지 게스트는 폰 안에만 있었지만, 이제
 * 처음 앱을 여는 순간 서버에 계정이 생기고 기록이 거기 쌓인다. 나중에 구글을
 * 붙여도 **같은 계정**이라 그동안 모은 것이 그대로 남는다 — 따로 옮기는
 * 과정이 없으니 옮기다 잃을 일도 없다.
 */
class SupabaseAuth(
    private val baseUrl: String,
    private val apiKey: String,
    private val http: HttpPoster = UrlConnectionPoster(),
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    private val authUrl get() = "${baseUrl.trimEnd('/')}/auth/v1"

    /** 처음 앱을 연 사람에게 익명 계정을 만들어 준다. */
    suspend fun signInAnonymously(): AuthResult =
        post("$authUrl/signup", "{}")

    /** 만료된 출입증을 새로 받는다. */
    suspend fun refresh(refreshToken: String): AuthResult =
        post(
            "$authUrl/token?grant_type=refresh_token",
            """{"refresh_token":${refreshToken.asJsonString()}}""",
        )

    /**
     * 구글 계정을 붙인다.
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
