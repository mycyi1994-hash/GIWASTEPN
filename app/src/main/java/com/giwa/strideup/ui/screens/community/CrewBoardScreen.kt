package com.giwa.strideup.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.domain.RewardEconomy
import com.giwa.strideup.ui.components.AvatarStack
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexBadge
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/**
 * 크루 상세 — 크루 전용 게시판.
 *
 * 가입한 사람만 글을 쓸 수 있고, 파티런 로비도 여기서 바로 연다.
 */
@Composable
fun CrewBoardScreen(
    crewId: String,
    onBack: () -> Unit = {},
    onOpenLobby: (String) -> Unit = {},
    onWritePost: (String) -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    val crews by viewModel.crews.collectAsStateWithLifecycle()
    val joined by viewModel.joinedCrewIds.collectAsStateWithLifecycle()
    val postFlow = remember(crewId) { viewModel.crewPosts(crewId) }
    val posts by postFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val crew = crews.firstOrNull { it.id == crewId }
    val isMember = joined.contains(crewId)
    val sorted = remember(posts) {
        posts.sortedWith(compareByDescending<com.giwa.strideup.domain.Post> { it.isFlash && !it.isClosed }
            .thenByDescending { it.createdAt })
    }

    CommentSheetHost(viewModel)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 10.dp,
                bottom = if (isMember) 92.dp else 26.dp,
            ),
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
                    Text(
                        text = crew?.name.orEmpty(),
                        modifier = Modifier.weight(1f),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                }
            }

            if (crew == null) {
                item {
                    GlowCard(contentPadding = PaddingValues(24.dp)) {
                        Text(
                            text = stringResource(R.string.common_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Silver,
                        )
                    }
                }
                return@LazyColumn
            }

            item {
                GlowCard(accent = true, contentPadding = PaddingValues(18.dp), spacing = 13.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        HexBadge(text = crew.monogram, size = 52.dp)
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = crew.tagline.ifBlank {
                                    stringResource(R.string.crew_create_preview_tag)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Snow,
                            )
                            Text(
                                text = stringResource(
                                    R.string.community_members,
                                    crew.memberCount,
                                ) + crew.area.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                                fontSize = 11.sp,
                                color = Silver,
                            )
                        }
                        AvatarStack(visible = 3, extra = crew.memberCount / 10, dot = 22.dp)
                    }
                    Text(
                        text = stringResource(
                            R.string.crew_boost,
                            RewardEconomy.partyBonusPercent(crew.roster.size + 1),
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                    if (isMember) {
                        VoltButton(
                            text = stringResource(R.string.crew_open_lobby),
                            onClick = { onOpenLobby(crewId) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        GhostButton(
                            text = stringResource(R.string.community_join_crew),
                            onClick = { viewModel.toggleJoin(crewId) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (sorted.isEmpty()) {
                item {
                    GlowCard(contentPadding = PaddingValues(26.dp)) {
                        Text(
                            text = stringResource(R.string.crew_board_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Silver,
                        )
                    }
                }
            }

            items(sorted, key = { it.id }) { post ->
                if (post.isFlash) {
                    FlashRunCard(
                        post = post,
                        onJoin = { viewModel.toggleJoinFlash(post.id) },
                        onLike = { viewModel.toggleLike(post.id) },
                        onComment = { viewModel.openComments(post.id) },
                        onDelete = { viewModel.deletePost(post.id) },
                    )
                } else {
                    TextPostCard(
                        post = post,
                        onLike = { viewModel.toggleLike(post.id) },
                        onComment = { viewModel.openComments(post.id) },
                        onDelete = { viewModel.deletePost(post.id) },
                    )
                }
            }
        }

        if (isMember) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Volt)
                    .quietClickable { onWritePost(crewId) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                androidx.compose.material3.Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = Night,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = stringResource(R.string.post_write),
                    color = Night,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
