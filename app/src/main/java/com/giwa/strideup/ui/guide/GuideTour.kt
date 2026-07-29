package com.giwa.strideup.ui.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/**
 * 스포트라이트 가이드 투어.
 *
 * 실제 화면 위에 어두운 오버레이를 깔고, 설명하려는 요소만 밝게 뚫어
 * 그 옆에 설명 창을 띄운다. 스텝이 다른 탭으로 넘어가면 탭도 자동 전환된다.
 */

data class GuideStep(
    /** [Modifier.guideTarget]로 등록한 대상 키 */
    val key: String,
    /** 이 스텝이 속한 하단 탭 라우트 */
    val tabRoute: String,
    val titleRes: Int,
    val bodyRes: Int,
)

object GuideTour {

    var active by mutableStateOf(false)
        private set

    var stepIndex by mutableIntStateOf(0)
        private set

    /** 화면 요소들이 자기 위치를 등록하는 곳 (루트 좌표) */
    val bounds = mutableStateMapOf<String, Rect>()

    object Targets {
        const val HOME_STEPS = "home_steps"
        const val HOME_ENERGY = "home_energy"
        const val HOME_START_RUN = "home_start_run"
        const val COMMUNITY_SEGMENTS = "community_segments"
        const val COMMUNITY_RANKING = "community_ranking"
        const val COMMUNITY_WRITE = "community_write"
        const val ITEMS_EQUIPPED = "items_equipped"
        const val ITEMS_MINT = "items_mint"
        const val ITEMS_COLLECTION = "items_collection"
        const val EVENTS_FEATURED = "events_featured"
        const val PROFILE_AVATAR = "profile_avatar"
        const val PROFILE_ACHIEVEMENTS = "profile_achievements"
    }

    val steps: List<GuideStep> = listOf(
        GuideStep(Targets.HOME_STEPS, "home", R.string.tour1_title, R.string.tour1_body),
        GuideStep(Targets.HOME_ENERGY, "home", R.string.tour2_title, R.string.tour2_body),
        GuideStep(Targets.HOME_START_RUN, "home", R.string.tour3_title, R.string.tour3_body),
        GuideStep(Targets.COMMUNITY_SEGMENTS, "community", R.string.tour4_title, R.string.tour4_body),
        GuideStep(Targets.COMMUNITY_RANKING, "community", R.string.tour5_title, R.string.tour5_body),
        GuideStep(Targets.COMMUNITY_WRITE, "community", R.string.tour6_title, R.string.tour6_body),
        GuideStep(Targets.ITEMS_EQUIPPED, "items", R.string.tour7_title, R.string.tour7_body),
        GuideStep(Targets.ITEMS_MINT, "items", R.string.tour8_title, R.string.tour8_body),
        GuideStep(Targets.ITEMS_COLLECTION, "items", R.string.tour9_title, R.string.tour9_body),
        GuideStep(Targets.EVENTS_FEATURED, "events", R.string.tour10_title, R.string.tour10_body),
        GuideStep(Targets.PROFILE_AVATAR, "profile", R.string.tour11_title, R.string.tour11_body),
        GuideStep(Targets.PROFILE_ACHIEVEMENTS, "profile", R.string.tour12_title, R.string.tour12_body),
    )

    val current: GuideStep? get() = if (active) steps.getOrNull(stepIndex) else null

    fun start() {
        // 이전 실행에서 남은 좌표로 엉뚱한 곳에 구멍이 뚫리지 않게 비운다
        bounds.clear()
        stepIndex = 0
        active = true
    }

    /** 다음 스텝. 마지막이었다면 false를 돌려주고 투어를 끝낸다. */
    fun advance(): Boolean {
        if (stepIndex < steps.lastIndex) {
            stepIndex++
            return true
        }
        active = false
        return false
    }

    fun stop() {
        active = false
    }
}

/**
 * 이 요소를 가이드 투어 스포트라이트 대상으로 등록한다.
 * 화면에서 사라지면 등록도 해제해, 옛 좌표에 구멍이 뚫리는 일을 막는다.
 */
fun Modifier.guideTarget(key: String): Modifier = composed {
    DisposableEffect(key) {
        onDispose { GuideTour.bounds.remove(key) }
    }
    Modifier.onGloballyPositioned { GuideTour.bounds[key] = it.boundsInRoot() }
}

/**
 * 투어 오버레이 — MainScaffold 최상단에 올린다.
 *
 * @param onSwitchTab 스텝의 탭으로 전환
 * @param onFinished 투어 종료(완주·건너뛰기 모두)
 */
@Composable
fun GuideOverlay(
    onSwitchTab: (String) -> Unit,
    onFinished: () -> Unit,
) {
    val step = GuideTour.current ?: return
    val isLast = GuideTour.stepIndex == GuideTour.steps.lastIndex

    // 투어 중 뒤로가기 = 건너뛰기와 동일하게 처리 (탭 스택이 꼬이지 않게)
    BackHandler {
        GuideTour.stop()
        onFinished()
    }

    // 스텝의 탭으로 자동 전환
    LaunchedEffect(step.tabRoute) { onSwitchTab(step.tabRoute) }

    val target: Rect? = GuideTour.bounds[step.key]
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()

    fun finish() {
        GuideTour.stop()
        onFinished()
    }

    Box(Modifier.fillMaxSize()) {
        // 딤 + 대상 구멍 — 탭하면 다음으로
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .pointerInput(step.key) {
                    detectTapGestures {
                        if (!GuideTour.advance()) onFinished()
                    }
                },
        ) {
            drawRect(Color.Black.copy(alpha = 0.84f))
            if (target != null) {
                val pad = 7.dp.toPx()
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(target.left - pad, target.top - pad),
                    size = Size(target.width + pad * 2, target.height + pad * 2),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    blendMode = BlendMode.Clear,
                )
            }
        }

        // 대상 주위 볼트 테두리
        if (target != null) {
            Canvas(Modifier.fillMaxSize()) {
                val pad = 7.dp.toPx()
                drawRoundRect(
                    color = Volt,
                    topLeft = Offset(target.left - pad, target.top - pad),
                    size = Size(target.width + pad * 2, target.height + pad * 2),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        // 설명 창 — 대상이 화면 위쪽이면 아래에, 아래쪽이면 위에 띄운다
        val density = androidx.compose.ui.platform.LocalDensity.current
        val tooltipOffsetDp = if (target != null) {
            with(density) {
                val below = target.bottom.toDp().value + 18f
                val targetCenterDp = target.center.y.toDp().value
                if (targetCenterDp < screenHeightDp * 0.45f) {
                    below
                } else {
                    (target.top.toDp().value - 178f).coerceAtLeast(52f)
                }
            }
        } else {
            screenHeightDp * 0.32f
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = tooltipOffsetDp.dp)
                .padding(horizontal = 26.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Carbon)
                .border(1.dp, Volt.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "${GuideTour.stepIndex + 1} / ${GuideTour.steps.size}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Volt,
            )
            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = Snow,
            )
            Text(
                text = stringResource(step.bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
                lineHeight = 19.sp,
            )
        }

        // 하단 컨트롤
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 18.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostButton(
                text = stringResource(R.string.guide_skip),
                onClick = { finish() },
                accent = Silver,
            )
            VoltButton(
                text = stringResource(if (isLast) R.string.guide_start else R.string.guide_next),
                onClick = { if (!GuideTour.advance()) onFinished() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
