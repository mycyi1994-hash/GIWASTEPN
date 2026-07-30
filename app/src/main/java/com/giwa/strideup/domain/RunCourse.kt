package com.giwa.strideup.domain

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 러닝 코스 — GPS로 기록한 실제 경로.
 *
 * 좌표는 문자열 하나로 압축해 보관한다("lat,lng;lat,lng;…").
 * 지도 SDK 없이도 [normalized]로 화면 좌표를 만들어 네온 코스맵을 그린다.
 */
data class GeoPoint(val lat: Double, val lng: Double)

data class RunCourse(
    val id: Long,
    val name: String,
    val area: String,
    val distanceKm: Double,
    val elevationM: Int,
    val points: List<GeoPoint>,
    val author: String,
    /** 내가 만든 코스 */
    val mine: Boolean,
    /** 코스 게시판에 공유했는지 */
    val shared: Boolean,
    val likes: Int,
    val liked: Boolean,
    val runCount: Int,
    val createdAt: Long,
) {
    /** 완주 보상(SUP) — 거리에 정비례한다 */
    val reward: Double get() = CourseRewards.forDistance(distanceKm)

    /** 좌표가 없으면 지도를 그릴 수 없다 */
    val hasTrack: Boolean get() = points.size >= 2

    /** 좌표를 0..1 화면 비율로 편다 — [normalizedTrack] 참고 */
    fun normalized(): List<Pair<Float, Float>> = points.normalizedTrack()

    fun encode(): String = points.joinToString(";") { "${it.lat},${it.lng}" }

    companion object {
        fun decode(raw: String): List<GeoPoint> = raw.split(';')
            .mapNotNull { chunk ->
                val parts = chunk.split(',')
                if (parts.size != 2) return@mapNotNull null
                val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                val lng = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                GeoPoint(lat, lng)
            }
    }
}

/** 완주 보상 규칙 — 거리 1km당 정액이라 어떤 코스든 계산이 투명하다. */
object CourseRewards {

    /** 1km 완주당 지급하는 SUP */
    const val SUP_PER_KM = 1.0

    /** 한 코스에서 받을 수 있는 최대 보상 (하프코스 이상은 동일) */
    const val MAX_REWARD = 42.0

    fun forDistance(distanceKm: Double): Double =
        (distanceKm * SUP_PER_KM).coerceIn(0.0, MAX_REWARD)
}

/**
 * 좌표를 0..1 화면 비율로 편다.
 * 위도·경도의 실제 거리 비율(경도는 cos(위도)만큼 좁다)을 반영하고,
 * 짧은 축을 가운데로 밀어 코스 모양이 찌그러지지 않게 한다.
 */
fun List<GeoPoint>.normalizedTrack(): List<Pair<Float, Float>> {
    if (size < 2) return emptyList()
    val lats = map { it.lat }
    val lngs = map { it.lng }
    val minLat = lats.min()
    val maxLat = lats.max()
    val minLng = lngs.min()
    val maxLng = lngs.max()
    val midLat = (minLat + maxLat) / 2
    val scaleLng = cos(Math.toRadians(midLat)).coerceAtLeast(0.05)

    val spanY = max(maxLat - minLat, 1e-6)
    val spanX = max((maxLng - minLng) * scaleLng, 1e-6)
    val span = max(spanX, spanY)
    val padX = (span - spanX) / 2
    val padY = (span - spanY) / 2

    return map { p ->
        val x = ((p.lng - minLng) * scaleLng + padX) / span
        // 화면 y는 아래로 증가하므로 위도를 뒤집는다
        val y = 1.0 - ((p.lat - minLat) + padY) / span
        x.toFloat().coerceIn(0f, 1f) to y.toFloat().coerceIn(0f, 1f)
    }
}

/** 좌표 사이 실제 거리(m) — 하버사인 */
fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val s = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) *
        sin(dLng / 2) * sin(dLng / 2)
    return r * 2 * atan2(sqrt(s), sqrt(1 - s))
}

/** 경로 전체 길이(km) */
fun List<GeoPoint>.trackDistanceKm(): Double {
    if (size < 2) return 0.0
    var total = 0.0
    for (i in 1 until size) total += haversineMeters(this[i - 1], this[i])
    return total / 1000
}

/**
 * 좌표를 솎아낸다 — 저장 크기를 줄이면서 코스 모양은 유지한다.
 * 직전에 남긴 점에서 [minMeters] 이상 움직였을 때만 새 점으로 인정한다.
 */
fun List<GeoPoint>.simplify(minMeters: Double = 12.0): List<GeoPoint> {
    if (size < 2) return this
    val out = mutableListOf(first())
    for (p in drop(1)) {
        if (haversineMeters(out.last(), p) >= minMeters) out.add(p)
    }
    if (out.size < 2) out.add(last())
    return out
}

/** 두 코스가 얼마나 겹치는지 아주 러프하게 — 시작점 근접도로 판단 */
fun RunCourse.startsNear(point: GeoPoint, meters: Double = 400.0): Boolean {
    val first = points.firstOrNull() ?: return false
    return haversineMeters(first, point) <= meters
}

/** 표시용 — 소수점 아래 자리가 흔들리지 않게 고정 */
fun formatKm(km: Double): String = "%.2f".format(min(abs(km), 999.99))
