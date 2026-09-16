package com.stepup.android

import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.toGeoPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 러닝 경로 저장 규칙.
 *
 * 이 경로는 곧 서버로 올라가 "정말 뛰었는가"를 판정하는 근거가 된다. 저장했다
 * 꺼내는 사이에 좌표나 시각이 틀어지면, 판정이 조용히 잘못되고 그 결과가
 * 토큰 지급으로 이어진다. 그래서 왕복이 정확한지를 먼저 못 박는다.
 */
class RunTrackTest {

    /** 여의도 한강공원 근처 — 실제 좌표대와 자릿수를 맞춘 표본 */
    private val sample = listOf(
        TrackPoint(37.526_312, 126.930_145, 1_700_000_000_000L),
        TrackPoint(37.526_874, 126.930_988, 1_700_000_002_500L),
        TrackPoint(37.527_401, 126.931_733, 1_700_000_005_000L),
    )

    @Test
    fun `저장했다 꺼내도 좌표와 시각이 그대로다`() {
        val restored = RunTrack.decode(RunTrack.encode(sample))
        assertEquals(sample, restored)
    }

    @Test
    fun `시각은 밀리초 그대로 남는다 - 초로 깎이면 구간 속도가 틀어진다`() {
        val restored = RunTrack.decode(RunTrack.encode(sample))
        assertEquals(1_700_000_002_500L, restored[1].at)
    }

    @Test
    fun `좌표는 소수점 6자리까지 남는다 - GPS 오차보다 정밀하다`() {
        // 1e-6도 ≈ 11cm. 그 아래는 정보가 아니라 잡음이라 버린다.
        val noisy = listOf(TrackPoint(37.526_312_987_6, 126.930_145_123_4, 1_700_000_000_000L))
        val restored = RunTrack.decode(RunTrack.encode(noisy))
        assertEquals(37.526_313, restored[0].lat, 1e-9)
        assertEquals(126.930_145, restored[0].lng, 1e-9)
    }

    @Test
    fun `빈 경로는 빈 문자열로 오간다`() {
        assertEquals("", RunTrack.encode(emptyList()))
        assertEquals(emptyList<TrackPoint>(), RunTrack.decode(""))
        assertEquals(emptyList<TrackPoint>(), RunTrack.decode("   "))
    }

    @Test
    fun `깨진 조각은 건너뛰고 나머지는 살린다`() {
        // 기록 화면을 여는 것이 앱이 죽을 만한 일은 아니다. 한 점이 깨졌다고
        // 나머지 경로까지 버릴 이유도 없다.
        val broken = "37.5,126.9,1700000000000;쓰레기;37.6,127.0;37.7,127.1,1700000005000"
        val restored = RunTrack.decode(broken)
        assertEquals(2, restored.size)
        assertEquals(37.5, restored[0].lat, 1e-9)
        assertEquals(37.7, restored[1].lat, 1e-9)
    }

    @Test
    fun `모양만 뽑으면 좌표 수가 유지된다`() {
        val geo = sample.toGeoPoints()
        assertEquals(sample.size, geo.size)
        assertEquals(sample[0].lat, geo[0].lat, 1e-9)
        assertEquals(sample[0].lng, geo[0].lng, 1e-9)
    }

    @Test
    fun `한 시간짜리 러닝도 저장할 만한 크기다`() {
        // 2.5초 간격, 8m마다 한 점 — 한 시간이면 대략 이 정도가 쌓인다.
        val hour = (0 until 1_440).map {
            TrackPoint(37.5 + it * 0.000_08, 127.0 + it * 0.000_08, 1_700_000_000_000L + it * 2_500L)
        }
        val encoded = RunTrack.encode(hour)
        assertEquals(hour.size, RunTrack.decode(encoded).size)
        // SQLite 한 행에 넣기에 무리 없는 크기인지 못 박아 둔다.
        assertTrue("경로 문자열이 너무 크다: ${encoded.length}", encoded.length < 60_000)
    }
}
