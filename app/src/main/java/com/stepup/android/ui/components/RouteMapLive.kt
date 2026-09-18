package com.stepup.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.stepup.android.domain.GeoPoint
import com.stepup.android.ui.theme.Volt
import kotlin.math.floor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 진짜 지도 위의 러닝 경로.
 *
 * OpenStreetMap 래스터 타일을 깔고 그 위에 볼트 네온 경로를 얹는다. 내가 달린
 * 길이 실제로 어느 도로였는지, 어느 공원을 돌았는지가 그대로 보인다.
 *
 * 타일은 원본이 밝은 지도라 그대로 쓰면 순블랙 테마와 부딪힌다. 색을 반전시켜
 * 어두운 지도로 바꾸고 채도를 죽여, 네온 경로만 화면에서 튀어나오게 한다.
 *
 * 의사 도로망([drawStreets])은 **항상 먼저 깔린다.** 타일이 도착하는 대로 그
 * 위를 덮으므로, 로딩 중에도 타일이 빠진 자리에도 검은 구멍이 생기지 않는다.
 */
@Composable
fun LiveRouteMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    /** 타일이 없는 자리에 깔 의사 도로망의 시드. 세션 내내 고정된 값을 넘겨야 한다. */
    seed: Int = 0,
    /** 0..1 — 경로 위 진행 지점에 러너 점을 찍는다. null이면 표시하지 않음 */
    progress: Float? = null,
    /**
     * 손가락으로 확대·축소·이동할 수 있게 한다.
     *
     * 목록 안에 들어가는 지도는 끄는 편이 낫다 — 지도를 잡으려다 목록이
     * 안 넘어가면 그게 더 답답하다. 러닝 화면처럼 지도가 주인공인 자리에서만 켠다.
     */
    interactive: Boolean = false,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    // 사용자가 손가락으로 만든 줌·이동. 경로에 맞춘 기본값에서 얼마나
    // 벗어났는지를 담는다 — 절대 위치가 아니라 차이로 두면, 달리는 동안
    // 경로가 늘어나도 보고 있던 자리가 유지된다.
    var zoomDelta by rememberSaveable { mutableIntStateOf(0) }
    var panX by rememberSaveable { mutableStateOf(0.0) }
    var panY by rememberSaveable { mutableStateOf(0.0) }
    // 손가락을 오므렸다 폈다 하는 동안 쌓이는 배율. 일정 선을 넘으면
    // 줌 한 단계로 환산한다. 타일은 정수 줌으로만 존재하기 때문이다.
    var pinch by remember { mutableFloatStateOf(1f) }
    val moved = zoomDelta != 0 || panX != 0.0 || panY != 0.0

    BoxWithConstraints(modifier) {
        val widthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
        val heightPx = if (constraints.hasBoundedHeight) constraints.maxHeight else 0

        val plan = remember(points, widthPx, heightPx, density, zoomDelta, panX, panY) {
            if (points.isEmpty() || widthPx <= 0 || heightPx <= 0) null
            else TilePlan.of(points, widthPx, heightPx, density, zoomDelta, panX, panY)
        }

        // 타일이 한 장 도착할 때마다 올라가는 카운터. Canvas가 이 값을 읽어
        // 스냅샷 의존성을 만들어 두므로, 도착이 곧 다시 그리기가 된다.
        var arrivals by remember { mutableIntStateOf(0) }

        // 달리는 동안 좌표는 계속 늘어나지만 대개 같은 타일 안이다. 효과를 좌표가
        // 아니라 **타일 범위**에 묶어, 몇 초마다 전체를 다시 받는 일을 막는다.
        val tileKey = plan?.rangeKey.orEmpty()

        LaunchedEffect(tileKey) {
            val current = plan ?: return@LaunchedEffect
            val wanted = buildList {
                for (ty in current.minTileY..current.maxTileY) {
                    for (tx in current.minTileX..current.maxTileX) {
                        add(current.wrapX(tx) to ty)
                    }
                }
            }.take(MapTiles.MAX_TILES)

            // 순차로 받으면 타임아웃 하나에 지도 전체가 멈춘다. 4개씩 병렬로 —
            // OSM 정책이 권하는 동시 연결 수 안이면서 체감이 확 달라진다.
            for (chunk in wanted.chunked(4)) {
                coroutineScope {
                    chunk.map { (tx, ty) ->
                        async { MapTiles.load(context, current.zoom, tx, ty) }
                    }.awaitAll()
                }
                arrivals++
            }
        }

        val scale = (density / 2f).coerceIn(1f, 2f)

        Canvas(
            Modifier
                .fillMaxSize()
                .then(
                    if (!interactive) {
                        Modifier
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                // 끈 거리는 화면 픽셀이다. 타일 좌표계로 옮겨야
                                // 줌이 바뀌어도 손가락과 지도가 같이 움직인다.
                                panX -= pan.x / scale
                                panY -= pan.y / scale

                                pinch *= gestureZoom
                                // 타일은 정수 줌만 있다. 2배쯤 벌리면 한 단계 올린다.
                                while (pinch > 1.8f && zoomDelta < MAX_USER_ZOOM_IN) {
                                    zoomDelta++
                                    pinch /= 2f
                                    // 줌이 한 단계 오르면 세계 좌표가 두 배가 된다.
                                    // 보고 있던 자리를 유지하려면 이동량도 같이 키운다.
                                    panX *= 2
                                    panY *= 2
                                }
                                while (pinch < 0.55f && zoomDelta > MAX_USER_ZOOM_OUT) {
                                    zoomDelta--
                                    pinch *= 2f
                                    panX /= 2
                                    panY /= 2
                                }
                            }
                        }
                    },
                ),
        ) {
            // arrivals를 읽어야 타일 도착이 다시 그리기로 이어진다
            val revision = arrivals

            // 폴백을 먼저 깔고 타일로 덮는다. 타일은 불투명이라 있는 자리는 가려지고,
            // 없는 자리는 도로망이 남는다 — 로딩 중에도 화면이 비지 않는다.
            drawStreets(seed)
            val drawn = if (plan != null && revision >= 0) drawTiles(plan) else 0
            if (drawn > 0) {
                // 지도를 한 겹 눌러 카드 배경과 붙인다
                drawRect(Color(0xFF060708).copy(alpha = 0.34f))
            }

            if (plan == null) return@Canvas

            if (points.size < 2) {
                val at = plan.toScreen(points.first())
                drawCircle(Volt.copy(alpha = 0.30f), radius = 9.dp.toPx(), center = at)
                drawCircle(Volt, radius = 4.5f.dp.toPx(), center = at)
                return@Canvas
            }

            val screen = points.map { plan.toScreen(it) }
            val path = Path().apply {
                moveTo(screen.first().x, screen.first().y)
                for (i in 1 until screen.size) lineTo(screen[i].x, screen[i].y)
            }

            // 글로우(넓고 옅게) → 본선(가늘고 진하게)
            drawPath(
                path,
                color = Volt.copy(alpha = 0.20f),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path,
                brush = Brush.linearGradient(listOf(Volt.copy(alpha = 0.85f), Volt)),
                style = Stroke(width = 3.5f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            drawCircle(Volt.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = screen.first())
            drawCircle(Color.White, radius = 3.dp.toPx(), center = screen.first())
            drawRouteFlag(screen.last())

            progress?.let { f ->
                val at = pointAlongRoute(screen, f.coerceIn(0f, 1f))
                drawCircle(Volt.copy(alpha = 0.30f), radius = 9.dp.toPx(), center = at)
                drawCircle(Volt, radius = 4.5f.dp.toPx(), center = at)
                drawCircle(Color(0xFF060708), radius = 2.dp.toPx(), center = at)
            }
        }

        if (interactive) {
            // ± 버튼. 손가락 두 개를 못 쓰는 상황(장갑, 한 손)이 러닝 중에는 흔하다.
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MapButton(Icons.Filled.Add) {
                    if (zoomDelta < MAX_USER_ZOOM_IN) {
                        zoomDelta++
                        panX *= 2
                        panY *= 2
                    }
                }
                MapButton(Icons.Filled.Remove) {
                    if (zoomDelta > MAX_USER_ZOOM_OUT) {
                        zoomDelta--
                        panX /= 2
                        panY /= 2
                    }
                }
            }

            // 지도를 옮겨 놓고 나면 경로를 다시 찾기 어렵다. 되돌아갈 자리를 준다.
            if (moved) {
                MapButton(
                    icon = Icons.Filled.MyLocation,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                ) {
                    zoomDelta = 0
                    panX = 0.0
                    panY = 0.0
                    pinch = 1f
                }
            }
        }
    }
}

/** 사용자가 기본 줌에서 더 당길 수 있는 단계 */
private const val MAX_USER_ZOOM_IN = 4

/** 더 밀어낼 수 있는 단계 (음수) */
private const val MAX_USER_ZOOM_OUT = -3

/** 지도 위 작은 원형 버튼 */
@Composable
private fun MapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xCC0B0D0F))
            .border(1.dp, Volt.copy(alpha = 0.4f), CircleShape)
            .quietClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Volt,
            modifier = Modifier.size(17.dp),
        )
    }
}

/**
 * 타일 배치 계획.
 *
 * 좌표 계산은 전부 **타일 픽셀 공간**(타일 한 장 = 256)에서 하고, 화면에 올릴 때만
 * [scale]을 곱한다. 이렇게 해야 3배 밀도 화면에서 타일이 실제 크기의 1/3로
 * 쪼그라들어 글씨를 못 읽는 일이 없다.
 */
internal data class TilePlan(
    val zoom: Int,
    val originX: Double,
    val originY: Double,
    val minTileX: Int,
    val maxTileX: Int,
    val minTileY: Int,
    val maxTileY: Int,
    val scale: Float,
) {
    /** 이 계획이 필요로 하는 타일 집합의 식별자 — 효과를 여기에 묶는다 */
    val rangeKey: String get() = "$zoom/$minTileX-$maxTileX/$minTileY-$maxTileY"

    /** 날짜변경선을 넘어간 타일 인덱스를 세계 범위 안으로 되돌린다 */
    fun wrapX(x: Int): Int {
        val n = 1 shl zoom
        return ((x % n) + n) % n
    }

    fun toScreen(point: GeoPoint): Offset {
        val x = (MapTiles.worldX(point.lng, zoom) - originX) * scale
        val y = (MapTiles.worldY(point.lat, zoom) - originY) * scale
        return Offset(x.toFloat(), y.toFloat())
    }

    companion object {
        /**
         * 타일을 화면에 몇 배로 늘려 그릴지.
         *
         * 예전에는 화면 밀도를 그대로 썼다. 3배 밀도 폰에서는 256px 타일을
         * 768px로 늘려 그리는 셈이라, 글자와 도로가 뭉개져 보였다 — "지도가
         * 흐리다"의 정체가 이것이다.
         *
         * 밀도의 절반만 쓴다. 같은 화면에 더 높은 줌의 타일이 들어오므로
         * 글자와 도로가 또렷해지고, 받는 타일 수는 예산([MapTiles.MAX_TILES])
         * 안에 머문다. 1.0 아래로는 내리지 않는다 — 그러면 지도가 실제보다
         * 작게 그려져 읽기 어려워진다.
         */
        private fun tileScale(density: Float): Float = (density / 2f).coerceIn(1f, 2f)

        /**
         * @param zoomDelta 사용자가 손가락으로 더하거나 뺀 줌 단계
         * @param panX 사용자가 끌어 옮긴 거리(타일 픽셀). 화면 픽셀이 아니다.
         */
        fun of(
            points: List<GeoPoint>,
            widthPx: Int,
            heightPx: Int,
            density: Float,
            zoomDelta: Int = 0,
            panX: Double = 0.0,
            panY: Double = 0.0,
        ): TilePlan {
            val scale = tileScale(density)
            // 뷰포트를 타일 픽셀 단위로 환산해서 줌과 원점을 잡는다
            val viewW = (widthPx / scale).toDouble()
            val viewH = (heightPx / scale).toDouble()

            val zoom = (MapTiles.fitZoom(points, viewW.toInt(), viewH.toInt()) + zoomDelta)
                .coerceIn(MapTiles.MIN_ZOOM, MapTiles.MAX_ZOOM)
            val xs = points.map { MapTiles.worldX(it.lng, zoom) }
            val ys = points.map { MapTiles.worldY(it.lat, zoom) }
            // 경로의 한가운데가 화면 한가운데 오도록 원점을 잡고, 사용자가
            // 끌어 옮긴 만큼 비킨다.
            val originX = (xs.min() + xs.max()) / 2 - viewW / 2 - panX
            val originY = (ys.min() + ys.max()) / 2 - viewH / 2 - panY

            val maxTileIndex = (1 shl zoom) - 1
            val minTileX = floor(originX / MapTiles.TILE_SIZE).toInt()
            val maxTileX = floor((originX + viewW) / MapTiles.TILE_SIZE).toInt()
            val minTileY = floor(originY / MapTiles.TILE_SIZE).toInt().coerceIn(0, maxTileIndex)
            val maxTileY = floor((originY + viewH) / MapTiles.TILE_SIZE).toInt()
                .coerceIn(0, maxTileIndex)

            return TilePlan(zoom, originX, originY, minTileX, maxTileX, minTileY, maxTileY, scale)
        }
    }
}

/**
 * 밝은 OSM 타일을 어두운 지도로 바꾸는 색 행렬.
 *
 * 반전으로 흰 배경을 검게 만들고 채도를 낮춰 도로가 회색 계열로만 남게 한다.
 * 그래야 그 위의 볼트 네온이 화면에서 유일한 색이 된다.
 */
private val darkMapFilter: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -0.62f, -0.20f, -0.10f, 0f, 232f,
            -0.20f, -0.62f, -0.10f, 0f, 232f,
            -0.16f, -0.20f, -0.56f, 0f, 236f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)

/**
 * 캐시에 있는 타일을 화면에 깐다. 그린 타일 수를 돌려준다.
 *
 * 비트맵을 컴포지션 상태로 따로 들지 않고 [MapTiles] 캐시에서 바로 읽는다.
 * 상태에 복사해 두면 LruCache가 비워도 컴포지션이 계속 붙잡고 있어, 크기 제한이
 * 무력해지고 줌을 오갈수록 메모리가 샌다.
 */
private fun DrawScope.drawTiles(plan: TilePlan): Int {
    var drawn = 0
    val side = (MapTiles.TILE_SIZE * plan.scale).toInt()
    for (ty in plan.minTileY..plan.maxTileY) {
        for (tx in plan.minTileX..plan.maxTileX) {
            val image = MapTiles.cached(plan.zoom, plan.wrapX(tx), ty) ?: continue
            // floor로 내림해야 음수 구간에서 타일 사이가 1px 벌어지지 않는다
            val left = floor((tx * MapTiles.TILE_SIZE - plan.originX) * plan.scale).toInt()
            val top = floor((ty * MapTiles.TILE_SIZE - plan.originY) * plan.scale).toInt()
            drawImage(
                image = image,
                dstOffset = IntOffset(left, top),
                dstSize = IntSize(side, side),
                colorFilter = darkMapFilter,
            )
            drawn++
        }
    }
    return drawn
}

/** 폴리라인 전체 길이 기준 f(0..1) 지점의 좌표 */
private fun pointAlongRoute(pts: List<Offset>, f: Float): Offset {
    if (pts.size < 2) return pts.firstOrNull() ?: Offset.Zero
    val segs = FloatArray(pts.size - 1)
    var total = 0f
    for (i in 0 until pts.size - 1) {
        val d = (pts[i + 1] - pts[i]).getDistance()
        segs[i] = d
        total += d
    }
    if (total <= 0f) return pts.first()
    var remain = total * f
    for (i in segs.indices) {
        if (remain <= segs[i]) {
            val t = if (segs[i] > 0f) remain / segs[i] else 0f
            return pts[i] + (pts[i + 1] - pts[i]) * t
        }
        remain -= segs[i]
    }
    return pts.last()
}

/** 도착 깃발 — 막대 + 삼각 깃발 */
private fun DrawScope.drawRouteFlag(at: Offset) {
    val h = 13.dp.toPx()
    drawCircle(Volt.copy(alpha = 0.30f), radius = 8.dp.toPx(), center = at)
    drawLine(
        color = Color.White,
        start = at,
        end = Offset(at.x, at.y - h),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    val flag = Path().apply {
        moveTo(at.x, at.y - h)
        lineTo(at.x + h * 0.62f, at.y - h * 0.78f)
        lineTo(at.x, at.y - h * 0.56f)
        close()
    }
    drawPath(flag, Volt)
}
