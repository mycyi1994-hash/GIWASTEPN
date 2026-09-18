package com.stepup.android.ui.guide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.North
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 스포트라이트 가이드 투어.
 *
 * 실제 화면 위에 어두운 오버레이를 깔고, 설명하려는 요소만 밝게 뚫어
 * 그 옆에 설명 창을 띄운다. 스텝이 다른 탭으로 넘어가면 탭도 자동 전환된다.
 */

data class GuideStep(
    /**
     * [Modifier.guideTarget]로 등록한 대상 키.
     * 비어 있으면 가리킬 곳 없이 설명만 보여준다(여정 개요).
     */
    val key: String,
    /** 이 스텝이 속한 하단 탭 라우트 */
    val tabRoute: String,
    val titleRes: Int,
    val bodyRes: Int,
    /** 러닝 → 출금 전체 흐름 카드를 함께 보여준다 */
    val journey: Boolean = false,
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
        const val HOME_TOKEN = "home_token"
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

        /**
         * 하단 탭 버튼.
         *
         * 기능을 설명하기 전에 **그 기능이 어느 버튼 안에 있는지**부터
         * 보여주기 위한 것이다. 설명만 읽고 화면을 덮으면, 다음에 다시
         * 찾아가지 못해서 그 기능은 없는 것이나 같아진다.
         */
        fun tab(route: String) = "tab_$route"
    }

    /*
     * 순서에 두 가지 규칙이 있다.
     *
     * 하나, **돈이 흐르는 길을 맨 앞에** 둔다. 이 앱을 처음 여는 사람이
     * 가장 먼저 묻는 것은 "그래서 뛰면 뭐가 남는데"이다. 걸음 수 카드 설명을
     * 세 장 넘긴 뒤에 답하면 늦다.
     *
     * 둘, **탭을 소개한 다음에 그 안의 기능을 설명한다.** 기능만 설명하고
     * 넘어가면 "좋은 기능이구나"까지는 가도 "어디서 다시 찾지"에서 막힌다.
     */
    val steps: List<GuideStep> = listOf(
        // ── 러닝 → 출금, 전체 흐름 ──
        GuideStep("", "home", R.string.tour_journey_title, R.string.tour_journey_body, journey = true),

        // ── 홈: 그 흐름이 실제로 어디에 있는지 ──
        GuideStep(Targets.tab("home"), "home", R.string.tour_tab_home_title, R.string.tour_tab_home_body),
        GuideStep(Targets.HOME_START_RUN, "home", R.string.tour3_title, R.string.tour3_body),
        GuideStep(Targets.HOME_TOKEN, "home", R.string.tour_token_title, R.string.tour_token_body),
        GuideStep(Targets.HOME_STEPS, "home", R.string.tour1_title, R.string.tour1_body),
        GuideStep(Targets.HOME_ENERGY, "home", R.string.tour2_title, R.string.tour2_body),

        // ── 커뮤니티 ──
        GuideStep(Targets.tab("community"), "community", R.string.tour_tab_community_title, R.string.tour_tab_community_body),
        GuideStep(Targets.COMMUNITY_SEGMENTS, "community", R.string.tour4_title, R.string.tour4_body),
        GuideStep(Targets.COMMUNITY_RANKING, "community", R.string.tour5_title, R.string.tour5_body),
        GuideStep(Targets.COMMUNITY_WRITE, "community", R.string.tour6_title, R.string.tour6_body),

        // ── 아이템 ──
        GuideStep(Targets.tab("items"), "items", R.string.tour_tab_items_title, R.string.tour_tab_items_body),
        GuideStep(Targets.ITEMS_EQUIPPED, "items", R.string.tour7_title, R.string.tour7_body),
        GuideStep(Targets.ITEMS_MINT, "items", R.string.tour8_title, R.string.tour8_body),
        GuideStep(Targets.ITEMS_COLLECTION, "items", R.string.tour9_title, R.string.tour9_body),

        // ── 이벤트 ──
        GuideStep(Targets.tab("events"), "events", R.string.tour_tab_events_title, R.string.tour_tab_events_body),
        GuideStep(Targets.EVENTS_FEATURED, "events", R.string.tour10_title, R.string.tour10_body),

        // ── 프로필 ──
        GuideStep(Targets.tab("profile"), "profile", R.string.tour_tab_profile_title, R.string.tour_tab_profile_body),
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

    /** 이전 스텝. 첫 스텝이면 아무 일도 하지 않고 false. */
    fun back(): Boolean {
        if (stepIndex == 0) return false
        stepIndex--
        return true
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

    // 투어 중 뒤로가기는 이전 스텝으로. 첫 스텝에서만 투어를 닫는다.
    //
    // 예전에는 어느 스텝에서든 뒤로가기가 투어를 통째로 끝냈다. 한 장을
    // 놓쳐서 돌아가려던 사람이 투어를 잃는 것은 손해가 너무 크다.
    BackHandler {
        if (!GuideTour.back()) {
            GuideTour.stop()
            onFinished()
        }
    }

    // 스텝의 탭으로 자동 전환
    LaunchedEffect(step.tabRoute) { onSwitchTab(step.tabRoute) }

    val target: Rect? = GuideTour.bounds[step.key]
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()

    // 테두리가 천천히 숨 쉰다. 정지한 사각형은 배경으로 읽히지만, 움직이는
    // 것은 눈이 먼저 찾는다 — "여기를 보세요"를 글로 쓰지 않고 전달하는 방법이다.
    val pulse by rememberInfiniteTransition(label = "guidePulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "guidePulseValue",
    )

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

        // 대상 주위 볼트 테두리 + 퍼져 나가는 고리
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
                // 물결처럼 한 겹 더 퍼졌다 사라진다
                val spread = pad + 14.dp.toPx() * pulse
                drawRoundRect(
                    color = Volt.copy(alpha = 0.55f * (1f - pulse)),
                    topLeft = Offset(target.left - spread, target.top - spread),
                    size = Size(target.width + spread * 2, target.height + spread * 2),
                    cornerRadius = CornerRadius(22.dp.toPx() + 14.dp.toPx() * pulse),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        val density = androidx.compose.ui.platform.LocalDensity.current

        // 가리키는 것이 화면 맨 아래(하단 탭)면, 아래에 붙은 조작 버튼이
        // 하필 그 위를 덮는다. 스포트라이트를 켜 놓고 그 자리를 자기 버튼으로
        // 가리는 셈이라, 그만큼 버튼을 위로 올린다.
        val controlsLiftDp = if (target != null) {
            with(density) {
                val topDp = target.top.toDp().value
                if (topDp > screenHeightDp - 130f) screenHeightDp - topDp + 12f else 0f
            }
        } else {
            0f
        }

        // 설명 창 — 대상이 화면 위쪽이면 아래에, 아래쪽이면 위에 띄운다
        val tooltipOffsetDp = if (target != null) {
            with(density) {
                val below = target.bottom.toDp().value + 18f
                val targetCenterDp = target.center.y.toDp().value
                if (targetCenterDp < screenHeightDp * 0.45f) {
                    below
                } else {
                    // 버튼을 올린 만큼 설명 창도 같이 올라가야 서로 겹치지 않는다.
                    (target.top.toDp().value - 178f - controlsLiftDp).coerceAtLeast(52f)
                }
            }
        } else if (step.journey) {
            // 다섯 줄짜리 카드라 아래로 길다. 조금 위에서 시작해야 다 보인다.
            screenHeightDp * 0.12f
        } else {
            screenHeightDp * 0.32f
        }

        // 대상이 설명 창보다 위에 있으면 창 위쪽에, 아래에 있으면 창 아래쪽에
        // 삼각형을 붙인다. 화면에 구멍이 둘 이상 뚫린 것처럼 보일 때
        // "이 설명은 저기 것"을 선 하나로 이어 주는 역할이다.
        val pointsUp = target != null && target.center.y.let { center ->
            with(density) { center.toDp().value } < tooltipOffsetDp
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = tooltipOffsetDp.dp)
                .padding(horizontal = 26.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (target != null && pointsUp) Pointer(up = true, pulse = pulse)

            Column(
                modifier = Modifier
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
                if (step.journey) JourneyFlow()
            }

            if (target != null && !pointsUp) Pointer(up = false, pulse = pulse)
        }

        // 하단 컨트롤
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 18.dp)
                .padding(bottom = controlsLiftDp.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isLast) {
                GhostButton(
                    text = stringResource(R.string.guide_skip),
                    onClick = { finish() },
                    accent = Silver,
                )
            }
            // 첫 스텝에는 돌아갈 곳이 없다. 눌리지 않는 버튼을 남겨 두는 것은
            // 자리를 지켜 다음 스텝에서 버튼들이 옆으로 밀리지 않게 하기 위해서다.
            GhostButton(
                text = stringResource(R.string.guide_prev),
                onClick = { GuideTour.back() },
                enabled = GuideTour.stepIndex > 0,
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

/**
 * 설명 창에서 대상 쪽으로 뻗는 삼각형.
 *
 * 화면에 밝은 구멍과 설명 창이 따로 떠 있으면, 처음 보는 사람은 둘이
 * 한 쌍인지 알기까지 한 박자를 쓴다. 삼각형이 그 한 박자를 없앤다.
 */
@Composable
private fun Pointer(up: Boolean, pulse: Float) {
    // 숨 쉬는 테두리와 같은 박자로 대상 쪽으로 살짝 다가갔다 돌아온다.
    val nudge = (if (up) -1f else 1f) * 3f * kotlin.math.sin(pulse * Math.PI).toFloat()
    Canvas(
        modifier = Modifier
            .padding(vertical = 1.dp)
            .offset(y = nudge.dp)
            .size(width = 18.dp, height = 9.dp),
    ) {
        val path = androidx.compose.ui.graphics.Path().apply {
            if (up) {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
            }
            close()
        }
        drawPath(path, color = Volt)
    }
}

/**
 * 러닝 → 출금까지 한 장.
 *
 * 투어의 첫 장에 둔다. 이 앱을 처음 여는 사람이 가장 먼저 묻는 것은
 * "뛰면 뭐가 남는가"이고, 그 답을 다섯 줄로 먼저 보여 준 다음에야
 * 화면 구경이 의미를 갖는다.
 *
 * 마지막 줄(출금)에는 "준비 중"을 붙인다. 지금 되는 것처럼 그려 놓으면
 * 첫 화면에서 한 거짓말이 되고, 그건 1,000 SUP 를 모은 사람이 발견한다.
 */
@Composable
private fun JourneyFlow() {
    val rows = listOf(
        Triple(Icons.AutoMirrored.Filled.DirectionsRun, R.string.journey_run_label, R.string.journey_run_desc),
        Triple(Icons.Filled.CheckCircle, R.string.journey_finish_label, R.string.journey_finish_desc),
        Triple(Icons.Filled.Bolt, R.string.journey_earn_label, R.string.journey_earn_desc),
        Triple(Icons.Filled.AccountBalanceWallet, R.string.journey_wallet_label, R.string.journey_wallet_desc),
        Triple(Icons.Filled.North, R.string.journey_withdraw_label, R.string.journey_withdraw_desc),
    )

    Column(Modifier.padding(top = 6.dp)) {
        rows.forEachIndexed { index, (icon, label, desc) ->
            val soon = index == rows.lastIndex
            Row(verticalAlignment = Alignment.Top) {
                // 번호 원 + 아래로 잇는 선. 선이 있어야 다섯 줄이 목록이
                // 아니라 "순서"로 읽힌다.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (soon) Carbon else Volt.copy(alpha = 0.16f))
                            .border(
                                1.dp,
                                if (soon) Slate.copy(alpha = 0.5f) else Volt.copy(alpha = 0.55f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (soon) Slate else Volt,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    if (!soon) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(19.dp)
                                .background(Volt.copy(alpha = 0.3f)),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(start = 11.dp, bottom = if (soon) 0.dp else 6.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (soon) Silver else Snow,
                        )
                        if (soon) {
                            Text(
                                text = stringResource(R.string.journey_soon),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Slate.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(desc),
                        fontSize = 11.sp,
                        color = Silver,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}
