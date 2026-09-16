package com.stepup.android.domain

/**
 * 시각이 붙은 GPS 좌표.
 *
 * [GeoPoint]와 따로 두는 이유는 쓰임이 다르기 때문이다. 코스는 "어디를 지나는
 * 길인가"라는 모양만 필요하지만, 러닝 기록은 "언제 거기 있었는가"가 있어야
 * 구간 속도를 계산할 수 있다. 속도를 못 재면 그 세션이 사람이 뛴 것인지
 * 판정할 수 없고, 판정할 수 없는 세션은 온체인으로 청구할 수 없다.
 *
 * @property at epoch 밀리초. 어테스터(`attester/src/economy.js`)의 `track[].t`와
 *   같은 단위다 — 바꾸면 서버 판정이 조용히 틀어진다.
 */
data class TrackPoint(val lat: Double, val lng: Double, val at: Long) {
    fun toGeoPoint(): GeoPoint = GeoPoint(lat, lng)
}

/**
 * 러닝 경로를 한 줄 문자열로 담았다 꺼낸다.
 *
 * 형식은 `위도,경도,시각;위도,경도,시각` — 코스가 쓰는
 * [RunCourse.encode]와 같은 모양에 시각만 더했다. 바이너리로 줄일 수도 있지만,
 * 이 값은 나중에 서버로 올라가 분쟁의 근거가 된다. 사람이 열어서 읽을 수
 * 있다는 점이 몇 KB보다 가치 있다.
 *
 * 좌표는 소수점 6자리로 끊는다. 위도 1e-6도는 약 11cm라 GPS 오차(수 m)보다
 * 훨씬 정밀하다. 그 아래 자릿수는 정보가 아니라 잡음이다.
 */
object RunTrack {

    /** 저장할 때 남길 좌표 소수점 자리수 */
    const val COORD_DECIMALS = 6

    private const val POINT_SEPARATOR = ';'
    private const val FIELD_SEPARATOR = ','

    private fun Double.trimmed(): String {
        val factor = 1_000_000.0 // 10^COORD_DECIMALS
        return (Math.round(this * factor) / factor).toString()
    }

    fun encode(points: List<TrackPoint>): String = points.joinToString(POINT_SEPARATOR.toString()) {
        "${it.lat.trimmed()}${FIELD_SEPARATOR}${it.lng.trimmed()}${FIELD_SEPARATOR}${it.at}"
    }

    /**
     * 저장된 문자열을 좌표 목록으로 되돌린다.
     *
     * 읽을 수 없는 조각은 예외를 던지지 않고 건너뛴다. 기록 화면을 여는 것이
     * 앱이 죽을 만한 일은 아니고, 한 점이 깨졌다고 나머지 경로까지 버릴 이유도
     * 없다. 실제로 쓸 수 있는 경로인지는 읽은 쪽이 개수로 판단하면 된다.
     */
    fun decode(raw: String): List<TrackPoint> {
        if (raw.isBlank()) return emptyList()
        return raw.split(POINT_SEPARATOR).mapNotNull { chunk ->
            val parts = chunk.split(FIELD_SEPARATOR)
            if (parts.size != 3) return@mapNotNull null
            val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
            val lng = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val at = parts[2].toLongOrNull() ?: return@mapNotNull null
            TrackPoint(lat, lng, at)
        }
    }
}

/** 시각을 떼고 모양만 넘긴다 — 지도 그리기와 코스 만들기가 쓰는 형태다. */
fun List<TrackPoint>.toGeoPoints(): List<GeoPoint> = map { it.toGeoPoint() }
