package com.giwa.strideup.ui.screens.events

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.data.repo.EventDef
import com.giwa.strideup.data.repo.Events
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.PillChip
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.guide.GuideTour
import com.giwa.strideup.ui.guide.guideTarget
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun EventsScreen(
    onOpenNotifications: () -> Unit = {},
    viewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory),
) {
    val context = LocalContext.current
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val weekSteps by viewModel.weekSteps.collectAsStateWithLifecycle()
    val claimed by viewModel.claimedIds.collectAsStateWithLifecycle()
    val claimResult by viewModel.claimResult.collectAsStateWithLifecycle()

    val claimedFmt = stringResource(R.string.toast_claimed, "%s")
    val notFinished = stringResource(R.string.toast_not_finished)
    LaunchedEffect(claimResult) {
        when (val r = claimResult) {
            is ClaimResult.Success ->
                Toast.makeText(context, claimedFmt.format("%,.0f".format(r.amount)), Toast.LENGTH_SHORT).show()
            ClaimResult.NotFinished ->
                Toast.makeText(context, notFinished, Toast.LENGTH_SHORT).show()
            null -> {}
        }
        if (claimResult != null) viewModel.consumeClaimResult()
    }

    val inviteSubject = stringResource(R.string.invite_subject)
    val inviteText = stringResource(R.string.invite_text)
    val shareInvite = {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, inviteSubject)
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        context.startActivity(Intent.createChooser(send, inviteSubject))
    }

    var selectedChip by rememberSaveable { mutableIntStateOf(0) }
    val chips = listOf(
        stringResource(R.string.chip_all_events),
        stringResource(R.string.chip_challenges),
        stringResource(R.string.chip_campaigns),
        stringResource(R.string.chip_missions),
    )

    // 데모 카운트다운 목표 시각 (화면 최초 진입 기준)
    val featuredTarget = rememberSaveable {
        System.currentTimeMillis() + ((12L * 24 + 18) * 3600 + 42 * 60 + 6) * 1000
    }
    val questTarget = rememberSaveable {
        System.currentTimeMillis() + ((2L * 24 + 14) * 3600 + 22 * 60) * 1000
    }

    val showChallenges = selectedChip == 0 || selectedChip == 1
    val showCampaigns = selectedChip == 0 || selectedChip == 2
    val showMissions = selectedChip == 0 || selectedChip == 3

    val stepSurgeProgress = (weekSteps.toFloat() / Events.STEP_SURGE.target.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            // 로고 · 누적 리워드 · 알림을 한 줄에 — 제목은 그 아래 전체 폭을 쓴다
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Wordmark(fontSize = 22.sp, modifier = Modifier.weight(1f))
                TotalRewardsCard(balance)
                DarkIconButton(
                    icon = Icons.Filled.Notifications,
                    contentDescription = stringResource(R.string.cd_notifications),
                    onClick = onOpenNotifications,
                    badge = true,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.tab_events),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.events_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chips.size) { index ->
                    PillChip(
                        text = chips[index],
                        selected = selectedChip == index,
                        onClick = { selectedChip = index },
                    )
                }
            }
        }

        if (showCampaigns) {
            item {
                Box(Modifier.guideTarget(GuideTour.Targets.EVENTS_FEATURED)) {
                FeaturedCampaign(
                    targetMillis = featuredTarget,
                    claimed = claimed.contains(Events.NEON_HORIZON.id),
                    onClaim = { viewModel.claim(Events.NEON_HORIZON, 1f) },
                )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (index == 0) 7.dp else 5.dp)
                                .alpha(if (index == 0) 1f else 0.35f)
                                .background(if (index == 0) Volt else Silver, CircleShape),
                        )
                    }
                }
            }
        }

        item { SectionHeader(title = stringResource(R.string.events_active)) }

        if (showChallenges) {
            item {
                EventCard(
                    def = Events.STEP_SURGE,
                    tag = stringResource(R.string.tag_weekly),
                    title = stringResource(R.string.event_step_surge),
                    desc = stringResource(R.string.event_step_surge_desc, "%,d".format(80_000)),
                    progress = stepSurgeProgress,
                    progressText = "%,d / %,d".format(weekSteps, 80_000),
                    rewardAmount = "+250",
                    claimed = claimed.contains(Events.STEP_SURGE.id),
                    onClaim = { viewModel.claim(Events.STEP_SURGE, stepSurgeProgress) },
                )
            }
        }

        if (showMissions) {
            item {
                EventCard(
                    def = Events.REFER,
                    tag = stringResource(R.string.tag_mission),
                    title = stringResource(R.string.event_refer),
                    desc = stringResource(R.string.event_refer_desc),
                    progress = 2f / 3f,
                    progressText = "2 / 3",
                    rewardAmount = "+500",
                    claimed = claimed.contains(Events.REFER.id),
                    onClaim = { viewModel.claim(Events.REFER, 2f / 3f) },
                    secondaryButton = {
                        GhostButton(
                            text = stringResource(R.string.events_invite),
                            onClick = shareInvite,
                        )
                    },
                )
            }
        }

        if (showChallenges) {
            item {
                EventCard(
                    def = Events.NIGHT_QUEST,
                    tag = stringResource(R.string.tag_limited),
                    title = stringResource(R.string.event_night_quest),
                    desc = stringResource(R.string.event_night_quest_desc),
                    progress = 12.4f / 20f,
                    progressText = "12.4 / 20 km",
                    rewardAmount = "+300",
                    claimed = claimed.contains(Events.NIGHT_QUEST.id),
                    onClaim = { viewModel.claim(Events.NIGHT_QUEST, 12.4f / 20f) },
                    countdownTarget = questTarget,
                )
            }
        }
    }
}

/** 매초 갱신되는 카운트다운 문자열 */
@Composable
private fun countdown(targetMillis: Long): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(targetMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val left = ((targetMillis - now) / 1000).coerceAtLeast(0)
    val d = left / 86_400
    val h = (left % 86_400) / 3_600
    val m = (left % 3_600) / 60
    val s = left % 60
    return "%02dd : %02dh : %02dm : %02ds".format(d, h, m, s)
}

@Composable
private fun TotalRewardsCard(balance: Double) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(CarbonHigh, shape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HexEmblem(size = 24.dp)
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = stringResource(R.string.events_total_rewards),
                fontSize = 9.sp,
                color = Silver,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = "%,.2f".format(balance),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Snow,
                    maxLines = 1,
                )
                Text(
                    text = "+$%,.2f".format(balance * 0.01),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Volt,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 추천 캠페인 — Neon Horizon. 시즌 참가 보상(온보딩)은 즉시 수령 가능. */
@Composable
private fun FeaturedCampaign(
    targetMillis: Long,
    claimed: Boolean,
    onClaim: () -> Unit,
) {
    GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 13.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = stringResource(R.string.events_featured),
                    style = MaterialTheme.typography.labelSmall,
                    color = Volt,
                )
                Text(
                    text = "Neon Horizon",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.events_season),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
                Text(
                    text = stringResource(R.string.events_campaign_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
            HorizonArt(
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp),
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Night.copy(alpha = 0.55f))
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = Volt, modifier = Modifier.size(15.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = stringResource(R.string.events_ends_in),
                    fontSize = 10.sp,
                    color = Silver,
                )
                Text(
                    text = countdown(targetMillis),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Snow,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RewardPill(
                modifier = Modifier.weight(1f),
                top = "+15,000",
                bottom = "StepUp Token",
            )
            if (claimed) {
                GhostButton(
                    text = stringResource(R.string.events_claimed),
                    onClick = {},
                    enabled = false,
                )
            } else {
                VoltButton(
                    text = stringResource(R.string.events_claim),
                    onClick = onClaim,
                )
            }
        }
    }
}

@Composable
private fun RewardPill(modifier: Modifier = Modifier, top: String, bottom: String) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonHigh)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HexEmblem(size = 24.dp, glow = false)
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(top, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Snow)
            Text(bottom, fontSize = 10.sp, color = Silver)
        }
    }
}

/** 캠페인 아트 — 지평선 위로 떠오르는 글로우 헥사곤 */
@Composable
private fun HorizonArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.42f
        val r = size.minDimension * 0.30f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Volt.copy(alpha = 0.40f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 2.2f,
            ),
            radius = r * 2.2f,
            center = Offset(cx, cy),
        )

        fun hex(radius: Float): Path = Path().apply {
            for (i in 0 until 6) {
                val a = (-90f + i * 60f) * (PI / 180.0)
                val x = cx + radius * cos(a).toFloat()
                val y = cy + radius * sin(a).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(hex(r), color = Volt.copy(alpha = 0.14f))
        drawPath(hex(r), color = Volt, style = Stroke(width = 2.5.dp.toPx()))
        drawPath(hex(r * 0.55f), color = Volt, style = Stroke(width = 1.6.dp.toPx()))
        drawCircle(Volt, radius = r * 0.14f, center = Offset(cx, cy))

        val horizonY = size.height * 0.86f
        drawArc(
            color = Volt.copy(alpha = 0.35f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(-size.width * 0.25f, horizonY - size.height * 0.06f),
            size = androidx.compose.ui.geometry.Size(size.width * 1.5f, size.height * 0.9f),
            style = Stroke(width = 2.dp.toPx()),
        )
        listOf(
            0.15f to 0.12f, 0.85f to 0.20f, 0.72f to 0.06f, 0.30f to 0.80f, 0.90f to 0.70f,
        ).forEach { (fx, fy) ->
            drawCircle(
                color = Snow.copy(alpha = 0.5f),
                radius = 1.4.dp.toPx(),
                center = Offset(size.width * fx, size.height * fy),
            )
        }
    }
}

/** 진행형 이벤트 카드 — 목표를 채우면 Claim이 활성화되고 실제 SUP가 적립된다 */
@Composable
private fun EventCard(
    def: EventDef,
    tag: String,
    title: String,
    desc: String,
    progress: Float,
    progressText: String,
    rewardAmount: String,
    claimed: Boolean,
    onClaim: () -> Unit,
    countdownTarget: Long? = null,
    secondaryButton: (@Composable () -> Unit)? = null,
) {
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            HexEmblem(size = 52.dp, glow = false)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(tag, style = MaterialTheme.typography.labelSmall, color = Volt)
                Text(title, style = MaterialTheme.typography.titleMedium, color = Snow)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Silver)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.events_reward),
                    fontSize = 10.sp,
                    color = Slate,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HexEmblem(size = 16.dp, glow = false)
                    Text(
                        text = rewardAmount,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Volt,
                    )
                }
                Text("StepUp Token", fontSize = 9.sp, color = Slate)
            }
        }

        if (countdownTarget != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = Volt, modifier = Modifier.size(13.dp))
                Text(
                    text = stringResource(R.string.events_ends_in) + " " + countdown(countdownTarget),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Volt,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                BarMeter(fraction = progress, height = 7.dp)
                Text(progressText, fontSize = 11.sp, color = Silver)
            }
            secondaryButton?.invoke()
            when {
                claimed -> GhostButton(
                    text = stringResource(R.string.events_claimed),
                    onClick = {},
                    enabled = false,
                )
                progress >= 1f -> VoltButton(
                    text = stringResource(R.string.events_claim),
                    onClick = onClaim,
                )
                else -> GhostButton(
                    text = stringResource(R.string.events_claim),
                    onClick = onClaim,
                    enabled = false,
                )
            }
        }
    }
}
