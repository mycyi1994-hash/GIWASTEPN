package com.giwa.strideup.ui.screens.community

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.core.ExternalIntents
import com.giwa.strideup.domain.Post
import com.giwa.strideup.ui.components.AvatarStack
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.RouteMap
import com.giwa.strideup.ui.components.VerticalHairline
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.Alert
import com.giwa.strideup.ui.theme.Carbon
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import com.giwa.strideup.ui.theme.VoltSoft
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 백엔드가 없으므로 참가자 이름은 랭킹 라이벌처럼 고정 목록으로 만든다. */
private val demoRunners = listOf(
    "Maya C.", "Jun H.", "Elena R.", "Marco P.", "Sora K.",
    "Diego M.", "Aiko T.", "Tomas L.", "Priya N.", "Owen D.",
)

/**
 * 번개러닝 상세 — 카드 어디를 눌러도 여기로 들어온다.
 *
 * 히어로(루트맵 배경 + 상태 배지 + 주최자) → 집결 정보 그리드 →
 * 참가자 → 파티 채팅(댓글) → 하단 참가 CTA 순서.
 */
@Composable
fun FlashRunDetailScreen(
    postId: Long,
    onBack: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    val posts by viewModel.allPosts.collectAsStateWithLifecycle()
    val post = posts.firstOrNull { it.id == postId }
    var showMembers by rememberSaveable { mutableStateOf(false) }

    // 파티 채팅 카드가 최신 댓글을 미리 보여준다
    val threadsFlow = remember(postId) { viewModel.commentThreads(postId) }
    val threads by threadsFlow.collectAsStateWithLifecycle(emptyList())

    // "채팅 입장"이 여는 댓글 창은 이 화면 위에 그대로 뜬다
    CommentSheetHost(viewModel)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
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
                Wordmark(fontSize = 20.sp, modifier = Modifier.weight(1f))
                DarkIconButton(
                    icon = Icons.Filled.Notifications,
                    contentDescription = stringResource(R.string.cd_notifications),
                    onClick = {},
                )
            }
        }

        if (post == null) {
            item {
                GlowCard(contentPadding = PaddingValues(26.dp)) {
                    Text(
                        text = stringResource(R.string.community_board_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                }
            }
            return@LazyColumn
        }

        item { FlashHeroCard(post) }

        item { FlashInfoGrid(post) }

        item {
            FlashParticipantsCard(
                post = post,
                onViewMembers = { showMembers = true },
            )
        }

        item {
            FlashChatCard(
                post = post,
                threads = threads,
                onEnterChat = { viewModel.openComments(post.id) },
            )
        }

        item {
            FlashCtaRow(
                post = post,
                onLike = { viewModel.toggleLike(post.id) },
                onToggleJoin = { viewModel.toggleJoinFlash(post.id) },
            )
        }
    }

    if (showMembers && post != null) {
        FlashMembersDialog(post = post, onDismiss = { showMembers = false })
    }
}

// ─────────────────────────────────────────────────────────────
// 히어로
// ─────────────────────────────────────────────────────────────

@Composable
private fun FlashHeroCard(post: Post) {
    GlowCard(accent = true, contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
        Box(Modifier.fillMaxWidth()) {
            // 배경 아트 — 아이소메트릭 루트맵을 반투명으로 깐다
            RouteMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .align(Alignment.TopEnd)
                    .alpha(0.55f),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            ) {
                FlashStatusPill(post)
                Spacer(Modifier.height(78.dp))
                Text(
                    text = post.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 32.sp,
                    color = Snow,
                )
                if (post.body.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = post.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VoltSoft.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(CarbonHigh, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = post.author.take(1).uppercase(),
                            color = Volt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        text = stringResource(R.string.flash_host),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = Slate,
                    )
                    Text(
                        text = post.author,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Snow,
                    )
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = null,
                        tint = Volt,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashStatusPill(post: Post) {
    val (label, tint) = when {
        post.isClosed -> stringResource(R.string.flash_closed_badge) to Slate
        post.isFull -> stringResource(R.string.flash_full_badge) to Alert
        else -> stringResource(R.string.flash_recruiting) to Volt
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            color = tint,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 집결 정보 그리드
// ─────────────────────────────────────────────────────────────

@Composable
private fun FlashInfoGrid(post: Post) {
    val context = LocalContext.current
    val hasPlace = post.place.isNotBlank()

    // 페이스·거리는 백엔드가 없어 글 id로 고정 시드를 만든다 (매번 흔들리면 신뢰를 잃는다)
    val demoIndex = (post.id % 3).toInt()
    val paceValue = when (demoIndex) {
        0 -> "6'30\"~7'00\""
        1 -> "5'30\"~6'00\""
        else -> "4'30\"~5'00\""
    }
    val paceChip = stringResource(
        when (demoIndex) {
            0 -> R.string.pace_easy
            1 -> R.string.pace_normal
            else -> R.string.pace_fast
        }
    )
    val estDistance = "%.2f".format(listOf(5.0, 8.0, 10.0)[demoIndex]) + " km"
    val meetTime = remember(post.meetAt) {
        Instant.ofEpochMilli(post.meetAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoCell(
                label = stringResource(R.string.flash_place),
                value = post.place.ifBlank { stringResource(R.string.post_place_tbd) },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LocationOn,
                onClick = if (hasPlace) {
                    { ExternalIntents.openPlaceInMaps(context, post.place) }
                } else {
                    null
                },
            )
            VerticalHairline(height = 44.dp)
            InfoCell(
                label = stringResource(R.string.flash_time),
                value = meetTime,
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Schedule,
                sub = startsInLabel(post.meetAt),
            )
        }
        HairlineDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoCell(
                label = stringResource(R.string.flash_pace),
                value = paceValue,
                modifier = Modifier.weight(1f),
                chip = paceChip,
            )
            VerticalHairline(height = 44.dp)
            InfoCell(
                label = stringResource(R.string.flash_est_distance),
                value = estDistance,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    sub: String? = null,
    chip: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.quietClickable(onClick) else Modifier)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = Slate,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Snow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (sub != null) {
            Text(
                text = sub,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
        }
        if (chip != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Volt.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = chip,
                    color = Volt,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 참가자
// ─────────────────────────────────────────────────────────────

@Composable
private fun FlashParticipantsCard(post: Post, onViewMembers: () -> Unit) {
    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.flash_members),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.post_slots, post.joinedCount, post.capacity),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Volt,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AvatarStack(
                visible = minOf(post.joinedCount, 5),
                extra = (post.joinedCount - 5).coerceAtLeast(0),
                modifier = Modifier.weight(1f),
            )
            GhostButton(
                text = stringResource(R.string.flash_view_members),
                onClick = onViewMembers,
            )
        }
    }
}

@Composable
private fun FlashMembersDialog(post: Post, onDismiss: () -> Unit) {
    // 주최자를 맨 앞에 두고, 나머지는 고정 데모 러너로 채운다 (최대 10명)
    val members = remember(post.id, post.author, post.joinedCount) {
        (listOf(post.author) + demoRunners)
            .distinct()
            .take(post.joinedCount.coerceIn(1, 10))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Carbon,
        titleContentColor = Snow,
        textContentColor = Silver,
        title = { Text(stringResource(R.string.flash_members)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                members.forEachIndexed { index, name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (index == 0) Volt.copy(alpha = 0.18f) else CarbonHigh,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = name.take(1).uppercase(),
                                color = if (index == 0) Volt else Silver,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Text(
                            text = name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Snow,
                            modifier = Modifier.weight(1f),
                        )
                        if (index == 0) {
                            Text(
                                text = stringResource(R.string.flash_host),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Volt,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok), color = Volt)
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────
// 파티 채팅 (댓글 미리보기)
// ─────────────────────────────────────────────────────────────

@Composable
private fun FlashChatCard(
    post: Post,
    threads: List<com.giwa.strideup.domain.CommentThread>,
    onEnterChat: () -> Unit,
) {
    val latest = remember(threads) {
        threads.sortedByDescending { it.comment.createdAt }.take(2)
    }
    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = stringResource(R.string.flash_chat),
                style = MaterialTheme.typography.titleSmall,
                color = Snow,
            )
            Text(
                text = stringResource(R.string.flash_chat_latest),
                fontSize = 10.sp,
                color = Slate,
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.quietClickable(onEnterChat),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.flash_enter_chat),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Volt,
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        if (latest.isEmpty()) {
            Text(
                text = stringResource(R.string.comments_empty),
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
            )
        } else {
            latest.forEach { thread ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(CarbonHigh, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = thread.comment.author.take(1).uppercase(),
                            color = Silver,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = thread.comment.author,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Snow,
                            )
                            Text(
                                text = relativeTime(thread.comment.createdAt),
                                fontSize = 10.sp,
                                color = Slate,
                            )
                        }
                        Text(
                            text = thread.comment.body,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Silver,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 하단 CTA
// ─────────────────────────────────────────────────────────────

@Composable
private fun FlashCtaRow(
    post: Post,
    onLike: () -> Unit,
    onToggleJoin: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(CarbonHigh)
                .quietClickable(onLike),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (post.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (post.liked) Alert else Silver,
                modifier = Modifier.size(20.dp),
            )
        }
        if (!post.joined) {
            VoltButton(
                text = stringResource(R.string.flash_join_cta),
                onClick = onToggleJoin,
                modifier = Modifier.weight(1f),
                enabled = !post.isClosed && !post.isFull,
            )
        } else {
            GhostButton(
                text = stringResource(R.string.flash_joined_cta),
                onClick = onToggleJoin,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
