package com.giwa.strideup.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.giwa.strideup.BuildConfig
import com.giwa.strideup.domain.GeoPoint
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 실제 지도 타일 — 러닝 경로를 진짜 도로 위에 얹기 위한 최소 구현.
 *
 * ## 왜 지도 SDK를 쓰지 않는가
 *
 * Google Maps SDK는 API 키와 결제 계정이 있어야 타일이 나온다. 키 없이 넣으면
 * 회색 화면만 뜨므로, 키가 없는 상태에서도 **지금 당장 동작하는** 쪽을 택했다.
 * OpenStreetMap 래스터 타일은 키가 필요 없고, 우리가 필요한 것(내가 달린 길이
 * 어느 도로였는지)을 그대로 보여준다.
 *
 * 지도를 크게 보거나 길 안내가 필요할 때는 [com.giwa.strideup.core.ExternalIntents]
 * 가 구글 지도로 넘긴다. 앱 안은 OSM, 앱 밖은 구글 지도 — 각자 잘하는 일을 맡긴다.
 *
 * ## OSM 타일 정책
 *
 * 타일 서버는 자원봉사로 운영된다. 정책이 요구하는 것을 지킨다.
 *  - 앱을 식별할 수 있는 User-Agent를 보낸다
 *  - 한 화면에 필요한 만큼만 받고([MAX_TILES] 상한), 대량 선인출을 하지 않는다
 *  - 메모리·디스크에 캐시해서 같은 타일을 다시 받지 않는다
 */
object MapTiles {

    const val TILE_SIZE = 256

    /** 한 화면에 받을 타일 상한. 이보다 많이 필요하면 줌을 낮춘다. */
    const val MAX_TILES = 24

    /** 경로가 한 점뿐일 때의 기본 줌 */
    const val DEFAULT_ZOOM = 16

    const val MIN_ZOOM = 3
    const val MAX_ZOOM = 18

    private const val TILE_URL = "https://tile.openstreetmap.org/%d/%d/%d.png"

    private val userAgent =
        "StepUp/${BuildConfig.VERSION_NAME} (Android; +https://github.com/mycyi1994-hash/GIWASTEPN)"

    /**
     * 타일 메모리 캐시 — **개수가 아니라 바이트로** 재는 것이 중요하다.
     * 256×256 ARGB 한 장이 256KB라, 개수로 96장을 잡으면 조용히 24MB를 쥔다.
     */
    private val memory = object : LruCache<String, ImageBitmap>(memoryBudgetBytes()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    private fun memoryBudgetBytes(): Int =
        (Runtime.getRuntime().maxMemory() / 8).coerceIn(4L * 1024 * 1024, 24L * 1024 * 1024).toInt()

    /**
     * 받아오다 실패한 타일과 그 시각.
     *
     * 실패를 영구히 기억하면 잠깐 끊긴 네트워크가 지도에 영구적인 검은 구멍을
     * 남긴다. [FAILURE_TTL_MS] 뒤에는 다시 시도하고, 404·403처럼 다시 시도해도
     * 소용없는 응답만 [PERMANENT] 로 못 박는다.
     */
    private val failed = ConcurrentHashMap<String, Long>()

    private const val FAILURE_TTL_MS = 60_000L
    private const val PERMANENT = Long.MAX_VALUE

    fun key(zoom: Int, x: Int, y: Int): String = "$zoom/$x/$y"

    fun cached(zoom: Int, x: Int, y: Int): ImageBitmap? = memory.get(key(zoom, x, y))

    /**
     * 타일 한 장을 가져온다. 메모리 → 디스크 → 네트워크 순.
     * 실패하면 null을 돌려주고, 호출부는 그 자리를 비워 둔 채 경로만 그린다.
     */
    suspend fun load(context: Context, zoom: Int, x: Int, y: Int): ImageBitmap? {
        val k = key(zoom, x, y)
        memory.get(k)?.let { return it }
        val failedAt = failed[k]
        if (failedAt != null) {
            if (failedAt == PERMANENT || SystemClock.elapsedRealtime() - failedAt < FAILURE_TTL_MS) return null
            failed.remove(k)
        }

        return withContext(Dispatchers.IO) {
            val file = diskFile(context, zoom, x, y)
            val fromDisk = runCatching {
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }.getOrNull()
            if (fromDisk != null) {
                val image = fromDisk.asImageBitmap()
                memory.put(k, image)
                return@withContext image
            }

            val fetched = runCatching { download(zoom, x, y) }.getOrNull()
            if (fetched == null || fetched.bytes == null) {
                // 404·403은 다시 받아도 없다. 타임아웃·5xx는 잠시 뒤 다시 시도한다.
                failed[k] = if (fetched?.permanent == true) PERMANENT else SystemClock.elapsedRealtime()
                return@withContext null
            }
            val bytes = fetched.bytes
            val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
            if (bitmap == null) {
                failed[k] = PERMANENT
                return@withContext null
            }
            runCatching {
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
                pruneDisk(context)
            }
            val image = bitmap.asImageBitmap()
            memory.put(k, image)
            image
        }
    }

    private fun diskFile(context: Context, zoom: Int, x: Int, y: Int): File =
        File(context.cacheDir, "tiles/$zoom/${x}_$y.png")

    private class Fetched(val bytes: ByteArray?, val permanent: Boolean)

    private fun download(zoom: Int, x: Int, y: Int): Fetched {
        // 로케일에 따라 %d가 아라비아 숫자가 아닌 글자를 뱉으면 URL이 깨진다
        val url = String.format(Locale.ROOT, TILE_URL, zoom, x, y)
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.setRequestProperty("User-Agent", userAgent)
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            val code = connection.responseCode
            when {
                code == HttpURLConnection.HTTP_OK ->
                    Fetched(connection.inputStream.use { it.readBytes() }, permanent = false)
                code == HttpURLConnection.HTTP_NOT_FOUND || code == HttpURLConnection.HTTP_FORBIDDEN ->
                    Fetched(null, permanent = true)
                else -> Fetched(null, permanent = false)
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 디스크 캐시가 무한히 자라지 않게 오래된 타일부터 지운다.
     * 쓸 때마다 전부 훑으면 낭비라, 몇 번에 한 번만 돈다.
     */
    private var writesSincePrune = 0

    @Synchronized
    private fun pruneDisk(context: Context) {
        if (++writesSincePrune < PRUNE_EVERY) return
        writesSincePrune = 0
        val dir = File(context.cacheDir, "tiles")
        val files = dir.walkTopDown().filter { it.isFile }.toMutableList()
        var total = files.sumOf { it.length() }
        if (total <= DISK_BUDGET_BYTES) return
        files.sortBy { it.lastModified() }
        for (f in files) {
            if (total <= DISK_BUDGET_BYTES) break
            val size = f.length()
            if (f.delete()) total -= size
        }
    }

    private const val DISK_BUDGET_BYTES = 30L * 1024 * 1024
    private const val PRUNE_EVERY = 40

    // ── 슬리피 맵 좌표 변환 ──────────────────────────────────────
    //
    // 웹 메르카토르. 경도는 선형, 위도는 로그 변환이라 극지방이 늘어난다.
    // 반환값은 "줌 z에서 세계 전체를 펼쳤을 때의 픽셀 좌표"다.

    fun worldX(lng: Double, zoom: Int): Double =
        (lng + 180.0) / 360.0 * worldSize(zoom)

    fun worldY(lat: Double, zoom: Int): Double {
        val s = sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
        return (0.5 - ln((1 + s) / (1 - s)) / (4 * PI)) * worldSize(zoom)
    }

    fun worldSize(zoom: Int): Double = (1 shl zoom).toDouble() * TILE_SIZE

    /**
     * 경로가 화면에 다 들어오는 가장 가까운 줌.
     *
     * 높은 줌부터 내려오며 첫 번째로 들어맞는 값을 고른다. 여백을 12% 남겨
     * 경로가 가장자리에 딱 붙지 않게 한다.
     */
    fun fitZoom(points: List<GeoPoint>, widthPx: Int, heightPx: Int): Int {
        if (points.isEmpty() || widthPx <= 0 || heightPx <= 0) return DEFAULT_ZOOM
        if (points.size == 1) return DEFAULT_ZOOM

        val minLat = points.minOf { it.lat }
        val maxLat = points.maxOf { it.lat }
        val minLng = points.minOf { it.lng }
        val maxLng = points.maxOf { it.lng }

        val usableW = widthPx * 0.88
        val usableH = heightPx * 0.88

        for (zoom in MAX_ZOOM downTo MIN_ZOOM) {
            val spanX = worldX(maxLng, zoom) - worldX(minLng, zoom)
            // y는 위도가 커질수록 작아지므로 뒤집어 뺀다
            val spanY = worldY(minLat, zoom) - worldY(maxLat, zoom)
            if (spanX <= usableW && spanY <= usableH) {
                // 실제로 받는 타일은 경로 span이 아니라 **뷰포트**를 덮는다.
                // 예산을 span으로 재면 상한이 상한 노릇을 못 한다.
                val tiles = tileCount(widthPx.toDouble(), heightPx.toDouble())
                if (tiles <= MAX_TILES) return zoom
            }
        }
        return MIN_ZOOM
    }

    private fun tileCount(spanX: Double, spanY: Double): Int {
        val cols = (spanX / TILE_SIZE).toInt() + 2
        val rows = (spanY / TILE_SIZE).toInt() + 2
        return max(1, cols * rows)
    }
}
