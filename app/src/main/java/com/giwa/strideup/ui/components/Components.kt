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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.ui.theme.CardFill
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.HairlineFade
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.NightBackdrop
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import com.giwa.strideup.ui.theme.VoltInk
import com.giwa.strideup.ui.theme.VoltPlate
import com.giwa.strideup.ui.theme.VoltSoft
import com.giwa.strideup.ui.theme.VoltSweep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────
// 배경
// ─────────────────────────────────────────────────────────────

/** 앱 전체 배경 — 딥 블랙 위에 아주 옅은 볼트 광원 하나. */
@Composable
fun NightCanvas(modifier: Modifier = Modifier) {
    Box(modifier.background(NightBackdrop)) {
        Canvas(Modifier.fillMaxSize()) {
            val glow = Offset(size.width * 0.85f, size.height * 0.02f)
            val radius = size.minDimension * 0.8f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Volt.copy(alpha = 0.05f), Color.Transparent),
                    center = glow,
                    radius = radius,
                ),
                radius = radius,
                center = glow,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 컨테이너 · 구분선
// ─────────────────────────────────────────────────────────────

/** 기본 카드 — 카본 표면 + 헤어라인. accent = true면 볼트 외곽선 + 은은한 글로우. */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    spacing: Dp = 13.dp,
    accent: Boolean = false,
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardFill, shape)
            .border(
                width = 1.dp,
                color = if (accent) Volt.copy(alpha = 0.40f) else Edge,
                shape = shape,
            )
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
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
                ),
            ),
    )
}

// ─────────────────────────────────────────────────────────────
// 타이포 요소
// ─────────────────────────────────────────────────────────────

/** 넓은 자간의 소형 대문자 라벨 */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Silver,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

/** 그라데이션 텍스트 (기본: 볼트) */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacing: TextUnit = 0.sp,
    brush: Brush = VoltInk,
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

/** StrideUp 워드마크 — 이탤릭 블랙, "Stride" 화이트 + "Up" 볼트 */
@Composable
fun Wordmark(fontSize: TextUnit = 20.sp, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Stride",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Snow,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text = "Up",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = Volt,
            letterSpacing = (-0.5).sp,
        )
    }
}

/** 섹션 헤더 (제목 + 우측 액션) */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Snow)
        if (actionText != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Volt,
                modifier = if (onAction != null) Modifier.quietClickable(onAction) else Modifier,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 헥사곤 · 배지
// ─────────────────────────────────────────────────────────────

private fun DrawScope.hexPath(cx: Float, cy: Float, radius: Float): Path = Path().apply {
    for (i in 0 until 6) {
        val angle = (-90f + i * 60f) * (PI / 180.0)
        val x = cx + radius * cos(angle).toFloat()
        val y = cy + radius * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** StrideTokens 헥사곤 엠블럼 — 이중 육각형 + 글로우 */
@Composable
fun HexEmblem(modifier: Modifier = Modifier, size: Dp = 42.dp, glow: Boolean = true) {
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = min(this.size.width, this.size.height) / 2f * 0.92f
        if (glow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Volt.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r * 1.4f,
                ),
                radius = r * 1.4f,
                center = Offset(cx, cy),
            )
        }
        drawPath(hexPath(cx, cy, r), color = Volt.copy(alpha = 0.10f))
        drawPath(hexPath(cx, cy, r), color = Volt, style = Stroke(width = 1.6.dp.toPx()))
        drawPath(hexPath(cx, cy, r * 0.55f), color = Volt, style = Stroke(width = 1.2.dp.toPx()))
        drawCircle(Volt, radius = r * 0.16f, center = Offset(cx, cy))
    }
}

/** 육각형 레벨 배지 — 카본 채움 + 볼트 스트로크 + 숫자 */
@Composable
fun HexBadge(text: String, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = min(this.size.width, this.size.height) / 2f * 0.96f
            drawPath(hexPath(cx, cy, r), color = Night)
            drawPath(hexPath(cx, cy, r), color = Volt, style = Stroke(width = 1.4.dp.toPx()))
        }
        Text(
            text = text,
            color = Snow,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 프로필 아바타 — 볼트 링 + 우하단 육각 레벨 배지 */
@Composable
fun LevelAvatar(
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    contentDescription: String? = null,
) {
    Box(modifier = modifier.size(size + 6.dp)) {
        Box(
            modifier = Modifier
                .size(size)
                .border(2.dp, Volt, CircleShape)
                .padding(4.dp)
                .background(CarbonHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = contentDescription,
                tint = Silver,
                modifier = Modifier.size(size * 0.44f),
            )
        }
        HexBadge(
            text = "$level",
            size = (size.value * 0.44f).dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 3.dp, y = 3.dp),
        )
    }
}

/** 겹쳐진 멤버 아바타 + "+N" 칩 */
@Composable
fun AvatarStack(visible: Int, extra: Int, modifier: Modifier = Modifier, dot: Dp = 24.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(visible) { index ->
            Box(
                modifier = Modifier
                    .offset(x = (-6 * index).dp)
                    .size(dot)
                    .border(1.5.dp, Night, CircleShape)
                    .background(CarbonHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Silver,
                    modifier = Modifier.size(dot * 0.55f),
                )
            }
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-6 * visible).dp)
                    .clip(RoundedCornerShape(50))
                    .background(CarbonHigh)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("+$extra", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 게이지
// ─────────────────────────────────────────────────────────────

/** 네온 진행 링 — 볼트 스윕 + 확산광 + 폴리시드 헤드 */
@Composable
fun NeonRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 13.dp,
    glowAlpha: Float = 0.16f,
    content: @Composable () -> Unit,
) {
    val swept = animatedFloat(progress.coerceIn(0f, 1f), durationMillis = 900)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = ringWidth.toPx()
            val inset = stroke / 2f + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (swept > 0.002f) {
                drawArc(
                    color = Volt.copy(alpha = glowAlpha),
                    startAngle = -90f,
                    sweepAngle = 360f * swept,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 2.2f, cap = StrokeCap.Round),
                )
                drawArc(
                    brush = VoltSweep,
                    startAngle = -90f,
                    sweepAngle = 360f * swept,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                val angle = (-90f + 360f * swept) * (PI / 180.0)
                val rx = arcSize.width / 2f
                val ry = arcSize.height / 2f
                val hx = topLeft.x + rx + rx * cos(angle).toFloat()
                val hy = topLeft.y + ry + ry * sin(angle).toFloat()
                drawCircle(Volt.copy(alpha = 0.35f), radius = stroke * 1.05f, center = Offset(hx, hy))
                drawCircle(Color.White, radius = stroke * 0.30f, center = Offset(hx, hy))
            }
        }
        content()
    }
}

/** 얇은 볼트 진행 바 */
@Composable
fun BarMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    val filled = animatedFloat(fraction.coerceIn(0f, 1f))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        if (filled > 0.002f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(filled)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(VoltPlate),
            )
        }
    }
}

/** 세그먼트 에너지 게이지 — 마지막 칸은 부분 충전까지 표현 */
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
                    .background(Color.White.copy(alpha = 0.07f)),
            ) {
                if (fill > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .fillMaxHeight()
                            .background(VoltPlate),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 버튼 · 컨트롤
// ─────────────────────────────────────────────────────────────

/** START RUN — 큼직한 볼트 CTA. 좌측 헥사곤 배지 + 중앙 타이틀 + 우측 화살표. */
@Composable
fun StartRunButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(shape)
            .background(VoltPlate, shape)
            .sheen(alpha = 0.22f)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(46.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawPath(hexPath(cx, cy, size.minDimension / 2f * 0.95f), color = Night)
            }
            Icon(icon, contentDescription = null, tint = Volt, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = Night,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )
            Text(
                text = subtitle,
                color = Night.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Night, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Volt,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 볼트 pill 버튼 (I'M IN, Claim 등) */
@Composable
fun VoltButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(
                if (enabled) VoltPlate else Brush.horizontalGradient(listOf(CarbonHigh, CarbonHigh)),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Night else Slate,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
    }
}

/** 외곽선 고스트 버튼 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Volt,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(accent.copy(alpha = if (enabled) 0.08f else 0.03f), shape)
            .border(1.dp, accent.copy(alpha = if (enabled) 0.45f else 0.15f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) accent else Slate,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}

/** 필터 칩 (Discover / Crews / …) */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badge: Int = 0,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Volt.copy(alpha = 0.12f) else CarbonHigh, shape)
            .border(1.dp, if (selected) Volt.copy(alpha = 0.55f) else Edge, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Volt else Silver,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            color = if (selected) Volt else Silver,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Volt, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$badge", color = Night, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 상단 우측 다크 아이콘 버튼 (알림 · 지갑) */
@Composable
fun DarkIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
) {
    Box(modifier = modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(CarbonHigh)
                .border(1.dp, Edge, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = Snow, modifier = Modifier.size(19.dp))
        }
        if (badge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp)
                    .size(8.dp)
                    .border(1.5.dp, Night, CircleShape)
                    .background(Volt, CircleShape),
            )
        }
    }
}

/** 원형 컨트롤 (러닝 화면) */
@Composable
fun CircleControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    accent: Color = Volt,
    filled: Boolean = true,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (filled) {
                    Modifier.background(
                        if (enabled) VoltPlate else Brush.horizontalGradient(listOf(CarbonHigh, CarbonHigh)),
                        CircleShape,
                    )
                } else {
                    Modifier
                        .background(CarbonHigh, CircleShape)
                        .border(1.5.dp, accent.copy(alpha = if (enabled) 0.45f else 0.15f), CircleShape)
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 리플 없는 클릭 영역 */
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

/** 라운드 사각 아이콘 배지 */
@Composable
fun IconSquare(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Volt,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.11f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** 아이콘 · 값 · 라벨 3단 스탯 (Row 스코프) */
@Composable
fun RowScope.StatCell(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color = Volt,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconSquare(icon = icon, tint = tint, size = 34.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Silver,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Snow,
            textAlign = TextAlign.Center,
        )
    }
}

/** 설정 리스트 행 */
@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CarbonHigh.copy(alpha = 0.6f), shape)
            .border(1.dp, Edge, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Silver, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = Snow,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Slate, modifier = Modifier.size(18.dp))
    }
}

/** 루트 맵 — 다크 지도 위 볼트 글로우 궤적 */
@Composable
fun RouteMap(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        val gridColor = Color.White.copy(alpha = 0.045f)
        for (c in 1 until 6) {
            val x = w * c / 6
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        for (r in 1 until 4) {
            val y = h * r / 4
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        val points = listOf(
            0.06f to 0.82f, 0.22f to 0.62f, 0.36f to 0.70f, 0.50f to 0.44f,
            0.63f to 0.52f, 0.78f to 0.28f, 0.92f to 0.18f,
        ).map { Offset(w * it.first, h * it.second) }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        drawPath(path, Volt.copy(alpha = 0.22f), style = Stroke(width = 13f, cap = StrokeCap.Round))
        drawPath(path, Volt, style = Stroke(width = 4f, cap = StrokeCap.Round))

        drawCircle(Volt.copy(alpha = 0.30f), radius = 12f, center = points.first())
        drawCircle(Night, radius = 6f, center = points.first())
        drawCircle(Volt, radius = 6f, center = points.first(), style = Stroke(width = 2.5f))

        drawCircle(Volt.copy(alpha = 0.35f), radius = 15f, center = points.last())
        drawCircle(Volt, radius = 6.5f, center = points.last())
    }
}

/** 토큰 잔액 카드 — 헥사곤 엠블럼 + StrideTokens + 환산가 */
@Composable
fun TokenCard(
    balance: Double,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(CardFill, shape)
            .border(1.dp, Edge, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HexEmblem(size = 38.dp)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("StrideTokens", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "%,.2f".format(balance),
                color = Snow,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "≈ $%,.2f".format(balance * 0.01),
                    color = Slate,
                    fontSize = 10.sp,
                )
                Text(
                    text = "+0.51%",
                    color = Volt,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (onClick != null) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
