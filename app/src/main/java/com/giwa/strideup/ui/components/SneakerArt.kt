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
import androidx.compose.ui.geometry.CornerRadius
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
import com.giwa.strideup.domain.Faction
import com.giwa.strideup.domain.Rarity
import com.giwa.strideup.domain.Silhouette
import com.giwa.strideup.domain.Sneaker
import com.giwa.strideup.domain.StripeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 스니커즈 측면 실루엣.
 *
 * 디자인 박스 1.0 × 0.62를 캔버스에 균등 스케일한다.
 * 속성이 색과 배경 이펙트를, 등급·변형이 실루엣과 오너먼트를 정한다.
 */
private const val BOX_W = 1.0f
private const val BOX_H = 0.62f

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

private fun Rarity.auraAlpha(): Float = when (this) {
    Rarity.COMMON -> 0.18f
    Rarity.RARE -> 0.28f
    Rarity.EPIC -> 0.40f
    Rarity.LEGENDARY -> 0.55f
}

@Composable
fun SneakerArt(
    sneaker: Sneaker,
    modifier: Modifier = Modifier,
    showGlow: Boolean = true,
    shimmer: Float = 0f,
    /** 0f~1f 반복 위상 — 속성 이펙트 애니메이션용 */
    phase: Float = 0f,
) {
    val fa = sneaker.faction
    val accent = Color(fa.accent)
    val accentSoft = Color(fa.accentSoft)
    val accentDeep = Color(fa.accentDeep)
    // 상위 등급의 물 속성은 어퍼가 밝은 실버로 바뀐다 (목업 반영)
    val lighten = sneaker.faction == Faction.WATER && sneaker.rarity.ordinal >= Rarity.EPIC.ordinal
    val upper = if (lighten) Color(0xFFDDE5EC) else Color(fa.upper)
    val upperShade = if (lighten) Color(0xFF9AA8B4) else Color(fa.upperShade)
    val sole = Color(fa.sole)
    val sil = sneaker.silhouette

    Canvas(modifier) {
        val f = Frame(this)
        val mt = 0.545f - sil.soleThickness
        val ct = sil.collarTop
        val rise = sil.toeRise

        // ── 배경: 속성 이펙트 ───────────────────────────────
        if (showGlow) {
            drawFactionEffect(f, fa, sneaker.rarity, accent, accentSoft, phase)
        }

        // ── 전설 오너먼트 (신발 뒤) ─────────────────────────
        if (sneaker.rarity == Rarity.LEGENDARY) {
            if (sneaker.variant == 0) {
                drawLegendaryWings(f, accent, accentSoft, phase)
            } else {
                drawLegendaryCreature(f, fa, accent, accentSoft, phase)
            }
        }

        // ── 바닥 발광 ──────────────────────────────────────
        if (showGlow) {
            val glowY = f.y(0.605f)
            val glowW = f.u(1.00f)
            val glowH = f.u(0.11f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = sneaker.rarity.auraAlpha()), Color.Transparent),
                    center = Offset(f.x(0.5f), glowY),
                    radius = glowW / 2f,
                ),
                topLeft = Offset(f.x(0.5f) - glowW / 2f, glowY - glowH / 2f),
                size = Size(glowW, glowH),
            )
            // 궤도 링 — 목업의 바닥 원형 광선
            drawOval(
                color = accent.copy(alpha = 0.45f),
                topLeft = Offset(f.x(0.5f) - f.u(0.50f), f.y(0.585f)),
                size = Size(f.u(1.00f), f.u(0.075f)),
                style = Stroke(width = f.u(0.007f)),
            )
        }

        // ── 아웃솔 ─────────────────────────────────────────
        val outsole = Path().apply {
            moveTo(f, 0.100f, 0.545f)
            lineTo(f, 0.950f, 0.535f - rise)
            cubicTo(f, 0.988f, 0.548f - rise, 0.980f, 0.582f - rise, 0.930f, 0.588f - rise)
            lineTo(f, 0.150f, 0.598f)
            cubicTo(f, 0.080f, 0.598f, 0.060f, 0.565f, 0.100f, 0.545f)
            close()
        }
        drawPath(outsole, color = Color(0xFF07080A))
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

        // ── 미드솔 ─────────────────────────────────────────
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
                colors = listOf(sole, Color(0xFF0B0D0F)),
                startY = f.y(mt),
                endY = f.y(0.556f),
            ),
        )

        // ── 발광 포드 (등급이 높을수록 많다) ─────────────────
        val winTop = mt + 0.020f
        val winBottom = 0.521f
        val podStart = 0.135f
        val podEnd = 0.62f
        val podW = (podEnd - podStart) / sil.pods * 0.68f
        for (i in 0 until sil.pods) {
            val x0 = podStart + (podEnd - podStart) * i / sil.pods
            val w = f.u(podW)
            val h = f.u(winBottom - winTop)
            val tl = Offset(f.x(x0), f.y(winTop))
            if (showGlow) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.62f), Color.Transparent),
                        center = Offset(tl.x + w / 2f, tl.y + h / 2f),
                        radius = w * 1.25f,
                    ),
                    topLeft = Offset(tl.x - w * 0.62f, tl.y - h * 1.2f),
                    size = Size(w * 2.24f, h * 3.4f),
                )
            }
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(accentSoft, accent)),
                topLeft = tl,
                size = Size(w, h),
                cornerRadius = CornerRadius(f.u(0.018f)),
            )
        }

        // ── 어퍼 ───────────────────────────────────────────
        val collarLift = if (sil.highTop) 0.055f else 0f
        val upperPath = Path().apply {
            moveTo(f, 0.055f, mt)
            cubicTo(f, 0.030f, mt - 0.12f, 0.045f, ct + 0.055f - collarLift, 0.100f, ct + 0.012f - collarLift)
            cubicTo(f, 0.150f, ct - 0.020f - collarLift, 0.220f, ct - 0.018f - collarLift, 0.262f, ct + 0.038f - collarLift)
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
        drawPath(
            upperPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                startY = f.y(ct),
                endY = f.y(mt - 0.05f),
            ),
        )

        // 상위 등급 어퍼 텍스처 — 육각 메쉬
        if (sneaker.rarity.ordinal >= Rarity.RARE.ordinal) {
            for (row in 0 until 3) {
                for (col in 0 until 7) {
                    val hx = 0.30f + col * 0.075f + (row % 2) * 0.037f
                    val hy = ct + 0.185f + row * 0.048f
                    if (hy > mt - 0.02f) continue
                    drawHex(f, hx, hy, 0.020f, accent.copy(alpha = 0.16f), 0.0035f)
                }
            }
        }

        // ── 토캡 ───────────────────────────────────────────
        val toeCap = Path().apply {
            moveTo(f, 0.782f, mt)
            cubicTo(f, 0.792f, mt - 0.058f, 0.845f, mt - 0.044f, 0.890f, mt - 0.030f)
            cubicTo(f, 0.940f, mt - 0.014f, 0.964f, mt - 0.002f, 0.960f, mt)
            close()
        }
        drawPath(toeCap, color = Color.White.copy(alpha = 0.06f))

        // ── 힐 카운터 ──────────────────────────────────────
        val heel = Path().apply {
            moveTo(f, 0.052f, mt)
            cubicTo(f, 0.030f, mt - 0.11f, 0.046f, ct + 0.055f - collarLift, 0.100f, ct + 0.014f - collarLift)
            lineTo(f, 0.158f, ct + 0.050f - collarLift)
            cubicTo(f, 0.112f, ct + 0.090f, 0.100f, mt - 0.060f, 0.112f, mt)
            close()
        }
        drawPath(heel, color = accent.copy(alpha = 0.22f))
        drawPath(heel, color = accent.copy(alpha = 0.60f), style = Stroke(width = f.u(0.006f)))

        // ── 칼라 개구부 ────────────────────────────────────
        rotate(degrees = -16f, pivot = f.p(0.300f, ct + 0.078f - collarLift)) {
            drawOval(
                color = Color(0xFF06070A),
                topLeft = Offset(
                    f.x(0.300f) - f.u(0.108f),
                    f.y(ct + 0.078f - collarLift) - f.u(0.046f),
                ),
                size = Size(f.u(0.216f), f.u(0.092f)),
            )
            drawOval(
                color = accent.copy(alpha = 0.35f),
                topLeft = Offset(
                    f.x(0.300f) - f.u(0.108f),
                    f.y(ct + 0.078f - collarLift) - f.u(0.046f),
                ),
                size = Size(f.u(0.216f), f.u(0.092f)),
                style = Stroke(width = f.u(0.007f)),
            )
        }

        // ── 레이스 ─────────────────────────────────────────
        for (i in 0 until 4) {
            val t = i / 3f
            val lx = 0.452f + t * 0.176f
            val ly = ct + 0.150f + t * 0.030f
            drawLine(
                color = accentSoft.copy(alpha = 0.9f),
                start = f.p(lx - 0.024f, ly),
                end = f.p(lx + 0.026f, ly + 0.052f),
                strokeWidth = f.u(0.013f),
                cap = StrokeCap.Round,
            )
        }

        // ── 사이드 스트라이프 ───────────────────────────────
        drawStripe(f, sil, accent, accentSoft, accentDeep, mt, ct)

        // ── 헥사곤 로고 배지 ────────────────────────────────
        val badgeC = f.p(0.430f, (ct + 0.160f + mt) / 2f + 0.030f)
        val badgeR = f.u(0.054f)
        val hex = Path().apply {
            for (i in 0 until 6) {
                val a = (-90f + i * 60f) * (PI / 180.0)
                val hx = badgeC.x + badgeR * cos(a).toFloat()
                val hy = badgeC.y + badgeR * sin(a).toFloat()
                if (i == 0) moveTo(hx, hy) else lineTo(hx, hy)
            }
            close()
        }
        drawPath(hex, color = accent.copy(alpha = 0.20f))
        drawPath(hex, color = accent, style = Stroke(width = f.u(0.009f)))
        drawFactionGlyph(f, fa, badgeC, badgeR * 0.55f, accentSoft)

        // ── 외곽선 ─────────────────────────────────────────
        drawPath(upperPath, color = Color.Black.copy(alpha = 0.45f), style = Stroke(width = f.u(0.007f)))

        // ── 광택 스윕 ──────────────────────────────────────
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

/** 작은 육각형 스트로크 */
private fun DrawScope.drawHex(f: Frame, cx: Float, cy: Float, r: Float, color: Color, width: Float) {
    val p = Path()
    for (i in 0 until 6) {
        val a = (-90f + i * 60f) * (PI / 180.0)
        val x = f.x(cx + r * cos(a).toFloat())
        val y = f.y(cy + r * sin(a).toFloat())
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color = color, style = Stroke(width = f.u(width)))
}

/** 배지 안 속성 문양 */
private fun DrawScope.drawFactionGlyph(f: Frame, fa: Faction, c: Offset, r: Float, color: Color) {
    when (fa) {
        Faction.FIRE -> {
            val p = Path().apply {
                moveTo(c.x, c.y - r * 1.2f)
                cubicTo(c.x + r, c.y - r * 0.2f, c.x + r * 0.7f, c.y + r, c.x, c.y + r)
                cubicTo(c.x - r * 0.7f, c.y + r, c.x - r, c.y - r * 0.2f, c.x, c.y - r * 1.2f)
                close()
            }
            drawPath(p, color = color)
        }
        Faction.WATER -> {
            val p = Path().apply {
                moveTo(c.x, c.y - r * 1.25f)
                cubicTo(c.x + r * 1.05f, c.y + r * 0.1f, c.x + r * 0.6f, c.y + r, c.x, c.y + r)
                cubicTo(c.x - r * 0.6f, c.y + r, c.x - r * 1.05f, c.y + r * 0.1f, c.x, c.y - r * 1.25f)
                close()
            }
            drawPath(p, color = color)
        }
        Faction.LIGHTNING -> {
            val p = Path().apply {
                moveTo(c.x + r * 0.35f, c.y - r * 1.2f)
                lineTo(c.x - r * 0.55f, c.y + r * 0.15f)
                lineTo(c.x + r * 0.05f, c.y + r * 0.15f)
                lineTo(c.x - r * 0.30f, c.y + r * 1.2f)
                lineTo(c.x + r * 0.60f, c.y - r * 0.25f)
                lineTo(c.x + r * 0.00f, c.y - r * 0.25f)
                close()
            }
            drawPath(p, color = color)
        }
        Faction.WIND -> {
            for (i in 0 until 3) {
                val yy = c.y - r * 0.5f + i * r * 0.5f
                val len = r * (1.1f - i * 0.2f)
                drawArc(
                    color = color,
                    startAngle = 160f,
                    sweepAngle = 200f,
                    useCenter = false,
                    topLeft = Offset(c.x - len, yy - r * 0.32f),
                    size = Size(len * 1.6f, r * 0.64f),
                    style = Stroke(width = f.u(0.006f), cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** 속성별 배경 이펙트 */
private fun DrawScope.drawFactionEffect(
    f: Frame,
    fa: Faction,
    rarity: Rarity,
    accent: Color,
    accentSoft: Color,
    phase: Float,
) {
    val density = when (rarity) {
        Rarity.COMMON -> 6
        Rarity.RARE -> 9
        Rarity.EPIC -> 13
        Rarity.LEGENDARY -> 18
    }
    when (fa) {
        Faction.FIRE -> {
            // 위로 흩날리는 불티
            for (i in 0 until density) {
                val seed = (i * 53 % 100) / 100f
                val t = ((seed + phase) % 1f)
                val ex = 0.10f + seed * 0.82f
                val ey = 0.56f - t * 0.48f
                val a = (1f - t) * 0.55f
                drawCircle(
                    color = if (i % 3 == 0) accentSoft.copy(alpha = a) else accent.copy(alpha = a),
                    radius = f.u(0.006f + (1f - t) * 0.008f),
                    center = f.p(ex + sin(t * 6.0).toFloat() * 0.02f, ey),
                )
            }
        }
        Faction.WATER -> {
            // 튀어오르는 물방울
            for (i in 0 until density) {
                val seed = (i * 41 % 100) / 100f
                val t = ((seed + phase) % 1f)
                val ex = 0.08f + seed * 0.86f
                val arc = 4f * t * (1f - t) // 포물선
                val ey = 0.57f - arc * 0.34f
                val a = (1f - t * 0.7f) * 0.55f
                drawCircle(
                    color = accentSoft.copy(alpha = a),
                    radius = f.u(0.005f + arc * 0.008f),
                    center = f.p(ex, ey),
                )
            }
        }
        Faction.LIGHTNING -> {
            // 지그재그 전격
            for (i in 0 until density / 3) {
                val seed = (i * 67 % 100) / 100f
                val on = ((seed + phase * 2f) % 1f) < 0.35f
                if (!on) continue
                val sx = 0.12f + seed * 0.74f
                val p = Path()
                var yy = 0.06f
                var xx = sx
                p.moveTo(f, xx, yy)
                repeat(4) { k ->
                    xx += if (k % 2 == 0) 0.045f else -0.035f
                    yy += 0.085f
                    p.lineTo(f, xx, yy)
                }
                drawPath(p, color = accent.copy(alpha = 0.55f), style = Stroke(width = f.u(0.008f), cap = StrokeCap.Round))
                drawPath(p, color = accentSoft.copy(alpha = 0.85f), style = Stroke(width = f.u(0.003f), cap = StrokeCap.Round))
            }
        }
        Faction.WIND -> {
            // 감아 도는 바람 궤적
            for (i in 0 until density / 2) {
                val seed = (i * 71 % 100) / 100f
                val t = ((seed + phase) % 1f)
                val cy = 0.20f + seed * 0.34f
                val w = 0.22f + t * 0.55f
                val a = (1f - t) * 0.42f
                drawArc(
                    color = accent.copy(alpha = a),
                    startAngle = 150f + t * 60f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = f.p(0.5f - w / 2f, cy - 0.05f),
                    size = Size(f.u(w), f.u(0.10f)),
                    style = Stroke(width = f.u(0.006f), cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** 전설 1 — 에너지 날개 */
private fun DrawScope.drawLegendaryWings(f: Frame, accent: Color, accentSoft: Color, phase: Float) {
    val flap = sin(phase * 2 * PI).toFloat() * 0.02f
    listOf(-1f, 1f).forEach { dir ->
        for (i in 0 until 5) {
            val t = i / 4f
            val baseX = 0.30f
            val baseY = 0.33f
            val len = 0.30f + t * 0.16f
            val spread = (0.10f + t * 0.26f + flap) * -1f
            val p = Path().apply {
                moveTo(f, baseX, baseY)
                cubicTo(
                    f,
                    baseX - len * 0.45f, baseY + spread * 0.6f,
                    baseX - len * 0.85f, baseY + spread * 1.1f,
                    baseX - len, baseY + spread * 1.5f * dir.coerceAtLeast(0.4f),
                )
            }
            val a = 0.55f - t * 0.28f
            drawPath(
                p,
                color = if (i % 2 == 0) accentSoft.copy(alpha = a) else accent.copy(alpha = a),
                style = Stroke(width = f.u(0.020f - t * 0.008f), cap = StrokeCap.Round),
            )
        }
    }
}

/** 전설 2 — 신발을 감싸는 드래곤 실루엣 */
private fun DrawScope.drawLegendaryCreature(
    f: Frame,
    fa: Faction,
    accent: Color,
    accentSoft: Color,
    phase: Float,
) {
    val wave = sin(phase * 2 * PI).toFloat()

    // 몸통 — 신발을 휘감는 S자 곡선
    val body = Path().apply {
        moveTo(f, 0.04f, 0.30f)
        cubicTo(f, 0.22f, 0.10f + wave * 0.015f, 0.46f, 0.10f, 0.62f, 0.20f)
        cubicTo(f, 0.78f, 0.30f, 0.86f, 0.44f, 0.98f, 0.40f)
    }
    drawPath(body, color = accent.copy(alpha = 0.30f), style = Stroke(width = f.u(0.055f), cap = StrokeCap.Round))
    drawPath(body, color = accent.copy(alpha = 0.70f), style = Stroke(width = f.u(0.026f), cap = StrokeCap.Round))
    drawPath(body, color = accentSoft.copy(alpha = 0.85f), style = Stroke(width = f.u(0.008f), cap = StrokeCap.Round))

    // 등지느러미
    for (i in 0 until 7) {
        val t = i / 6f
        val bx = 0.10f + t * 0.52f
        val by = 0.20f - sin(t * PI).toFloat() * 0.10f + wave * 0.012f
        val p = Path().apply {
            moveTo(f, bx, by)
            lineTo(f, bx + 0.020f, by - 0.055f - t * 0.02f)
            lineTo(f, bx + 0.042f, by)
            close()
        }
        drawPath(p, color = accentSoft.copy(alpha = 0.60f))
    }

    // 머리
    val headC = f.p(0.055f, 0.30f)
    val hr = f.u(0.055f)
    drawCircle(accent.copy(alpha = 0.35f), radius = hr * 1.5f, center = headC)
    val head = Path().apply {
        moveTo(headC.x - hr * 1.6f, headC.y)
        cubicTo(
            headC.x - hr * 0.6f, headC.y - hr * 1.1f,
            headC.x + hr * 0.8f, headC.y - hr * 0.9f,
            headC.x + hr * 1.1f, headC.y,
        )
        cubicTo(
            headC.x + hr * 0.8f, headC.y + hr * 0.9f,
            headC.x - hr * 0.6f, headC.y + hr * 1.0f,
            headC.x - hr * 1.6f, headC.y,
        )
        close()
    }
    drawPath(head, color = accent.copy(alpha = 0.85f))
    drawPath(head, color = accentSoft, style = Stroke(width = f.u(0.006f)))
    // 눈
    drawCircle(Color.White, radius = hr * 0.20f, center = Offset(headC.x - hr * 0.2f, headC.y - hr * 0.18f))
    // 뿔
    listOf(-0.55f, -0.15f).forEach { dy ->
        drawLine(
            color = accentSoft,
            start = Offset(headC.x + hr * 0.2f, headC.y + hr * dy),
            end = Offset(headC.x + hr * 1.5f, headC.y + hr * (dy - 0.9f)),
            strokeWidth = f.u(0.010f),
            cap = StrokeCap.Round,
        )
    }
}

/** 사이드 스트라이프 */
private fun DrawScope.drawStripe(
    f: Frame,
    sil: Silhouette,
    accent: Color,
    accentSoft: Color,
    accentDeep: Color,
    mt: Float,
    ct: Float,
) {
    val mid = (ct + 0.170f + mt) / 2f
    when (sil.stripe) {
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
        StripeStyle.SPLIT -> {
            // 갈라지는 두 갈래 — 상위 등급용
            val a = Path().apply {
                moveTo(f, 0.190f, mt - 0.005f)
                cubicTo(f, 0.360f, mid + 0.030f, 0.540f, mid - 0.020f, 0.780f, mid - 0.085f)
                lineTo(f, 0.792f, mid - 0.048f)
                cubicTo(f, 0.550f, mid + 0.014f, 0.370f, mid + 0.066f, 0.200f, mt + 0.022f)
                close()
            }
            drawPath(a, brush = Brush.horizontalGradient(listOf(accentDeep, accent)))
            val b = Path().apply {
                moveTo(f, 0.230f, mt + 0.010f)
                cubicTo(f, 0.400f, mid + 0.086f, 0.560f, mid + 0.040f, 0.720f, mid - 0.010f)
                lineTo(f, 0.730f, mid + 0.024f)
                cubicTo(f, 0.570f, mid + 0.074f, 0.410f, mid + 0.120f, 0.240f, mt + 0.042f)
                close()
            }
            drawPath(b, brush = Brush.horizontalGradient(listOf(accent, accentSoft)))
        }
    }
}

/** 히어로 연출 — 부유 + 광택 + 속성 이펙트 애니메이션 */
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
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "heroPhase",
    )
    val accent = Color(sneaker.faction.accent)

    Box(modifier, contentAlignment = Alignment.Center) {
        if (orbit) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.62f
                val rx = size.width * 0.46f
                val ry = size.height * 0.15f
                listOf(1.0f to 0.50f, 0.76f to 0.26f).forEach { (s, a) ->
                    drawOval(
                        color = accent.copy(alpha = a),
                        topLeft = Offset(cx - rx * s, cy - ry * s),
                        size = Size(rx * 2 * s, ry * 2 * s),
                        style = Stroke(width = size.minDimension * 0.007f),
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
            phase = phase,
        )
    }
}
