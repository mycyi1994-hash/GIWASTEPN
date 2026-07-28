package com.giwa.strideup.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.ui.theme.Border
import com.giwa.strideup.ui.theme.CardSheen
import com.giwa.strideup.ui.theme.CardWhite
import com.giwa.strideup.ui.theme.Coral
import com.giwa.strideup.ui.theme.CoralDeep
import com.giwa.strideup.ui.theme.Cream
import com.giwa.strideup.ui.theme.CreamBackdrop
import com.giwa.strideup.ui.theme.HairlineFade
import com.giwa.strideup.ui.theme.Honey
import com.giwa.strideup.ui.theme.HoneyPlate
import com.giwa.strideup.ui.theme.Ink
import com.giwa.strideup.ui.theme.Peach
import com.giwa.strideup.ui.theme.Sand
import com.giwa.strideup.ui.theme.SunsetInk
import com.giwa.strideup.ui.theme.SunsetPlate
import com.giwa.strideup.ui.theme.SunsetSweep
import com.giwa.strideup.ui.theme.Taupe
import com.giwa.strideup.ui.theme.TaupeLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────
// 배경
// ─────────────────────────────────────────────────────────────

/** 앱 전체 배경. 크림 그라데이션 위에 아침 햇살 같은 옅은 광원 두 개를 얹는다. */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(CreamBackdrop)) {
        Canvas(Modifier.fillMaxSize()) {
            val sun = Offset(size.width * 0.90f, size.height * 0.02f)
            val sunRadius = size.minDimension * 0.9f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Peach.copy(alpha = 0.10f), Color.Transparent),
                    center = sun,
                    radius = sunRadius,
                ),
                radius = sunRadius,
                center = sun,
            )
            val glow = Offset(size.width * 0.04f, size.height * 0.86f)
            val glowRadius = size.minDimension * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Honey.copy(alpha = 0.07f), Color.Transparent),
                    center = glow,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = glow,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 컨테이너 · 구분선
// ─────────────────────────────────────────────────────────────

/**
 * 종이 질감 카드. 딱딱한 외곽선 대신 부드러운 이중 그림자가 면을 띄운다.
 * accent = true면 코럴 기운이 도는 히어로 카드가 된다.
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    spacing: Dp = 14.dp,
    accent: Boolean = false,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (accent) 14.dp else 8.dp,
                shape = shape,
                spotColor = if (accent) Coral.copy(alpha = 0.30f) else Ink.copy(alpha = 0.16f),
                ambientColor = Ink.copy(alpha = 0.08f),
            )
            .background(CardSheen, shape)
            .border(1.dp, if (accent) Coral.copy(alpha = 0.20f) else Border, shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** 양끝이 사라지는 수평 헤어라인 */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(HairlineFade),
    )
}

/** 카드 안에서 좌우를 나누는 수직 헤어라인 */
@Composable
fun VerticalHairline(height: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(1.dp)
            .height(height)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Ink.copy(alpha = 0.10f), Color.Transparent),
                ),
            ),
    )
}

// ─────────────────────────────────────────────────────────────
// 타이포 요소
// ─────────────────────────────────────────────────────────────

/** 소형 대문자 라벨 — 데이터 위에 얹는 "눈썹" */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Taupe,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

/** 그라데이션으로 채운 텍스트 (기본: 선셋) */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    letterSpacing: TextUnit = 0.sp,
    brush: Brush = SunsetInk,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = TextStyle(
            brush = brush,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
        ),
    )
}

/** STRIDEUP 워드마크 — 둥근 굵은 획 + 선셋 포인트 */
@Composable
fun Wordmark(fontSize: TextUnit = 19.sp, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Stride",
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
            letterSpacing = (-0.3).sp,
        )
        GradientText(
            text = "Up",
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
        )
    }
}

/** 화면 상단 타이틀 블록 (눈썹 라벨 + 큼직한 제목) */
@Composable
fun ScreenTitle(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Eyebrow(eyebrow, color = Coral)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
            )
        }
        trailing?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────
// 배지 · 엠블럼
// ─────────────────────────────────────────────────────────────

/** 프로필 아바타 + 선셋 링 + 레벨 배지 */
@Composable
fun MemberBadge(level: Int, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .border(2.dp, SunsetPlate, CircleShape)
                .padding(4.dp)
                .background(Cream, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "프로필",
                tint = Taupe,
                modifier = Modifier.size(size * 0.42f),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 7.dp)
                .clip(RoundedCornerShape(50))
                .background(SunsetPlate)
                .padding(horizontal = 7.dp, vertical = 1.dp),
        ) {
            Text(
                text = "LV $level",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

/** 아이콘 메달리온 — 파스텔 채움의 둥근 아이콘 */
@Composable
fun IconMedallion(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Coral,
    size: Dp = 42.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

/** SUP 토큰 코인 — 허니 그라데이션 동전 */
@Composable
fun TokenCoin(modifier: Modifier = Modifier, size: Dp = 42.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                spotColor = Honey.copy(alpha = 0.45f),
                ambientColor = Honey.copy(alpha = 0.2f),
            )
            .background(HoneyPlate, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // 동전 내림 각인 링
            drawCircle(
                color = Color.White.copy(alpha = 0.40f),
                radius = this.size.minDimension / 2 * 0.72f,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
        Text(
            text = "S",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.38f).sp,
        )
    }
}

/** 기요셰(guilloché) 무늬 — 히어로 카드에 은은하게 새기는 동심원 결 */
@Composable
fun GuillochePattern(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val cx = size.width * 0.80f
        val cy = size.height * 0.42f
        val step = size.minDimension * 0.09f
        repeat(9) { i ->
            drawCircle(
                color = color.copy(alpha = 0.07f),
                radius = step * (i + 2),
                center = Offset(cx, cy),
                style = Stroke(width = 1.2f),
            )
        }
        repeat(7) { i ->
            drawCircle(
                color = color.copy(alpha = 0.05f),
                radius = step * (i + 2) * 0.8f,
                center = Offset(cx - step * 1.8f, cy + step * 1.1f),
                style = Stroke(width = 1.2f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 게이지
// ─────────────────────────────────────────────────────────────

/**
 * 선셋 진행 링. 샌드 트랙 위로 코럴→살구 그라데이션 아크가 차오른다.
 */
@Composable
fun SunsetRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 14.dp,
    glowAlpha: Float = 0.12f,
    content: @Composable () -> Unit,
) {
    val swept = animatedFloat(progress.coerceIn(0f, 1f), durationMillis = 900)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = ringWidth.toPx()
            val inset = stroke / 2f + 7.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // 트랙
            drawArc(
                color = Sand,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (swept > 0.002f) {
                // 따뜻한 확산광
                drawArc(
                    color = Coral.copy(alpha = glowAlpha),
                    startAngle = -90f,
                    sweepAngle = 360f * swept,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 2.1f, cap = StrokeCap.Round),
                )
                // 선셋 아크
                drawArc(
                    brush = SunsetSweep,
                    startAngle = -90f,
                    sweepAngle = 360f * swept,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // 진행 헤드 — 흰 점
                val angle = (-90f + 360f * swept) * (PI / 180.0)
                val rx = arcSize.width / 2f
                val ry = arcSize.height / 2f
                val hx = topLeft.x + rx + rx * cos(angle).toFloat()
                val hy = topLeft.y + ry + ry * sin(angle).toFloat()
                drawCircle(Coral.copy(alpha = 0.30f), radius = stroke * 1.05f, center = Offset(hx, hy))
                drawCircle(Color.White, radius = stroke * 0.32f, center = Offset(hx, hy))
            }
        }
        content()
    }
}

/** 얇은 선셋 진행 바 */
@Composable
fun LineMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
) {
    val filled = animatedFloat(fraction.coerceIn(0f, 1f))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Sand),
    ) {
        if (filled > 0.002f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(filled)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(SunsetPlate),
            )
        }
    }
}

/** 세그먼트 에너지 게이지 — 허니 그라데이션. 마지막 칸은 부분 충전까지 표현한다. */
@Composable
fun EnergyMeter(
    current: Double,
    max: Double,
    modifier: Modifier = Modifier,
    segments: Int = 12,
) {
    val safeMax = if (max <= 0.0) 1.0 else max
    val exact = (current / safeMax).coerceIn(0.0, 1.0) * segments
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(segments) { index ->
            val fill = (exact - index).coerceIn(0.0, 1.0).toFloat()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Sand),
            ) {
                if (fill > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .fillMaxHeight()
                            .background(HoneyPlate),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 버튼 · 컨트롤
// ─────────────────────────────────────────────────────────────

/** 주 CTA — 선셋 그라데이션 pill. 따뜻한 그림자가 배경으로 번진다. */
@Composable
fun SunsetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                spotColor = Coral.copy(alpha = 0.45f),
                ambientColor = Coral.copy(alpha = 0.18f),
            )
            .background(
                if (enabled) SunsetPlate else Brush.horizontalGradient(listOf(Sand, Sand)),
                shape,
            )
            .clip(shape)
            .then(if (enabled) Modifier.sheen(alpha = 0.22f) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (enabled) Color.White else TaupeLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
        )
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (enabled) CardWhite else Color.White.copy(alpha = 0.55f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (enabled) CoralDeep else TaupeLight,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/** 보조 CTA — 파스텔 칩 버튼 */
@Composable
fun ChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Coral,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(accent.copy(alpha = if (enabled) 0.11f else 0.05f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) accent else TaupeLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
        )
    }
}

/** 워킹 화면의 원형 컨트롤 */
@Composable
fun CircleControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    accent: Color = Coral,
    filled: Boolean = true,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (enabled) (if (filled) 10.dp else 5.dp) else 0.dp,
                shape = CircleShape,
                spotColor = if (filled) Coral.copy(alpha = 0.40f) else Ink.copy(alpha = 0.14f),
                ambientColor = Ink.copy(alpha = 0.06f),
            )
            .then(
                if (filled) {
                    Modifier.background(
                        if (enabled) SunsetPlate else Brush.horizontalGradient(listOf(Sand, Sand)),
                        CircleShape,
                    )
                } else {
                    Modifier
                        .background(CardWhite, CircleShape)
                        .border(1.5.dp, accent.copy(alpha = if (enabled) 0.40f else 0.15f), CircleShape)
                },
            )
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 리플 없이 눌리는 영역 — 하단 탭처럼 조용해야 하는 곳에 쓴다. */
@Composable
fun Modifier.quietClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
}

// ─────────────────────────────────────────────────────────────
// 데이터 표시
// ─────────────────────────────────────────────────────────────

/** 파스텔 아이콘 · 값 · 라벨 3단 스탯 (Row 스코프) */
@Composable
fun RowScope.StatColumn(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconMedallion(icon = icon, tint = tint, size = 32.dp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Taupe,
        )
    }
}

/** 좌: 라벨 / 우: 값 한 줄 */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Ink,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Taupe,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
        )
    }
}
