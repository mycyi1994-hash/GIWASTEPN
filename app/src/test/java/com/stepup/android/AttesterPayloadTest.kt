package com.stepup.android

import com.stepup.android.data.remote.ClaimRequest
import com.stepup.android.data.remote.ClaimResponse
import com.stepup.android.data.remote.TrackPointDto
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.TrackPoint
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ⚠️ v2(온체인)용 — v1 에서는 이 경로를 쓰지 않는다.
 *
 * 앱이 어테스터에 보내는 본문의 모양을 못 박는다. 지금은 잠들어 있지만,
 * 안 쓰는 동안 조용히 어긋나면 온체인으로 갈 때 원인을 찾기 어렵다.
 * 같은 본문을 서버 쪽에서도 확인한다 — `attester/test/app-payload.test.js`.
 */
class AttesterPayloadTest {

    /** 5초 간격 15m — 시속 10.8km 의 조깅. 어테스터 쪽 표본과 같은 좌표다. */
    private val track = RunTrack.encode(
        listOf(
            TrackPoint(37.526_312, 126.930_145, 1_700_000_000_000L),
            TrackPoint(37.526_447, 126.930_145, 1_700_000_005_000L),
        ),
    )

    @Test
    fun `보내는 JSON의 이름이 서버가 읽는 이름과 같다`() {
        // 서버(attester/src/economy.js)는 좌표의 시각을 t 로 읽는다. 앱 안에서는
        // at 이라 부르므로, 직렬화가 이름을 바꿔 주지 않으면 모든 구간의 시각이
        // 0이 되고 속도 계산이 통째로 틀어진다.
        val request = ClaimRequest(
            runner = "0x1111111111111111111111111111111111111111",
            startedAt = 1_700_000_000_000L,
            endedAt = 1_700_000_600_000L,
            steps = 2_000,
            boostBps = 1_200,
            partySize = 2,
            track = RunTrack.decode(track).map { TrackPointDto(it.lat, it.lng, it.at) },
        )
        val encoded = Json.encodeToString(request)

        assertTrue("좌표 시각은 t 여야 한다: $encoded", encoded.contains("\"t\":1700000000000"))
        assertTrue(encoded.contains("\"runner\":"))
        assertTrue(encoded.contains("\"startedAt\":"))
        assertTrue(encoded.contains("\"endedAt\":"))
        assertTrue(encoded.contains("\"boostBps\":"))
        assertTrue(encoded.contains("\"partySize\":"))
        assertTrue(encoded.contains("\"lat\":"))
        assertTrue(encoded.contains("\"lng\":"))
        // 앱 내부 이름이 새어 나가면 서버는 그 필드를 무시하고 기본값을 쓴다.
        assertTrue("내부 이름 at 이 나가면 안 된다: $encoded", !encoded.contains("\"at\":"))
    }

    @Test
    fun `서버가 모르는 필드를 더 붙여도 앱이 죽지 않는다`() {
        // 서버는 앱보다 자주 바뀐다. 필드가 하나 늘었다고 이미 설치된 앱이
        // 응답을 못 읽으면, 그 사용자의 세션은 영영 올라가지 않는다.
        val json = Json { ignoreUnknownKeys = true }
        val body = """
            {"ok":true,"verdict":"CLEAN","newFieldFromFuture":123,
             "claim":{"runner":"0x1","sessionHash":"0x2","amount":"5","day":1,"deadline":2},
             "signature":"0x3"}
        """.trimIndent()
        val parsed = json.decodeFromString<ClaimResponse>(body)

        assertTrue(parsed.ok)
        assertEquals("0x3", parsed.signature)
        assertEquals("5", parsed.claim?.amount)
        assertNull(parsed.error)
    }
}
