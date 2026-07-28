package com.giwa.strideup.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.data.local.NotificationEntity
import com.giwa.strideup.data.local.NotificationType
import com.giwa.strideup.data.repo.CrewRepository
import com.giwa.strideup.data.repo.NotificationRepository
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 알림함 — 본문은 저장된 type + 인자를 표시 시점에 현지화한다. */
class NotificationsViewModel(
    private val repo: NotificationRepository,
    private val crewRepository: CrewRepository,
) : ViewModel() {

    val items: StateFlow<List<NotificationEntity>> = repo.notifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markAllRead() {
        viewModelScope.launch { repo.markAllRead() }
    }

    /** "모두 읽음" — 알림함 전체 비우기 */
    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }

    fun acceptCrewInvite(entity: NotificationEntity) {
        viewModelScope.launch { repo.acceptCrewInvite(entity, crewRepository) }
    }

    fun decline(entity: NotificationEntity) {
        viewModelScope.launch { repo.decline(entity) }
    }

    fun acceptPartyInvite(entity: NotificationEntity, onOpenLobby: (String) -> Unit) {
        viewModelScope.launch {
            repo.acceptPartyInvite(entity)
            if (entity.argExtra.isNotBlank()) onOpenLobby(entity.argExtra)
        }
    }

    fun claimEventReward(entity: NotificationEntity) {
        viewModelScope.launch { repo.claimEventReward(entity) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                NotificationsViewModel(
                    ServiceLocator.notificationRepository,
                    ServiceLocator.crewRepository,
                )
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onOpenLobby: (String) -> Unit = {},
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory),
) {
    val notifications by viewModel.items.collectAsStateWithLifecycle()
    val now = remember { System.currentTimeMillis() }

    // 화면을 열면 배지를 비운다.
    LaunchedEffect(Unit) { viewModel.markAllRead() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
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
                    onClick = onBack,
                )
                Text(
                    text = stringResource(R.string.notif_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Snow,
                )
                Spacer(Modifier.weight(1f))
                if (notifications.isNotEmpty()) {
                    GhostButton(
                        text = stringResource(R.string.notif_mark_read),
                        onClick = viewModel::clearAll,
                    )
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                GlowCard(contentPadding = PaddingValues(26.dp), spacing = 6.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        IconSquare(icon = Icons.Filled.Notifications, size = 42.dp)
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = stringResource(R.string.notif_empty_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Snow,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.notif_empty_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = Silver,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            items(notifications, key = { it.id }) { entity ->
                NotificationRow(
                    entity = entity,
                    now = now,
                    onAcceptCrew = { viewModel.acceptCrewInvite(entity) },
                    onDecline = { viewModel.decline(entity) },
                    onAcceptParty = { viewModel.acceptPartyInvite(entity, onOpenLobby) },
                    onClaim = { viewModel.claimEventReward(entity) },
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(
    entity: NotificationEntity,
    now: Long,
    onAcceptCrew: () -> Unit,
    onDecline: () -> Unit,
    onAcceptParty: () -> Unit,
    onClaim: () -> Unit,
) {
    val actionable = entity.type in listOf(
        NotificationType.CREW_INVITE,
        NotificationType.PARTY_INVITE,
        NotificationType.EVENT_REWARD,
    )
    GlowCard(
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        shape = RoundedCornerShape(18.dp),
        accent = actionable && !entity.actioned,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            IconSquare(icon = iconFor(entity.type), size = 38.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = messageFor(entity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Snow,
                )
                Text(
                    text = relativeTime(timestamp = entity.timestamp, now = now),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
            if (!entity.read) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Volt, CircleShape),
                )
            }
        }

        if (actionable) {
            if (entity.actioned) {
                Text(
                    text = stringResource(R.string.notif_done),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    when (entity.type) {
                        NotificationType.CREW_INVITE -> {
                            VoltButton(
                                text = stringResource(R.string.notif_accept),
                                onClick = onAcceptCrew,
                                modifier = Modifier.weight(1f),
                            )
                            GhostButton(
                                text = stringResource(R.string.notif_decline),
                                onClick = onDecline,
                                accent = Silver,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        NotificationType.PARTY_INVITE -> {
                            VoltButton(
                                text = stringResource(R.string.notif_accept),
                                onClick = onAcceptParty,
                                modifier = Modifier.weight(1f),
                            )
                            GhostButton(
                                text = stringResource(R.string.notif_decline),
                                onClick = onDecline,
                                accent = Silver,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        else -> {
                            VoltButton(
                                text = stringResource(R.string.notif_claim),
                                onClick = onClaim,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun iconFor(type: String): ImageVector = when (type) {
    NotificationType.REWARD_EARNED -> Icons.AutoMirrored.Filled.DirectionsWalk
    NotificationType.GOAL_REACHED -> Icons.Filled.EmojiEvents
    NotificationType.SNEAKER_MINTED -> Icons.Filled.AutoAwesome
    NotificationType.SNEAKER_UPGRADED -> Icons.Filled.TrendingUp
    NotificationType.BOOST_ACTIVATED -> Icons.Filled.Whatshot
    NotificationType.CREW_JOINED -> Icons.Filled.Shield
    NotificationType.PARTY_FINISHED -> Icons.Filled.MilitaryTech
    NotificationType.EVENT_CLAIMED -> Icons.Filled.CheckCircle
    NotificationType.PARTY_MEMBER_LEFT -> Icons.Filled.PersonOff
    NotificationType.CREW_INVITE -> Icons.Filled.GroupAdd
    NotificationType.PARTY_INVITE -> Icons.Filled.Bolt
    NotificationType.EVENT_REWARD -> Icons.Filled.Redeem
    else -> Icons.Filled.Notifications
}

/** 저장된 인자를 표시 시점 로케일로 조립한다. */
@Composable
private fun messageFor(entity: NotificationEntity): String {
    val amount = "%,.2f".format(entity.argAmount)
    return when (entity.type) {
        NotificationType.REWARD_EARNED ->
            stringResource(R.string.notif_reward_earned, entity.argText, amount)

        NotificationType.GOAL_REACHED ->
            stringResource(R.string.notif_goal_reached, entity.argText, amount)

        NotificationType.SNEAKER_MINTED ->
            stringResource(R.string.notif_sneaker_minted, entity.argText)

        NotificationType.SNEAKER_UPGRADED ->
            stringResource(R.string.notif_sneaker_upgraded, entity.argText)

        NotificationType.BOOST_ACTIVATED ->
            stringResource(R.string.notif_boost_activated)

        NotificationType.CREW_JOINED ->
            stringResource(R.string.notif_crew_joined, entity.argText)

        NotificationType.PARTY_FINISHED ->
            stringResource(R.string.notif_party_finished, entity.argText, amount)

        NotificationType.EVENT_CLAIMED ->
            stringResource(R.string.notif_event_claimed, entity.argText, amount)

        NotificationType.PARTY_MEMBER_LEFT ->
            stringResource(R.string.notif_party_member_left, entity.argText)

        NotificationType.CREW_INVITE ->
            stringResource(R.string.notif_crew_invite, entity.argText)

        NotificationType.PARTY_INVITE ->
            stringResource(R.string.notif_party_invite, entity.argText)

        NotificationType.EVENT_REWARD ->
            stringResource(R.string.notif_event_reward, entity.argText, amount)

        else -> stringResource(R.string.notif_title)
    }
}

@Composable
private fun relativeTime(timestamp: Long, now: Long): String {
    val elapsed = (now - timestamp).coerceAtLeast(0L)
    val minutes = elapsed / 60_000L
    val hours = elapsed / 3_600_000L
    val days = elapsed / 86_400_000L
    return when {
        minutes < 1L -> stringResource(R.string.time_just_now)
        hours < 1L -> stringResource(R.string.time_minutes_ago, minutes.toInt())
        days < 1L -> stringResource(R.string.time_hours_ago, hours.toInt())
        else -> stringResource(R.string.time_days_ago, days.toInt())
    }
}
