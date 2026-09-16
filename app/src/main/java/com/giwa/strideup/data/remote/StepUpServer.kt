package com.giwa.strideup.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 서버가 세션을 받아 내린 결론.
 *
 * 적립액은 **서버가 계산한 값**이다. 앱이 보낸 금액이 아니다 —
 * `supabase/migrations/0003_ledger.sql` 의 `record_session()` 이 걸음 수를
 * 받아 직접 계산한다. 그래서 앱이 예상한 금액과 다를 수 있고, 다르면
 * 서버 쪽이 맞다.
 */
@Serializable
data class SessionRecorded(
    @SerialName("session_id") val sessionId: Long,
    /** CLEAN · FLAGGED · VOID */
    val verdict: String,
    @SerialName("points_awarded") val pointsAwarded: Double,
    /** 이 세션까지 반영한 잔고 */
    val balance: Double,
)

@Serializable
private data class BalanceRow(val balance: Double)

/** 서버 호출의 결말 */
sealed interface ServerResult<out T> {
    data class Ok<T>(val value: T) : ServerResult<T>

    /** 서버가 거절했다. 다시 보내도 같다. */
    data class Rejected(val reason: String) : ServerResult<Nothing>

    /** 지금은 안 되지만 나중에는 된다. */
    data class Retry(val reason: String) : ServerResult<Nothing>

    /** 사용자가 다시 로그인해야 한다. */
    data class SignInRequired(val reason: String) : ServerResult<Nothing>
}

/**
 * StepUp 서버(Supabase).
 *
 * 부르는 것은 둘뿐이다 — 세션을 기록하고, 잔고를 읽는다. 표에 직접 쓰지
 * 않는 이유는 앱에 박힌 키를 누구나 꺼낼 수 있기 때문이다. 기록은 서버
 * 함수만 할 수 있고, 그 함수가 금액을 직접 정한다.
 */
class StepUpServer(
    private val baseUrl: String,
    private val apiKey: String,
    private val sessions: SessionHolder,
    private val http: HttpPoster = UrlConnectionPoster(),
) {

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    private val restUrl get() = "${baseUrl.trimEnd('/')}/rest/v1"

    /**
     * 러닝 세션을 서버에 기록한다.
     *
     * 같은 세션을 다시 보내도 안전하다. 서버가 (사용자, 시작시각)으로
     * 중복을 걸러 내고 원래 결과를 그대로 돌려준다 — 지하철에서 응답을
     * 못 받고 재시도하는 일이 흔하기 때문이다.
     */
    suspend fun recordSession(
        startedAtMillis: Long,
        endedAtMillis: Long,
        steps: Int,
        durationSec: Long,
        track: String,
        boostBps: Int,
        partySize: Int,
    ): ServerResult<SessionRecorded> {
        val body = jsonBody {
            put("p_started_at", startedAtMillis.toIsoInstant())
            put("p_ended_at", endedAtMillis.toIsoInstant())
            put("p_steps", steps)
            put("p_duration_sec", durationSec)
            put("p_track", track)
            put("p_boost_bps", boostBps)
            put("p_party_size", partySize)
        }

        return authed { token ->
            http.post(
                url = "$restUrl/rpc/record_session",
                body = body,
                headers = headers(token),
            )
        }.map { text ->
            // 이 함수는 표를 돌려주므로 배열로 온다. 행이 없으면 뭔가 잘못된 것이다.
            json.decodeFromString<List<SessionRecorded>>(text).firstOrNull()
        }
    }

    /** 지금 잔고. 서버 원장의 합이다. */
    suspend fun balance(): ServerResult<Double> =
        authed { token ->
            http.get(
                url = "$restUrl/sup_balances?select=balance",
                headers = headers(token),
            )
        }.map { text ->
            // 아직 한 번도 적립한 적이 없으면 행이 없다. 그건 오류가 아니라 0이다.
            json.decodeFromString<List<BalanceRow>>(text).firstOrNull()?.balance ?: 0.0
        }

    // ── 공통 ────────────────────────────────────────────────────────

    private fun headers(token: String) = mapOf(
        "apikey" to apiKey,
        "Authorization" to "Bearer $token",
        "Content-Type" to "application/json",
    )

    /** 출입증을 챙겨서 요청하고, 응답을 결말로 옮긴다. */
    private suspend fun authed(call: suspend (String) -> HttpResponse): ServerResult<String> {
        if (!isConfigured) return ServerResult.Retry("서버 주소가 설정되지 않았습니다")

        val token = when (val t = sessions.accessToken()) {
            is TokenResult.Ok -> t.accessToken
            is TokenResult.Unavailable -> return ServerResult.Retry(t.reason)
            is TokenResult.SignInRequired -> return ServerResult.SignInRequired(t.reason)
        }

        val response = call(token)
        return when {
            response.status in 200..299 -> ServerResult.Ok(response.body)
            response.status == 0 -> ServerResult.Retry(response.body)
            // 출입증이 방금 만료됐을 수 있다. 다음 차례에 갱신해서 다시 시도한다.
            response.status == 401 -> ServerResult.Retry("인증이 만료되었습니다")
            // 429 는 요청이 몰린 것, 5xx 는 서버 문제 — 둘 다 나중에 다시.
            response.status == 429 || response.status >= 500 ->
                ServerResult.Retry("서버가 바쁩니다 (${response.status})")
            else -> ServerResult.Rejected(response.postgrestMessage())
        }
    }

    private fun <T> ServerResult<String>.map(transform: (String) -> T?): ServerResult<T> =
        when (this) {
            is ServerResult.Ok -> {
                val parsed = runCatching { transform(value) }.getOrNull()
                if (parsed == null) ServerResult.Retry("응답을 이해할 수 없습니다")
                else ServerResult.Ok(parsed)
            }
            is ServerResult.Rejected -> this
            is ServerResult.Retry -> this
            is ServerResult.SignInRequired -> this
        }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * Postgres 가 보낸 실패 이유를 꺼낸다.
 *
 * 함수에서 raise 한 메시지(예: "SUP가 부족합니다")가 여기 들어 있다.
 * 흘리면 사용자에게 "알 수 없는 오류"만 보여주게 된다.
 */
private fun HttpResponse.postgrestMessage(): String {
    val parsed = runCatching {
        Json { ignoreUnknownKeys = true }.parseToJsonElement(body)
    }.getOrNull() as? kotlinx.serialization.json.JsonObject
    val message = listOf("message", "hint", "details")
        .firstNotNullOfOrNull { key ->
            (parsed?.get(key) as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
        }
    return message ?: "요청이 거절되었습니다 ($status)"
}

/** epoch 밀리초를 Postgres 가 읽는 ISO-8601 UTC 문자열로. */
internal fun Long.toIsoInstant(): String = java.time.Instant.ofEpochMilli(this).toString()

/** 손으로 JSON 을 만들 때 따옴표·역슬래시로 깨지지 않게. */
private fun jsonBody(build: MutableMap<String, Any>.() -> Unit): String {
    val map = LinkedHashMap<String, Any>().apply(build)
    return map.entries.joinToString(",", "{", "}") { (key, value) ->
        val encoded = when (value) {
            is String -> value.asJsonString()
            is Number -> value.toString()
            is Boolean -> value.toString()
            else -> value.toString().asJsonString()
        }
        "${key.asJsonString()}:$encoded"
    }
}
