package com.giwa.strideup.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.giwa.strideup.domain.Rarity
import com.giwa.strideup.domain.Sneaker
import com.giwa.strideup.domain.SneakerModel
import com.giwa.strideup.domain.StripeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 스니커즈 측면 실루엣.
 *
 * 디자인 박스는 1.0 × 0.62 (가로:세로)로 고정하고 캔버스에 맞춰 균등 스케일한다.
 * 모델 파라미터(밑창 두께 · 토 들림 · 칼라 높이 · 스트라이프)가 실루엣을 바꾸고,
 * 컬러웨이가 색을, 희귀도가 발광 강도를 정한다.
 */
private const val BOX_W = 1.0f
private const val BOX_H = 0.62f

/** 캔버스 좌표 변환기 */
private class Frame(scope: DrawScope) {
    val scale = min(scope.size.width / BOX_W, scope.size.height / BOX_H)
    val ox = (scope.size.width - BOX_W * scale) / 2f
    val oy = (scope.size.height - BOX_H * scale) / 2f
    fun x(v: Float) = ox + v * scale
    fun y(v: Float) = oy + v * scale
    fun p(px: Float, py: Float) = Offset(x(px), y(py))
    fun u(v: Float) = v * scale
}

private fun Path.moveTo(f: Frame, x: Float, y: Float) = moveTo(f.x(x), f.y(y))
private fun Path.lineTo(f: Frame, x: Float, y: Float) = lineTo(f.x(x), f.y(y))
private fun Path.cubicTo(
    f: Frame,
    x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float,
) = cubicTo(f.x(x1), f.y(y1), f.x(x2), f.y(y2), f.x(x3), f.y(y3))

/** 희귀도별 외곽 아우라 강도 */
private fun Rarity.auraAlpha(): Float = when (this) {
    Rarity.COMMON -> 0.16f
    Rarity.UNCOMMON -> 0.22f
    Rarity.RARE -> 0.30f
    Rarity.EPIC -> 0.40f
    Rarity.LEGENDARY -> 0.52f
}

/**
 * 스니커즈를 그린다.
 *
 * @param shimmer 0f~1f, 표면을 훑는 광택 위치. 0이면 광택 없음.
 */
@Composable
fun SneakerArt(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    showGlow: Boolean = true,
    shimmer: Float = 0f,
) {
    val cw = sneaker.colorway
    val model = sneaker.model
    val upper = Color(cw.upper)
    val upperShade = Color(cw.upperShade)
    val accent = Color(cw.accent)
    val accentSoft = Color(cw.accentSoft)
    val sole = Color(cw.sole)

    Canvas(modifier) {
        val f = Frame(this)
        val mt = 0.545f - model.soleThickness      // 미드솔 상단 = 어퍼 하단
        val ct = model.collarTop                    // 발목 칼라 상단
        val rise = model.toeRise

        // ── 0. 바닥 발광 ─────────────────────────────────────
        if (showGlow) {
            val glowY = f.y(0.605f)
            val glowW = f.u(0.98f)
            val glowH = f.u(0.10f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = sneaker.rarity.auraAlpha()), Color.Transparent),
                    center = Offset(f.x(0.5f), glowY),
                    radius = glowW / 2f,
                ),
                topLeft = Offset(f.x(0.5f) - glowW / 2f, glowY - glowH / 2f),
                size = Size(glowW, glowH),
            )
            // 상위 등급은 신발 뒤로 아우라가 한 겹 더 퍼진다
            if (sneaker.rarity.ordinal >= Rarity.EPIC.ordinal) {
                val r = f.u(0.52f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(f.x(0.5f), f.y(0.36f)),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(f.x(0.5f), f.y(0.36f)),
                )
            }
        }

        // ── 1. 아웃솔 (바닥 트레드) ──────────────────────────
        val outsole = Path().apply {
            moveTo(f, 0.100f, 0.545f)
            lineTo(f, 0.950f, 0.535f - rise)
            cubicTo(f, 0.988f, 0.548f - rise, 0.980f, 0.582f - rise, 0.930f, 0.588f - rise)
            lineTo(f, 0.150f, 0.598f)
            cubicTo(f, 0.080f, 0.598f, 0.060f, 0.565f, 0.100f, 0.545f)
            close()
        }
        drawPath(outsole, color = Color(0xFF0A0C0D))

        // 트레드 홈
        for (i in 0 until 9) {
            val t = i / 8f
            val gx = 0.16f + t * 0.75f
            val gy = 0.560f - rise * t
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = f.p(gx, gy),
                end = f.p(gx - 0.012f, gy + 0.032f),
                strokeWidth = f.u(0.010f),
                cap = StrokeCap.Round,
            )
        }

        // ── 2. 미드솔 ────────────────────────────────────────
        val midsole = Path().apply {
            moveTo(f, 0.048f, mt)
            lineTo(f, 0.950f, mt)
            cubicTo(f, 0.982f, mt + 0.015f, 0.990f, 0.520f - rise, 0.948f, 0.548f - rise)
            lineTo(f, 0.105f, 0.556f)
            cubicTo(f, 0.040f, 0.556f, 0.024f, mt + 0.040f, 0.048f, mt)
            close()
        }
        drawPath(
            midsole,
            brush = Brush.verticalGradient(
                colors = listOf(sole, Color(0xFF0E1011)),
                startY = f.y(mt),
                endY = f.y(0.556f),
            ),
        )

        // ── 3. 미드솔 발광 윈도우 (목업의 LED) ────────────────
        val winTop = mt + 0.022f
        val winBottom = 0.520f
        listOf(0.150f to 0.262f, 0.300f to 0.392f).forEach { (x0, x1) ->
            val w = f.u(x1 - x0)
            val h = f.u(winBottom - winTop)
            val tl = Offset(f.x(x0), f.y(winTop))
            if (showGlow) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(tl.x + w / 2f, tl.y + h / 2f),
                        radius = w * 1.1f,
                    ),
                    topLeft = Offset(tl.x - w * 0.55f, tl.y - h * 1.1f),
                    size = Size(w * 2.1f, h * 3.2f),
                )
            }
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(accentSoft, accent)),
                topLeft = tl,
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(f.u(0.020f)),
            )
        }

        // ── 4. 어퍼 (본체) ───────────────────────────────────
        val upperPath = Path().apply {
            moveTo(f, 0.055f, mt)
            cubicTo(f, 0.030f, mt - 0.12f, 0.045f, ct + 0.055f, 0.100f, ct + 0.012f)
            cubicTo(f, 0.150f, ct - 0.020f, 0.220f, ct - 0.018f, 0.262f, ct + 0.038f)
            cubicTo(f, 0.305f, ct + 0.092f, 0.350f, ct + 0.128f, 0.420f, ct + 0.148f)
            lineTo(f, 0.530f, ct + 0.160f)
            cubicTo(f, 0.680f, ct + 0.185f, 0.800f, mt - 0.070f, 0.890f, mt - 0.030f)
            cubicTo(f, 0.940f, mt - 0.014f, 0.964f, mt - 0.002f, 0.960f, mt)
            lineTo(f, 0.055f, mt)
            close()
        }
        drawPath(
            upperPath,
            brush = Brush.linearGradient(
                colors = listOf(upper, upperShade),
                start = f.p(0.2f, ct),
                end = f.p(0.9f, mt),
            ),
        )

        // 어퍼 상단 하이라이트 (빛 받는 면)
        drawPath(
            upperPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                startY = f.y(ct),
                endY = f.y(mt - 0.05f),
            ),
        )

        // ── 5. 토캡 ──────────────────────────────────────────
        val toeCap = Path().apply {
            moveTo(f, 0.782f, mt)
            cubicTo(f, 0.792f, mt - 0.058f, 0.845f, mt - 0.044f, 0.890f, mt - 0.030f)
            cubicTo(f, 0.940f, mt - 0.014f, 0.964f, mt - 0.002f, 0.960f, mt)
            close()
        }
        drawPath(toeCap, color = Color.White.copy(alpha = 0.06f))

        // ── 6. 힐 카운터 (뒤꿈치 보강재) ──────────────────────
        val heel = Path().apply {
            moveTo(f, 0.052f, mt)
            cubicTo(f, 0.030f, mt - 0.11f, 0.046f, ct + 0.055f, 0.100f, ct + 0.014f)
            lineTo(f, 0.158f, ct + 0.050f)
            cubicTo(f, 0.112f, ct + 0.090f, 0.100f, mt - 0.060f, 0.112f, mt)
            close()
        }
        drawPath(heel, color = accent.copy(alpha = 0.20f))
        drawPath(heel, color = accent.copy(alpha = 0.55f), style = Stroke(width = f.u(0.006f)))

        // ── 7. 발목 칼라 개구부 ──────────────────────────────
        rotate(degrees = -16f, pivot = f.p(0.300f, ct + 0.078f)) {
            drawOval(
                color = Color(0xFF07090A),
                topLeft = Offset(f.x(0.300f) - f.u(0.108f), f.y(ct + 0.078f) - f.u(0.046f)),
                size = Size(f.u(0.216f), f.u(0.092f)),
            )
            drawOval(
                color = accent.copy(alpha = 0.30f),
                topLeft = Offset(f.x(0.300f) - f.u(0.108f), f.y(ct + 0.078f) - f.u(0.046f)),
                size = Size(f.u(0.216f), f.u(0.092f)),
                style = Stroke(width = f.u(0.007f)),
            )
        }

        // ── 8. 레이스 (끈) ───────────────────────────────────
        for (i in 0 until 4) {
            val t = i / 3f
            val lx = 0.452f + t * 0.176f
            val ly = ct + 0.150f + t * 0.030f
            drawLine(
                color = accentSoft.copy(alpha = 0.85f),
                start = f.p(lx - 0.024f, ly),
                end = f.p(lx + 0.026f, ly + 0.052f),
                strokeWidth = f.u(0.013f),
                cap = StrokeCap.Round,
            )
        }
        // 레이스 패널 라인
        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = f.p(0.430f, ct + 0.160f),
            end = f.p(0.690f, ct + 0.212f),
            strokeWidth = f.u(0.008f),
            cap = StrokeCap.Round,
        )

        // ── 9. 사이드 스트라이프 (모델별) ─────────────────────
        drawStripe(f, model, accent, accentSoft, mt, ct)

        // ── 10. 헥사곤 로고 배지 ─────────────────────────────
        val badgeC = f.p(0.430f, (ct + 0.160f + mt) / 2f + 0.030f)
        val badgeR = f.u(0.052f)
        val hex = Path().apply {
            for (i in 0 until 6) {
                val a = (-90f + i * 60f) * (PI / 180.0)
                val hx = badgeC.x + badgeR * cos(a).toFloat()
                val hy = badgeC.y + badgeR * sin(a).toFloat()
                if (i == 0) moveTo(hx, hy) else lineTo(hx, hy)
            }
            close()
        }
        drawPath(hex, color = accent.copy(alpha = 0.16f))
        drawPath(hex, color = accent, style = Stroke(width = f.u(0.009f)))
        drawCircle(accent, radius = badgeR * 0.26f, center = badgeC)

        // ── 11. 어퍼 외곽선 ──────────────────────────────────
        drawPath(upperPath, color = Color.Black.copy(alpha = 0.45f), style = Stroke(width = f.u(0.007f)))

        // ── 12. 광택 스윕 ────────────────────────────────────
        if (shimmer > 0f) {
            val band = f.u(0.30f)
            val cx = f.x(-0.2f) + (f.x(1.2f) - f.x(-0.2f)) * shimmer
            drawPath(
                upperPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                    start = Offset(cx - band, 0f),
                    end = Offset(cx + band, size.height),
                ),
            )
        }
    }
}

/** 모델별 사이드 스트라이프 */
private fun DrawScope.drawStripe(
    f: Frame,
    model: SneakerModel,
    accent: Color,
    accentSoft: Color,
    mt: Float,
    ct: Float,
) {
    val mid = (ct + 0.170f + mt) / 2f
    when (model.stripe) {
        StripeStyle.SWOOSH -> {
            val p = Path().apply {
                moveTo(f, 0.170f, mt - 0.020f)
                cubicTo(f, 0.330f, mid + 0.055f, 0.520f, mid - 0.010f, 0.760f, mid - 0.070f)
                lineTo(f, 0.775f, mid - 0.028f)
                cubicTo(f, 0.530f, mid + 0.036f, 0.340f, mid + 0.100f, 0.185f, mt + 0.008f)
                close()
            }
            drawPath(p, brush = Brush.horizontalGradient(listOf(accent, accentSoft)))
        }
        StripeStyle.BLADE -> {
            val p = Path().apply {
                moveTo(f, 0.250f, mt - 0.012f)
                lineTo(f, 0.640f, mid - 0.060f)
                lineTo(f, 0.660f, mid - 0.006f)
                lineTo(f, 0.268f, mt + 0.038f)
                close()
            }
            drawPath(p, brush = Brush.horizontalGradient(listOf(accentSoft, accent)))
        }
        StripeStyle.CHEVRON -> {
            for (i in 0 until 3) {
                val sx = 0.270f + i * 0.150f
                val p = Path().apply {
                    moveTo(f, sx, mt - 0.006f)
                    lineTo(f, sx + 0.085f, mid - 0.030f)
                    lineTo(f, sx + 0.108f, mid + 0.006f)
                    lineTo(f, sx + 0.024f, mt + 0.030f)
                    close()
                }
                drawPath(p, color = if (i == 1) accentSoft else accent)
            }
        }
        StripeStyle.DUAL -> {
            listOf(0f, 0.052f).forEach { dy ->
                drawLine(
                    color = if (dy == 0f) accent else accentSoft,
                    start = f.p(0.215f, mt - 0.010f + dy),
                    end = f.p(0.740f, mid - 0.055f + dy),
                    strokeWidth = f.u(0.019f),
                    cap = StrokeCap.Round,
                )
            }
        }
        StripeStyle.WAVE -> {
            val p = Path()
            val steps = 26
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val wx = 0.180f + t * 0.590f
                val wy = mt - 0.012f - t * 0.070f + sin(t * PI * 2.0).toFloat() * 0.030f
                if (i == 0) p.moveTo(f, wx, wy) else p.lineTo(f, wx, wy)
            }
            drawPath(
                p,
                brush = Brush.horizontalGradient(listOf(accent, accentSoft)),
                style = Stroke(width = f.u(0.022f), cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * 히어로 연출 — 공중에 뜬 스니커즈 + 궤도 링 + 흐르는 광택.
 * 스플래시와 상세 화면에서 쓴다.
 */
@Composable
fun SneakerHero(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    orbit: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "sneakerHero")
    val shimmer by transition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, 900, LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "heroShimmer",
    )
    val float by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroFloat",
    )
    val accent = Color(sneaker.colorway.accent)

    Box(modifier, contentAlignment = Alignment.Center) {
        if (orbit) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.60f
                val rx = size.width * 0.46f
                val ry = size.height * 0.17f
                // 궤도 링 두 겹
                listOf(1.0f to 0.55f, 0.78f to 0.30f).forEach { (s, a) ->
                    drawOval(
                        color = accent.copy(alpha = a),
                        topLeft = Offset(cx - rx * s, cy - ry * s),
                        size = Size(rx * 2 * s, ry * 2 * s),
                        style = Stroke(width = size.minDimension * 0.008f),
                    )
                }
            }
        }
        SneakerArt(
            sneaker = sneaker,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = float * 7f },
            showGlow = true,
            shimmer = shimmer.coerceIn(0f, 1f),
        )
    }
}
