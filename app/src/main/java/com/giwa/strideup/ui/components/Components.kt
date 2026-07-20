package com.giwa.strideup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.ui.theme.NeonGreen
import com.giwa.strideup.ui.theme.NeonGreenSoft
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Stroke
import com.giwa.strideup.ui.theme.Surface2
import com.giwa.strideup.ui.theme.TextPrimary
import com.giwa.strideup.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** 공용 카드 컨테이너 (은은한 외곽선) */
@Composable
fun StrideCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Stroke, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        content()
    }
}

/** STRIDEUP 워드마크 (STRIDE 흰색 + UP 라임, 이탤릭 볼드) */
@Composable
fun StrideUpWordmark(fontSize: TextUnit = 24.sp) {
    Row {
        Text(
            "STRIDE",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = TextPrimary,
            letterSpacing = (-0.5).sp,
        )
        Text(
            "UP",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = NeonGreen,
            letterSpacing = (-0.5).sp,
        )
    }
}

/** 프로필 아바타 + 레벨 뱃지 */
@Composable
fun ProfileBadge(level: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .border(2.dp, NeonGreen, CircleShape)
                .padding(3.dp)
                .background(Surface2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = "프로필",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp),
            )
        }
        Box(
            modifier = Modifier
                .offset(y = 7.dp)
                .background(NeonGreen, RoundedCornerShape(50))
                .padding(horizontal = 7.dp, vertical = 1.dp),
        ) {
            Text(
                "Lv.$level",
                color = Night,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** 진행률 링 (네온 글로우 + 진행 헤드 점). 중앙에 content 배치. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 16.dp,
    trackColor: Color = Color.White.copy(alpha = 0.06f),
    content: @Composable () -> Unit,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = ringWidth.toPx()
            val inset = strokePx / 2 + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            if (clamped > 0f) {
                // 발광 bloom
                drawArc(
                    color = NeonGreen.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx * 2.4f, cap = StrokeCap.Round),
                )
                // 메인 아크
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(NeonGreen.copy(alpha = 0.55f), NeonGreen, NeonGreenSoft),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
                // 진행 헤드(발광 점)
                val angle = (-90f + 360f * clamped) * (PI / 180.0)
                val rx = arcSize.width / 2
                val ry = arcSize.height / 2
                val cx = topLeft.x + rx + rx * cos(angle).toFloat()
                val cy = topLeft.y + ry + ry * sin(angle).toFloat()
                drawCircle(NeonGreen.copy(alpha = 0.30f), radius = strokePx * 1.3f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = strokePx * 0.5f, center = Offset(cx, cy))
            }
        }
        content()
    }
}

/** 아이콘 + 값 + 라벨 스탯 항목 (Row 스코프에서 사용) */
@Composable
fun RowScope.StatItem(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 세그먼트형 에너지 게이지 */
@Composable
fun EnergyBar(
    current: Double,
    max: Double,
    modifier: Modifier = Modifier,
    segments: Int = 10,
) {
    val safeMax = if (max <= 0) 1.0 else max
    val fillRatio = (current / safeMax).coerceIn(0.0, 1.0)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(segments) { index ->
            val segmentStart = index.toDouble() / segments
            val filled = fillRatio > segmentStart + 1e-9
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(
                        color = if (filled) NeonGreen else Color.White.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(5.dp),
                    )
            )
        }
    }
}

/** 얇은 라운드 진행 바 */
@Composable
fun LineProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(NeonGreen, NeonGreenSoft))),
        )
    }
}

/** 원형 아이콘 배지 */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(tint.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size / 2))
    }
}

/** 육각형 토큰 뱃지 (가운데 S) */
@Composable
fun TokenBadge(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val r = min(this.size.width, this.size.height) / 2 * 0.94f
            val cx = this.size.width / 2
            val cy = this.size.height / 2
            val path = Path()
            for (i in 0 until 6) {
                val a = (-90f + i * 60f) * (PI / 180.0)
                val x = cx + r * cos(a).toFloat()
                val y = cy + r * sin(a).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = NeonGreen.copy(alpha = 0.16f))
            drawPath(path, color = NeonGreen, style = Stroke(width = 2.dp.toPx()))
        }
        Text("S", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = (size.value * 0.42f).sp)
    }
}

/** 발광 원형 컨트롤 버튼 (워킹 화면 컨트롤) */
@Composable
fun GlowCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    containerColor: Color = NeonGreen,
    glow: Boolean = true,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(size + 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (glow && enabled) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(containerColor.copy(alpha = 0.35f), Color.Transparent),
                        radius = this.size.minDimension / 2,
                    ),
                    radius = this.size.minDimension / 2,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    if (enabled) containerColor else containerColor.copy(alpha = 0.25f),
                    CircleShape,
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** 큼직한 START WALK 스타일 pill 버튼 */
@Composable
fun StartWalkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(NeonGreen, NeonGreenSoft))
                } else {
                    Brush.horizontalGradient(listOf(Surface2, Surface2))
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 28.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (enabled) Night else TextSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Night, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = if (enabled) NeonGreen else TextSecondary,
            )
        }
    }
}
