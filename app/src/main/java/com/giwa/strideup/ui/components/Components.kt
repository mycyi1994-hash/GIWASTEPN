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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.giwa.strideup.ui.theme.Ash
import com.giwa.strideup.ui.theme.AshDim
import com.giwa.strideup.ui.theme.Champagne
import com.giwa.strideup.ui.theme.ChampagneLight
import com.giwa.strideup.ui.theme.CharcoalHigh
import com.giwa.strideup.ui.theme.GlassEdge
import com.giwa.strideup.ui.theme.GlassFill
import com.giwa.strideup.ui.theme.GoldEdge
import com.giwa.strideup.ui.theme.HairlineFade
import com.giwa.strideup.ui.theme.Ivory
import com.giwa.strideup.ui.theme.MetalInk
import com.giwa.strideup.ui.theme.MetalPlate
import com.giwa.strideup.ui.theme.MetalSweep
import com.giwa.strideup.ui.theme.ObsidianBackdrop
import com.giwa.strideup.ui.theme.ObsidianDeep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────
// 배경
// ─────────────────────────────────────────────────────────────

/** 앱 전체 배경. 흑요석 그라데이션 위에 아주 옅은 금빛 광원 두 개를 얹는다. */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(ObsidianBackdrop)) {
        Canvas(Modifier.fillMaxSize()) {
            val topGlow = Offset(size.width * 0.88f, size.height * 0.04f)
            val topRadius = size.minDimension * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Champagne.copy(alpha = 0.055f), Color.Transparent),
                    center = topGlow,
                    radius = topRadius,
                ),
                radius = topRadius,
                center = topGlow,
            )
            val bottomGlow = Offset(size.width * 0.05f, size.height * 0.82f)
            val bottomRadius = size.minDimension * 0.7f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Champagne.copy(alpha = 0.028f), Color.Transparent),
                    center = bottomGlow,
                    radius = bottomRadius,
                ),
                radius = bottomRadius,
                center = bottomGlow,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 컨테이너 · 구분선
// ─────────────────────────────────────────────────────────────

/** 유리 표면 카드. 1px 엣지 라이팅이 고급감의 8할을 만든다. */
@Composable
fun LuxeCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    spacing: Dp = 14.dp,
    accent: Boolean = false,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .background(GlassFill, shape)
            .border(1.dp, if (accent) GoldEdge else GlassEdge, shape)
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

/** 넓은 자간의 소형 대문자 라벨 — 섹션 위에 얹는 "눈썹" */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ash,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

/** 금속 그라데이션으로 채운 텍스트 */
@Composable
fun MetalText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Light,
    letterSpacing: TextUnit = 0.sp,
    brush: Brush = MetalInk,
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

/** STRIDEUP 워드마크 — 가는 획 + 넓은 자간의 럭셔리 로고타입 */
@Composable
fun Wordmark(fontSize: TextUnit = 19.sp, modifier: Modifier = Modifier) {
    val tracking = (fontSize.value * 0.24f).sp
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "STRIDE",
            fontSize = fontSize,
            fontWeight = FontWeight.Light,
            color = Ivory,
            letterSpacing = tracking,
        )
        MetalText(
            text = "UP",
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = tracking,
        )
    }
}

/** 화면 상단 타이틀 블록 (눈썹 라벨 + 가는 대제목) */
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
            Eyebrow(eyebrow, color = Champagne.copy(alpha = 0.75f))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Ivory,
            )
        }
        trailing?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────
// 배지 · 엠블럼
// ─────────────────────────────────────────────────────────────

/** 프로필 아바타 + 금속 링 + 레벨 배지 */
@Composable
fun MemberBadge(level: Int, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .border(1.dp, MetalSweep, CircleShape)
                .padding(4.dp)
                .background(CharcoalHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "프로필",
                tint = Ash,
                modifier = Modifier.size(size * 0.44f),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 7.dp)
                .clip(RoundedCornerShape(50))
                .background(MetalPlate)
                .padding(horizontal = 7.dp, vertical = 1.dp),
        ) {
            Text(
                text = "LV $level",
                color = ObsidianDeep,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
        }
    }
}

/** 아이콘 메달리온 — 옅은 채움 + 얇은 링 */
@Composable
fun IconMedallion(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Champagne,
    size: Dp = 42.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.26f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

/** SUP 토큰 엠블럼 — 이중 육각형에 금속 스트로크 */
@Composable
fun TokenEmblem(modifier: Modifier = Modifier, size: Dp = 42.dp) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val outer = min(this.size.width, this.size.height) / 2f

            fun hexagon(radius: Float): Path = Path().apply {
                for (i in 0 until 6) {
                    val angle = (-90f + i * 60f) * (PI / 180.0)
                    val x = cx + radius * cos(angle).toFloat()
                    val y = cy + radius * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }

            drawPath(hexagon(outer * 0.97f), color = Champagne.copy(alpha = 0.07f))
            drawPath(hexagon(outer * 0.97f), brush = MetalSweep, style = Stroke(width = 1.4.dp.toPx()))
            drawPath(
                hexagon(outer * 0.70f),
                color = Champagne.copy(alpha = 0.16f),
                style = Stroke(width = 0.8.dp.toPx()),
            )
        }
        MetalText(
            text = "S",
            fontSize = (size.value * 0.34f).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 멤버십 카드에 새기는 기요셰(guilloché) 무늬 — 지폐·시계 다이얼의 그 결 */
@Composable
fun GuillocheOverlay(modifier: Modifier = Modifier, color: Color = ObsidianDeep) {
    Canvas(modifier) {
        val cx = size.width * 0.78f
        val cy = size.height * 0.5f
        val step = size.minDimension * 0.055f
        repeat(9) { i ->
            drawCircle(
                color = color.copy(alpha = 0.05f),
                radius = step * (i + 2),
                center = Offset(cx, cy),
                style = Stroke(width = 1f),
            )
        }
        repeat(7) { i ->
            drawCircle(
                color = color.copy(alpha = 0.035f),
                radius = step * (i + 2) * 0.8f,
                center = Offset(cx - step * 1.6f, cy + step * 0.9f),
                style = Stroke(width = 1f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 게이지
// ─────────────────────────────────────────────────────────────

/**
 * 금속 진행 링. 네온 발광 대신 얕은 앰비언트 확산 + 폴리시드 헤드로 마감한다.
 */
@Composable
fun MetalRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 12.dp,
    glowAlpha: Float = 0.10f,
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
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (swept > 0.002f) {
                // 앰비언트 확산 (금속이 주변을 은은히 물들이는 정도)
                drawArc(
                    color = Champagne.copy(alpha = glowAlpha),
                    startAngle = -90f,
                    sweepAngle = 360f * swept,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 2.3f, cap = StrokeCap.Round),
                )
                // 금속 아크
                drawArc(
                    brush = MetalSweep,
                    startAngle = -90f,
                    sweepAngle = 360f * swept,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // 폴리시드 헤드
                val angle = (-90f + 360f * swept) * (PI / 180.0)
                val rx = arcSize.width / 2f
                val ry = arcSize.height / 2f
                val hx = topLeft.x + rx + rx * cos(angle).toFloat()
                val hy = topLeft.y + ry + ry * sin(angle).toFloat()
                drawCircle(Champagne.copy(alpha = 0.22f), radius = stroke * 1.1f, center = Offset(hx, hy))
                drawCircle(ChampagneLight, radius = stroke * 0.34f, center = Offset(hx, hy))
            }
        }
        content()
    }
}

/** 얇은 금속 진행 바 */
@Composable
fun LineMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val filled = animatedFloat(fraction.coerceIn(0f, 1f))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.07f)),
    ) {
        if (filled > 0.002f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(filled)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MetalPlate),
            )
        }
    }
}

/** 세그먼트 에너지 게이지. 마지막 칸은 부분 충전까지 표현한다. */
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
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
            ) {
                if (fill > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .fillMaxHeight()
                            .background(MetalPlate),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 버튼 · 컨트롤
// ─────────────────────────────────────────────────────────────

/** 주 CTA — 금속판 pill 위로 광택이 흐른다. */
@Composable
fun GoldButton(
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
            .clip(shape)
            .background(
                if (enabled) MetalPlate else Brush.horizontalGradient(listOf(CharcoalHigh, CharcoalHigh)),
                shape,
            )
            .then(if (enabled) Modifier.sheen(alpha = 0.20f) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 26.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.weight(1f),
            color = if (enabled) ObsidianDeep else AshDim,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.2.sp,
        )
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (enabled) ObsidianDeep.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.04f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (enabled) Champagne else AshDim,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/** 보조 CTA — 채움 없이 헤어라인만 두른 고스트 버튼 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Champagne,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(accent.copy(alpha = if (enabled) 0.07f else 0.02f), shape)
            .border(1.dp, accent.copy(alpha = if (enabled) 0.32f else 0.12f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) accent else AshDim,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
        )
    }
}

/** 워킹 화면의 원형 컨트롤. 활성 시 뒤로 옅은 헤일로가 퍼진다. */
@Composable
fun CircleControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    accent: Color = Champagne,
    filled: Boolean = true,
    halo: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(size + 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (halo && enabled) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = this.size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                        radius = radius,
                    ),
                    radius = radius,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (filled && enabled) {
                        Modifier.background(MetalPlate)
                    } else {
                        Modifier
                            .background(CharcoalHigh)
                            .border(1.dp, accent.copy(alpha = if (enabled) 0.35f else 0.12f), CircleShape)
                    },
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
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

/** 아이콘 · 값 · 라벨 3단 스탯 (Row 스코프) */
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
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Ivory,
            textAlign = TextAlign.Center,
        )
        Eyebrow(label, color = AshDim)
    }
}

/** 좌: 라벨 / 우: 값 한 줄 */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Ivory,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Ash,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
        )
    }
}
