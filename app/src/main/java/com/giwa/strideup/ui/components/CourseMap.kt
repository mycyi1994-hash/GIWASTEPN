package com.giwa.strideup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.giwa.strideup.ui.theme.Volt
import kotlin.math.abs

/**
 * 네온 코스맵 — 실제 GPS 좌표(0..1로 정규화된)를 어두운 지도 스타일 위에 그린다.
 *
 * 지도 SDK 없이 목업의 느낌을 낸다: 코스마다 고정된 의사 도로망(시드 기반)을
 * 희미하게 깔고, 그 위에 볼트 네온 경로 + 시작점 + 도착 깃발을 얹는다.
 * 같은 코스는 언제나 같은 도로망이 나오므로 지도처럼 "그 장소"로 인식된다.
 */
@Composable
fun CourseTrackMap(
    points: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    seed: Int = 0,
    /** 0..1 — 경로 위 진행 지점에 러너 점을 찍는다. null이면 표시하지 않음 */
    progress: Float? = null,
) {
    Canvas(modifier) {
        drawStreets(seed)
        if (points.size < 2) return@Canvas

        // 경로를 8% 안쪽 여백에 펼친다
        val inset = 0.08f
        fun px(p: Pair<Float, Float>): Offset = Offset(
            (inset + p.first * (1f - inset * 2)) * size.width,
            (inset + p.second * (1f - inset * 2)) * size.height,
        )

        val path = Path()
        val first = px(points.first())
        path.moveTo(first.x, first.y)
        for (i in 1 until points.size) {
            val p = px(points[i])
            path.lineTo(p.x, p.y)
        }

        // 글로우(넓고 옅게) → 본선(가늘고 진하게)
        drawPath(
            path,
            color = Volt.copy(alpha = 0.18f),
            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path,
            brush = Brush.linearGradient(listOf(Volt.copy(alpha = 0.85f), Volt)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // 시작점 — 흰 심의 볼트 링
        drawCircle(Volt.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = first)
        drawCircle(Color.White, radius = 3.dp.toPx(), center = first)

        // 도착 깃발
        val last = px(points.last())
        drawFlag(last)

        // 진행 지점 러너 점
        progress?.let { f ->
            val at = pointAlong(points.map(::px), f.coerceIn(0f, 1f))
            drawCircle(Volt.copy(alpha = 0.30f), radius = 9.dp.toPx(), center = at)
            drawCircle(Volt, radius = 4.5f.dp.toPx(), center = at)
            drawCircle(Color(0xFF060708), radius = 2.dp.toPx(), center = at)
        }
    }
}

/** 시드 고정 의사 도로망 — 지도 SDK 없이 "지도 위" 느낌을 만든다 */
private fun DrawScope.drawStreets(seed: Int) {
    var s = seed * 92821 + 137
    fun rand(): Float {
        s = s * 1_103_515_245 + 12_345
        return abs(s % 1000) / 1000f
    }
    val faint = Color.White.copy(alpha = 0.045f)
    val fainter = Color.White.copy(alpha = 0.025f)
    // 큰 도로 — 화면을 가로지르는 꺾인 선 몇 개
    repeat(4) {
        val y = rand() * size.height
        val bend = (rand() - 0.5f) * size.height * 0.3f
        val path = Path().apply {
            moveTo(0f, y)
            lineTo(size.width * (0.3f + rand() * 0.2f), y + bend)
            lineTo(size.width, y + bend * 0.4f)
        }
        drawPath(path, faint, style = Stroke(width = 2.dp.toPx()))
    }
    repeat(4) {
        val x = rand() * size.width
        val bend = (rand() - 0.5f) * size.width * 0.3f
        val path = Path().apply {
            moveTo(x, 0f)
            lineTo(x + bend, size.height * (0.4f + rand() * 0.2f))
            lineTo(x + bend * 0.5f, size.height)
        }
        drawPath(path, faint, style = Stroke(width = 2.dp.toPx()))
    }
    // 골목 — 짧은 점선
    repeat(10) {
        val x0 = rand() * size.width
        val y0 = rand() * size.height
        val horizontal = rand() > 0.5f
        val len = (0.1f + rand() * 0.2f)
        drawLine(
            color = fainter,
            start = Offset(x0, y0),
            end = if (horizontal) Offset(x0 + size.width * len, y0) else Offset(x0, y0 + size.height * len),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
        )
    }
    // 강 — 넓고 아주 어두운 띠 하나
    val riverY = size.height * (0.55f + rand() * 0.3f)
    drawLine(
        color = Color(0xFF0D1A20).copy(alpha = 0.8f),
        start = Offset(0f, riverY),
        end = Offset(size.width, riverY - size.height * 0.12f),
        strokeWidth = 14.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

/** 폴리라인 전체 길이 기준 f(0..1) 지점의 좌표 */
private fun pointAlong(pts: List<Offset>, f: Float): Offset {
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
private fun DrawScope.drawFlag(at: Offset) {
    val h = 13.dp.toPx()
    val pole = 2.dp.toPx()
    drawCircle(Volt.copy(alpha = 0.30f), radius = 8.dp.toPx(), center = at)
    drawLine(
        color = Color.White,
        start = at,
        end = Offset(at.x, at.y - h),
        strokeWidth = pole,
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
