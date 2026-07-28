package com.giwa.strideup.ui.screens.community

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.data.repo.CrewRepository
import com.giwa.strideup.data.repo.PartyMember
import com.giwa.strideup.data.repo.PartyPhase
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.service.WalkSessionService
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.components.BarMeter
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexBadge
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/**
 * 파티런 로비 — 로비를 연 사람이 파티장이다.
 *
 * 파티장은 크루원을 초대·강퇴할 수 있고, 전원이 준비를 마치면
 * "시작"을 눌러 카운트다운 후 다 같이 측정에 들어간다.
 * 러닝 중 파티장에게서 일정 거리 이상 떨어진 크루원은 자동으로 빠진다.
 */
@Composable
fun PartyLobbyScreen(
    crewId: String,
    onBack: () -> Unit,
    onRunStarted: () -> Unit,
    viewModel: PartyLobbyViewModel = viewModel(factory = PartyLobbyViewModel.Factory),
) {
    val context = LocalContext.current
    val party by viewModel.party.collectAsStateWithLifecycle()
    var showInvite by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (StepPermissions.hasActivityRecognition(context)) {
            WalkSessionService.start(context, party.partySize)
        }
    }

    LaunchedEffect(crewId) { viewModel.openLobby(crewId) }

    // 전원 준비 → 카운트다운이 끝나면 실제 세션을 시작한다.
    LaunchedEffect(party.phase) {
        if (party.phase == PartyPhase.RUNNING) {
            val missing = StepPermissions.missing(context)
            if (missing.isEmpty()) {
                WalkSessionService.start(context, party.partySize)
                onRunStarted()
            } else {
                permissionLauncher.launch(missing)
            }
        }
    }

    val boostPercent = RewardEconomy.partyBonusPercent(party.partySize)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DarkIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        onClick = {
                            viewModel.leaveLobby()
                            onBack()
                        },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.crew_lobby),
                            style = MaterialTheme.typography.bodySmall,
                            color = Volt,
                        )
                        Text(
                            text = party.crewName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = Snow,
                        )
                    }
                    HexBadge(text = "${party.partySize}", size = 36.dp)
                }
            }

            // 준비 현황
            item {
                GlowCard(accent = party.allReady, contentPadding = PaddingValues(18.dp), spacing = 13.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (party.allReady) {
                                stringResource(R.string.crew_all_ready)
                            } else {
                                stringResource(R.string.crew_waiting)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (party.allReady) Volt else Snow,
                        )
                        Text(
                            text = stringResource(
                                R.string.crew_ready_count,
                                party.readyCount,
                                party.partySize,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (party.allReady) Volt else Silver,
                        )
                    }
                    BarMeter(
                        fraction = if (party.partySize > 0) {
                            party.readyCount.toFloat() / party.partySize
                        } else {
                            0f
                        },
                        height = 7.dp,
                    )
                }
            }

            // 파티 부스트 안내
            item {
                GlowCard(contentPadding = PaddingValues(16.dp), spacing = 9.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Volt,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.crew_boost, boostPercent),
                            style = MaterialTheme.typography.titleMedium,
                            color = Volt,
                            modifier = Modifier.weight(1f),
                        )
                        HexEmblem(size = 28.dp, glow = false)
                    }
                    Text(
                        text = stringResource(R.string.crew_boost_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate,
                    )
                }
            }

            // 멤버 목록 (파티장은 강퇴 가능)
            items(party.members.size) { index ->
                val m = party.members[index]
                MemberRow(
                    member = m,
                    canKick = party.phase == PartyPhase.LOBBY && !m.isMe,
                    onKick = { viewModel.kick(m.id) },
                )
            }

            // 초대 (로비 단계에서만)
            if (party.phase == PartyPhase.LOBBY) {
                item {
                    GhostButton(
                        text = stringResource(R.string.party_invite_button),
                        onClick = { showInvite = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // 컨트롤
            item {
                Spacer(Modifier.height(4.dp))
                when (party.phase) {
                    PartyPhase.RUNNING -> {
                        GlowCard(accent = true, contentPadding = PaddingValues(18.dp), spacing = 10.dp) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = null,
                                    tint = Volt,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.crew_running_together,
                                        party.partySize,
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Snow,
                                )
                            }
                        }
                    }
                    PartyPhase.FINISHED -> {
                        GlowCard(accent = true, contentPadding = PaddingValues(20.dp), spacing = 12.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.crew_result_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Snow,
                                )
                                Text(
                                    text = "+%.2f SUP".format(party.resultPoints),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-1).sp,
                                    color = Volt,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.crew_result_body,
                                        "%,d".format(party.resultSteps),
                                        boostPercent,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Silver,
                                )
                            }
                            GhostButton(
                                text = stringResource(R.string.common_ok),
                                onClick = {
                                    viewModel.dismissResult()
                                    onBack()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            if (party.allReady) {
                                VoltButton(
                                    text = stringResource(R.string.party_start),
                                    onClick = { viewModel.startParty() },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (party.myReady) {
                                GhostButton(
                                    text = stringResource(R.string.crew_ready_cancel),
                                    onClick = { viewModel.setReady(false) },
                                    accent = Silver,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                VoltButton(
                                    text = stringResource(R.string.crew_ready),
                                    onClick = { viewModel.setReady(true) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.party_gps_rule,
                                    CrewRepository.MAX_PARTY_DISTANCE_M,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate,
                            )
                        }
                    }
                }
            }
        }

        // 카운트다운 오버레이
        if (party.phase == PartyPhase.COUNTDOWN) {
            CountdownOverlay(party.countdown)
        }
    }

    // 초대 다이얼로그
    if (showInvite) {
        InviteDialog(
            candidates = viewModel.inviteCandidates(),
            onInvite = { name ->
                viewModel.invite(name)
                showInvite = false
            },
            onDismiss = { showInvite = false },
        )
    }
}

@Composable
private fun InviteDialog(
    candidates: List<String>,
    onInvite: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_close),
                    color = Volt,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.party_invite_title),
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (candidates.isEmpty()) {
                    Text(
                        text = stringResource(R.string.party_invite_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                }
                candidates.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CarbonHigh)
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            color = Snow,
                        )
                        GhostButton(
                            text = stringResource(R.string.party_invite_action),
                            onClick = { onInvite(name) },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun MemberRow(
    member: PartyMember,
    canKick: Boolean,
    onKick: () -> Unit,
) {
    val name = if (member.isMe) stringResource(R.string.crew_you) else member.name
    GlowCard(
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        shape = RoundedCornerShape(18.dp),
        accent = member.isMe,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CarbonHigh)
                    .border(
                        width = 1.5.dp,
                        color = if (member.ready) Volt else Edge,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = if (member.ready) Volt else Slate,
                    modifier = Modifier.size(17.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                    if (member.isMe) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Volt.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.party_host_badge),
                                color = Volt,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
                if (!member.isMe) {
                    Text(
                        text = stringResource(R.string.level_chip, member.level) +
                            "  ·  ${member.uid}  ·  ${member.distanceM}m",
                        fontSize = 11.sp,
                        color = Slate,
                    )
                }
            }
            if (member.ready) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Volt.copy(alpha = 0.13f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Volt,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = stringResource(R.string.crew_state_ready),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.crew_state_waiting),
                    fontSize = 11.sp,
                    color = Slate,
                )
            }
            if (canKick) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(CarbonHigh)
                        .quietClickable(onKick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.party_kick),
                        tint = Slate,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownOverlay(count: Int) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(420, easing = EaseOutBack),
        label = "countdownScale",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.crew_all_ready),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Volt,
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                HexEmblem(size = 180.dp)
                Text(
                    text = if (count > 0) "$count" else stringResource(R.string.crew_go),
                    modifier = Modifier.scale(scale),
                    fontSize = if (count > 0) 92.sp else 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-3).sp,
                    color = Snow,
                )
            }
        }
    }
}
