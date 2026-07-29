package com.giwa.strideup.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.components.AllSneakerImages
import com.giwa.strideup.ui.components.fadedEdges
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import com.giwa.strideup.ui.theme.VoltSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 앱 진입 스플래시.
 *
 * 목업 그대로 로고 · 스니커즈 · 원형 로딩을 보여주면서 그 뒤로 실제 준비 작업
 * (스타터 스니커즈 지급, 만료 부스트 정리, 저장소 첫 방출 대기)을 수행한다.
 * 준비가 일찍 끝나도 최소 노출 시간은 지켜 화면이 깜빡이지 않게 한다.
 */
private const val MIN_VISIBLE_MILLIS = 2100L

@Composable
fun SplashScreen(onReady: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(420, easing = LinearEasing),
        label = "splashProgress",
    )
    // 실행할 때마다 다른 NFT를 세워둔다
    val heroImage = remember { AllSneakerImages.random() }

    LaunchedEffect(Unit) {
        val startedAt = System.currentTimeMillis()

        // 실제 준비 작업 — 각 단계가 끝날 때마다 진행률을 올린다.
        progress = 0.15f
        withTimeoutOrNull(4_000) {
            ServiceLocator.userPrefs.ensureRunnerUid()
            ServiceLocator.sneakerRepository.ensureStarter()
        }

        progress = 0.34f
        withTimeoutOrNull(2_000) {
            ServiceLocator.boostRepository.purgeExpired()
        }

        progress = 0.50f
        withTimeoutOrNull(3_000) {
            ServiceLocator.crewRepository.ensureSeeded()
            ServiceLocator.communityRepository.ensureSeeded()
            ServiceLocator.notificationRepository.seedWelcome()
        }

        progress = 0.68f
        // 첫 방출을 기다려 홈이 0으로 깜빡이지 않게 한다.
        withTimeoutOrNull(3_000) {
            ServiceLocator.rewardRepository.balance.first()
            ServiceLocator.stepRepository.dailyGoal.first()
        }

        progress = 0.86f
        withTimeoutOrNull(2_000) {
            ServiceLocator.sneakerRepository.equipped.first()
        }

        progress = 1f
        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed < MIN_VISIBLE_MILLIS) delay(MIN_VISIBLE_MILLIS - elapsed)
        delay(260) // 100% 를 잠깐 보여준다
        onReady()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night),
        contentAlignment = Alignment.Center,
    ) {
        SpeedBackdrop(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 로고 + 워드마크 + 슬로건 (전부 네이티브 — 배경과 완전히 이어진다)
            HexEmblem(size = 64.dp)
            Spacer(Modifier.height(14.dp))
            Wordmark(fontSize = 44.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                fontSize = 13.sp,
                color = Silver,
                textAlign = TextAlign.Center,
            )

            // 실행할 때마다 다른 스니커즈 NFT 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 34.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 카드 뒤 볼트 글로우
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Volt.copy(alpha = 0.16f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.62f,
                        ),
                        radius = size.minDimension * 0.62f,
                        center = center,
                    )
                }
                // 테두리 없이 가장자리를 녹여 스피드 라인 배경 위에 자연스럽게 띄운다
                Image(
                    painter = painterResource(heroImage),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .fadedEdges(0.2f),
                    contentScale = ContentScale.Fit,
                )
            }

            // 원형 진행률
            Box(
                modifier = Modifier.size(148.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.055f
                    val inset = stroke / 2f + size.minDimension * 0.04f
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = Color.White.copy(alpha = 0.07f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    if (animated > 0.004f) {
                        drawArc(
                            color = Volt.copy(alpha = 0.22f),
                            startAngle = -90f,
                            sweepAngle = 360f * animated,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke * 2.4f, cap = StrokeCap.Round),
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Volt, VoltSoft, Volt)),
                            startAngle = -90f,
                            sweepAngle = 360f * animated,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animated * 100).toInt()}%",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                        color = Snow,
                    )
                    Text(
                        text = stringResource(R.string.splash_loading),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp,
                        color = Slate,
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = stringResource(R.string.splash_syncing),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Volt,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.splash_preparing),
                fontSize = 13.sp,
                color = Silver,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // 하단 헥사곤 + 좌우 라인
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Volt.copy(alpha = 0.35f)),
                            ),
                        ),
                )
                Spacer(Modifier.width(12.dp))
                HexEmblem(size = 26.dp, glow = false)
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Volt.copy(alpha = 0.35f), Color.Transparent),
                            ),
                        ),
                )
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

/** 도시를 가로지르는 속도선 배경 (목업의 모션 블러 느낌) */
@Composable
private fun SpeedBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "speed")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "speedShift",
    )

    Canvas(modifier) {
        // 아래쪽에서 퍼지는 라임 안개
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Volt.copy(alpha = 0.06f), Color.Transparent),
                startY = size.height * 0.45f,
                endY = size.height,
            ),
        )

        // 원근 속도선 — 화면 중앙 소실점에서 바깥으로 흐른다
        val vpX = size.width * 0.5f
        val vpY = size.height * 0.62f
        val lines = 22
        for (i in 0 until lines) {
            val seed = (i * 37 % 100) / 100f
            val t = ((seed + shift) % 1f)
            // t가 커질수록 소실점에서 멀어지고 길고 진해진다
            val spread = t * t
            val angle = (seed * 2f - 1f) * 1.15f
            val startD = 0.06f + spread * 0.55f
            val endD = startD + 0.05f + spread * 0.28f
            val alpha = (t * 0.55f).coerceIn(0f, 0.55f) * (1f - t * 0.35f)
            drawLine(
                color = if (i % 4 == 0) VoltSoft.copy(alpha = alpha) else Volt.copy(alpha = alpha * 0.8f),
                start = Offset(vpX + angle * size.width * startD, vpY + spread * size.height * 0.22f * (if (i % 2 == 0) 1f else -1f)),
                end = Offset(vpX + angle * size.width * endD, vpY + spread * size.height * 0.30f * (if (i % 2 == 0) 1f else -1f)),
                strokeWidth = size.minDimension * (0.002f + spread * 0.004f),
                cap = StrokeCap.Round,
            )
        }
    }
}
