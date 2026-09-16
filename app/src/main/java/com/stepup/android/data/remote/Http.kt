package com.stepup.android.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HTTP 응답.
 *
 * [status] 가 0이면 응답을 받지 못한 것이다 — 연결 실패, 시간 초과, 끊김.
 * 서버가 준 오류(4xx·5xx)와 구분해야 한다. 앞의 것은 나중에 다시 하면 되고,
 * 뒤의 것은 다시 해도 같은 답이 온다.
 */
data class HttpResponse(val status: Int, val body: String)

/**
 * 요청을 보내는 쪽.
 *
 * 인터페이스로 둔 것은 네트워크 없이 서버 응답을 흉내 내기 위해서다.
 * 토큰 갱신이나 재시도 규칙은 실기기에서 재현하기 어려운데, 틀리면 사용자가
 * 로그아웃되거나 기록을 잃는다.
 */
interface HttpPoster {
    suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse

    suspend fun get(url: String, headers: Map<String, String>): HttpResponse
}

/**
 * [HttpURLConnection] 으로 보내는 기본 구현.
 *
 * HTTP 라이브러리를 새로 들이지 않는다. 쓰는 엔드포인트가 몇 개뿐이고,
 * 지도 타일이 이미 같은 방식으로 동작한다.
 */
class UrlConnectionPoster(
    private val timeoutMillis: Int = 20_000,
) : HttpPoster {

    override suspend fun post(url: String, body: String, headers: Map<String, String>) =
        send(url, "POST", body, headers)

    override suspend fun get(url: String, headers: Map<String, String>) =
        send(url, "GET", null, headers)

    private suspend fun send(
        url: String,
        method: String,
        body: String?,
        headers: Map<String, String>,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            return@withContext HttpResponse(0, "연결할 수 없습니다: ${e.message}")
        }

        try {
            connection.requestMethod = method
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray()) }
            }

            val status = connection.responseCode
            // 4xx·5xx 본문은 errorStream 으로 온다. 여기서 흘리면 서버가 적어 보낸
            // 실패 이유가 사라져 "왜 안 되는지 모르는" 상태가 된다.
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            HttpResponse(status, text)
        } catch (e: IOException) {
            HttpResponse(0, "통신 실패: ${e.message}")
        } catch (e: Exception) {
            HttpResponse(0, "요청 실패: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }
}
