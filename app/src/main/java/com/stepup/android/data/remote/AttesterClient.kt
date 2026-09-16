// ⚠️ v2(온체인)용 — 지금은 쓰이지 않는다.
//
// v1 은 오프체인 포인트라 서명된 청구서가 필요 없다. 세션은 Supabase 의
// record_session() 으로 가고, 그 경로는 StepUpServer 가 맡는다.
//
// 이 파일과 ClaimApi.kt 는 온체인으로 갈 때 그대로 깨워 쓴다. 규격이 서버와
// 어긋나지 않는지는 attester/test/app-payload.test.js 가 계속 확인한다 —
// 안 쓰는 동안 조용히 썩는 것을 막기 위해서다.
// 계획: docs/LAUNCH-PLAN.md §9.5
package com.stepup.android.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 업로드 결과.
 *
 * "실패"를 두 가지로 나누는 것이 이 타입의 요점이다. 다시 시도해서 될 일과
 * 아무리 시도해도 안 될 일을 구분하지 못하면, 앱은 영원히 거절당할 세션을
 * 붙잡고 배터리를 태우거나 — 반대로 — 잠깐 끊긴 지하철 구간에서 멀쩡한
 * 러닝 기록을 버린다.
 */
sealed interface ClaimResult {

    /** 서명을 받았다. 이제 체인에 제출할 수 있다. */
    data class Signed(val claim: SignedClaim, val signature: String, val verdict: String) : ClaimResult

    /**
     * 서버가 이 세션을 거절했다. 다시 보내도 같은 답이 온다.
     *
     * 러닝으로 인정되지 않았거나(VOID), 이미 청구된 세션이거나, 형식이 틀렸다.
     */
    data class Rejected(val reason: String, val verdict: String?) : ClaimResult

    /**
     * 지금은 안 되지만 나중에는 될 수 있다.
     *
     * 네트워크가 끊겼거나, 서버가 잠깐 죽었거나, 오늘 배출 예산이 소진됐다.
     * 예산 소진은 내일이면 풀린다 — 이걸 영구 실패로 처리하면 사용자는
     * 정당하게 뛴 몫을 잃는다.
     */
    data class Retry(val reason: String) : ClaimResult
}

/**
 * 세션을 증명 서버로 보내는 쪽.
 *
 * 인터페이스로 둔 것은 업로드 규칙(무엇을 언제 다시 시도할지)을 실제 네트워크
 * 없이 검증하기 위해서다. 그 규칙이 틀리면 사용자가 정당하게 뛴 기록을 잃거나
 * 영원히 거절당할 요청으로 배터리를 태운다 — 실기기에서만 확인할 수 있는
 * 종류의 버그로 두기엔 대가가 크다.
 */
interface ClaimSubmitter {
    val isConfigured: Boolean
    suspend fun submit(request: ClaimRequest): ClaimResult
}

/**
 * 러닝 증명 서버에 세션을 올리고 서명된 청구서를 받아 온다.
 *
 * 주소를 생성자로 받는 이유는 이 앱이 아직 어디에 붙을지 정해지지 않았기
 * 때문이다. 지금은 어테스터 Worker 에 직접 붙지만, 백엔드가 생기면 그
 * 백엔드가 같은 규격으로 중계한다. 앱은 어느 쪽인지 알 필요가 없다.
 *
 * HTTP 클라이언트 라이브러리를 새로 들이지 않고 [HttpURLConnection] 을 쓴다.
 * 엔드포인트가 하나뿐이고, 이미 지도 타일이 같은 방식으로 동작한다.
 */
class AttesterClient(
    private val baseUrl: String,
    private val timeoutMillis: Int = 20_000,
) : ClaimSubmitter {

    private val json = Json {
        // 서버가 필드를 더 붙여도 앱이 죽지 않아야 한다. 서버는 앱보다 자주 바뀐다.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 주소가 없으면 올릴 곳도 없다. 호출부가 헛수고하지 않게 미리 알려준다. */
    override val isConfigured: Boolean get() = baseUrl.isNotBlank()

    override suspend fun submit(request: ClaimRequest): ClaimResult = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext ClaimResult.Retry("서버 주소가 설정되지 않았습니다")

        val connection = try {
            (URL("${baseUrl.trimEnd('/')}/claim").openConnection() as HttpURLConnection)
        } catch (e: Exception) {
            return@withContext ClaimResult.Retry("연결할 수 없습니다: ${e.message}")
        }

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            connection.outputStream.use { it.write(json.encodeToString(request).toByteArray()) }

            val status = connection.responseCode
            // 4xx·5xx 는 errorStream 으로 온다. 서버가 실패 이유를 본문에 담아
            // 보내므로, 여기서 흘리면 "왜 안 되는지 모르는" 상태가 된다.
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            parse(status, body)
        } catch (e: IOException) {
            ClaimResult.Retry("통신 실패: ${e.message}")
        } catch (e: Exception) {
            // 응답이 JSON이 아니거나(프록시가 가로챈 HTML 등) 예상 못 한 모양일 때.
            // 세션을 버리지는 않는다 — 서버 쪽 일시적 문제일 수 있다.
            ClaimResult.Retry("응답을 읽을 수 없습니다: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(status: Int, body: String): ClaimResult {
        val parsed = runCatching { json.decodeFromString<ClaimResponse>(body) }.getOrNull()
            ?: return if (status in 500..599) {
                ClaimResult.Retry("서버 오류 ($status)")
            } else {
                ClaimResult.Rejected("응답을 이해할 수 없습니다 ($status)", null)
            }

        if (parsed.ok) {
            val claim = parsed.claim
            val signature = parsed.signature
            // ok 인데 서명이 없으면 쓸 수 있는 결과가 아니다. 성공으로 기록해
            // 두면 그 세션은 영영 청구되지 않은 채 사라진다.
            if (claim == null || signature.isNullOrBlank()) {
                return ClaimResult.Retry("서명이 빠진 응답입니다")
            }
            return ClaimResult.Signed(claim, signature, parsed.verdict ?: "CLEAN")
        }

        val reason = parsed.error ?: "알 수 없는 오류 ($status)"
        return when (status) {
            // 400 형식 오류 · 409 이미 청구됨 · 422 러닝으로 인정되지 않음 —
            // 셋 다 같은 요청을 다시 보내봐야 같은 답이 온다.
            400, 409, 422 -> ClaimResult.Rejected(reason, parsed.verdict)
            // 429 는 오늘 배출 예산 소진. 내일이면 풀리므로 버리지 않는다.
            429 -> ClaimResult.Retry(reason)
            // 503 은 어테스터가 아직 설정되지 않은 상태다.
            else -> ClaimResult.Retry(reason)
        }
    }
}
