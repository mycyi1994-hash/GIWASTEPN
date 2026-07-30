package com.giwa.strideup.data.repo

import com.giwa.strideup.data.local.CourseDao
import com.giwa.strideup.data.local.CourseEntity
import com.giwa.strideup.data.local.NotificationType
import com.giwa.strideup.data.local.RewardType
import com.giwa.strideup.data.prefs.UserPrefs
import com.giwa.strideup.domain.CourseRewards
import com.giwa.strideup.domain.GeoPoint
import com.giwa.strideup.domain.RunCourse
import com.giwa.strideup.domain.simplify
import com.giwa.strideup.domain.trackDistanceKm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 러닝 코스 — 선택 · 내 코스 등록 · 게시판 공유 · 완주 보상.
 *
 * 백엔드가 없으므로 "게시판"은 shared 플래그가 켜진 로컬 코스들이고,
 * 첫 실행 때 실제 서울 러닝 명소를 본뜬 데모 코스를 심는다.
 */
class CourseRepository(
    private val dao: CourseDao,
    private val prefs: UserPrefs,
    private val rewardRepository: RewardRepository,
) {

    val courses: Flow<List<RunCourse>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 지금 달리기로 고른 코스 */
    val selectedCourse: Flow<RunCourse?> = combine(courses, prefs.selectedCourseId) { list, id ->
        list.firstOrNull { it.id == id }
    }

    suspend fun select(id: Long) = prefs.setSelectedCourse(id)

    suspend fun clearSelection() = prefs.setSelectedCourse(-1L)

    suspend fun selectedCourseNow(): RunCourse? {
        val id = prefs.selectedCourseNow()
        if (id < 0) return null
        return dao.byId(id)?.toDomain()
    }

    /** 방금 달린 GPS 트랙을 코스로 등록한다. @return 새 코스 id (트랙이 짧으면 null) */
    suspend fun create(
        name: String,
        area: String,
        track: List<GeoPoint>,
        shared: Boolean,
    ): Long? {
        val slim = track.simplify()
        val km = slim.trackDistanceKm()
        if (slim.size < 2 || km < 0.2) return null
        val id = dao.insert(
            CourseEntity(
                name = name.trim().ifEmpty { "Course" },
                area = area.trim(),
                distanceKm = km,
                // 고도 센서가 없으므로 데모 추정치 — 1km당 완만한 8m
                elevationM = (km * 8).toInt(),
                track = slim.joinToString(";") { "${it.lat},${it.lng}" },
                author = "",
                mine = true,
                shared = shared,
                likes = 0,
                liked = false,
                runCount = 0,
                createdAt = System.currentTimeMillis(),
            )
        )
        return id
    }

    suspend fun setShared(id: Long, shared: Boolean) {
        val entity = dao.byId(id) ?: return
        if (!entity.mine) return
        dao.update(entity.copy(shared = shared))
    }

    suspend fun toggleLike(id: Long) {
        val entity = dao.byId(id) ?: return
        dao.update(
            entity.copy(
                liked = !entity.liked,
                likes = (entity.likes + if (entity.liked) -1 else 1).coerceAtLeast(0),
            )
        )
    }

    suspend fun delete(id: Long) {
        dao.deleteMine(id)
        if (prefs.selectedCourseNow() == id) prefs.setSelectedCourse(-1L)
    }

    /**
     * 코스 완주 정산 — 거리 1km당 [CourseRewards.SUP_PER_KM] SUP 정량 지급.
     * 세션 거리가 코스 거리의 98% 이상이면 완주로 인정한다(GPS 오차 허용).
     */
    suspend fun grantCompletionIfFinished(sessionKm: Double): RunCourse? {
        val course = selectedCourseNow() ?: return null
        if (sessionKm < course.distanceKm * 0.98) return null
        rewardRepository.credit(
            RewardType.EARN_EVENT,
            course.reward,
            "코스 완주: ${course.name}",
        )
        rewardRepository.notify(
            type = NotificationType.COURSE_COMPLETE,
            argText = course.name,
            argAmount = course.reward,
        )
        dao.byId(course.id)?.let { dao.update(it.copy(runCount = it.runCount + 1)) }
        return course
    }

    suspend fun ensureSeeded() {
        if (dao.count() > 0) return
        dao.insertAll(seedCourses())
    }

    // ── 데모 시드 — 서울의 실제 러닝 명소를 본뜬 좌표 ─────────────
    private fun seedCourses(): List<CourseEntity> {
        val now = System.currentTimeMillis()

        fun course(
            name: String,
            area: String,
            author: String,
            likes: Int,
            runs: Int,
            points: List<GeoPoint>,
            hoursAgo: Int,
        ): CourseEntity {
            val km = points.trackDistanceKm()
            return CourseEntity(
                name = name,
                area = area,
                distanceKm = km,
                elevationM = (km * 8).toInt(),
                track = points.joinToString(";") { "${it.lat},${it.lng}" },
                author = author,
                mine = false,
                shared = true,
                likes = likes,
                liked = false,
                runCount = runs,
                createdAt = now - hoursAgo * 3_600_000L,
            )
        }

        return listOf(
            course(
                name = "여의도 한강 루프", area = "여의도", author = "Sora K.",
                likes = 128, runs = 342, hoursAgo = 96,
                points = listOf(
                    GeoPoint(37.5268, 126.9165), GeoPoint(37.5289, 126.9204),
                    GeoPoint(37.5301, 126.9251), GeoPoint(37.5312, 126.9302),
                    GeoPoint(37.5318, 126.9346), GeoPoint(37.5301, 126.9382),
                    GeoPoint(37.5275, 126.9394), GeoPoint(37.5252, 126.9367),
                    GeoPoint(37.5243, 126.9315), GeoPoint(37.5238, 126.9262),
                    GeoPoint(37.5241, 126.9210), GeoPoint(37.5253, 126.9172),
                    GeoPoint(37.5268, 126.9165),
                ),
            ),
            course(
                name = "서울숲 순환 코스", area = "성수", author = "Marco P.",
                likes = 86, runs = 205, hoursAgo = 150,
                points = listOf(
                    GeoPoint(37.5432, 127.0357), GeoPoint(37.5446, 127.0382),
                    GeoPoint(37.5459, 127.0411), GeoPoint(37.5452, 127.0442),
                    GeoPoint(37.5434, 127.0456), GeoPoint(37.5415, 127.0447),
                    GeoPoint(37.5404, 127.0419), GeoPoint(37.5407, 127.0388),
                    GeoPoint(37.5419, 127.0365), GeoPoint(37.5432, 127.0357),
                ),
            ),
            course(
                name = "남산 야경 업힐", area = "남산", author = "Elena R.",
                likes = 74, runs = 118, hoursAgo = 220,
                points = listOf(
                    GeoPoint(37.5512, 126.9882), GeoPoint(37.5527, 126.9904),
                    GeoPoint(37.5541, 126.9931), GeoPoint(37.5552, 126.9962),
                    GeoPoint(37.5546, 126.9995), GeoPoint(37.5530, 127.0012),
                    GeoPoint(37.5511, 127.0003), GeoPoint(37.5499, 126.9974),
                    GeoPoint(37.5497, 126.9938), GeoPoint(37.5503, 126.9905),
                    GeoPoint(37.5512, 126.9882),
                ),
            ),
            course(
                name = "반포 달빛 러닝", area = "반포", author = "Kai W.",
                likes = 143, runs = 276, hoursAgo = 40,
                points = listOf(
                    GeoPoint(37.5093, 126.9925), GeoPoint(37.5104, 126.9968),
                    GeoPoint(37.5113, 127.0012), GeoPoint(37.5121, 127.0058),
                    GeoPoint(37.5128, 127.0103), GeoPoint(37.5119, 127.0141),
                    GeoPoint(37.5098, 127.0128), GeoPoint(37.5089, 127.0084),
                    GeoPoint(37.5081, 127.0038), GeoPoint(37.5074, 126.9991),
                    GeoPoint(37.5081, 126.9948), GeoPoint(37.5093, 126.9925),
                ),
            ),
            course(
                name = "올림픽공원 5K", area = "송파", author = "Aiko T.",
                likes = 97, runs = 231, hoursAgo = 310,
                points = listOf(
                    GeoPoint(37.5188, 127.1170), GeoPoint(37.5206, 127.1201),
                    GeoPoint(37.5222, 127.1236), GeoPoint(37.5230, 127.1275),
                    GeoPoint(37.5221, 127.1311), GeoPoint(37.5199, 127.1323),
                    GeoPoint(37.5177, 127.1305), GeoPoint(37.5165, 127.1268),
                    GeoPoint(37.5163, 127.1228), GeoPoint(37.5173, 127.1192),
                    GeoPoint(37.5188, 127.1170),
                ),
            ),
        )
    }
}

/** Room 엔티티 → 도메인 모델 */
fun CourseEntity.toDomain(): RunCourse = RunCourse(
    id = id,
    name = name,
    area = area,
    distanceKm = distanceKm,
    elevationM = elevationM,
    points = RunCourse.decode(track),
    author = author,
    mine = mine,
    shared = shared,
    likes = likes,
    liked = liked,
    runCount = runCount,
    createdAt = createdAt,
)
