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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.data.repo.Crew
import com.giwa.strideup.domain.Leaderboard
import com.giwa.strideup.domain.Post
import com.giwa.strideup.domain.PostCategory
import com.giwa.strideup.domain.RankBoard
import com.giwa.strideup.ui.components.AvatarStack
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexBadge
import com.giwa.strideup.ui.components.PillChip
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.guide.GuideTour
import com.giwa.strideup.ui.guide.guideTarget
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/**
 * 커뮤니티 — 두 개의 세그먼트로 나뉜다.
 *
 * **게시판**: 앱 사용자 전체가 쓰는 열린 공간. 번개러닝(가까운 순) · 자유 · 꿀팁.
 * **크루**: 내가 속한 소규모 커뮤니티. 모임 만들기와 크루 전용 게시판.
 */
@Composable
fun CommunityScreen(
    onOpenLobby: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenRanking: () -> Unit = {},
    onOpenCrew: (String) -> Unit = {},
    onCreateCrew: () -> Unit = {},
    onWritePost: (String) -> Unit = {},
    onOpenFlash: (Long) -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val segments = listOf(
        stringResource(R.string.community_seg_board),
        stringResource(R.string.community_seg_crew),
    )

    // 댓글 창은 어느 세그먼트에 있든 같은 뷰모델이 열고 닫는다
    CommentSheetHost(viewModel)

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tab_community),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                    Text(
                        text = " / GIWA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp,
                        color = Volt,
                    )
                }
                DarkIconButton(
                    icon = Icons.Filled.Notifications,
                    contentDescription = stringResource(R.string.cd_notifications),
                    onClick = onOpenNotifications,
                    badge = true,
                )
            }

            SegmentedTabs(
                labels = segments,
                selected = if (tab == CommunityTab.BOARD) 0 else 1,
                onSelect = {
                    viewModel.selectTab(if (it == 0) CommunityTab.BOARD else CommunityTab.CREW)
                },
                modifier = Modifier.guideTarget(GuideTour.Targets.COMMUNITY_SEGMENTS),
            )
        }

        when (tab) {
            CommunityTab.BOARD -> BoardTab(
                viewModel = viewModel,
                onOpenRanking = onOpenRanking,
                onWritePost = { onWritePost("") },
                onOpenFlash = onOpenFlash,
            )

            CommunityTab.CREW -> CrewTab(
                viewModel = viewModel,
                onOpenLobby = onOpenLobby,
                onOpenCrew = onOpenCrew,
                onCreateCrew = onCreateCrew,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 게시판 탭
// ─────────────────────────────────────────────────────────────

@Composable
private fun BoardTab(
    viewModel: CommunityViewModel,
    onOpenRanking: () -> Unit,
    onWritePost: () -> Unit,
    onOpenFlash: (Long) -> Unit,
) {
    val posts by viewModel.boardPosts.collectAsStateWithLifecycle()
    val filter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    val meLabel = stringResource(R.string.rank_me)
    val topSpeed by viewModel.topSpeedKmh.collectAsStateWithLifecycle()
    val activeSec by viewModel.totalActiveSec.collectAsStateWithLifecycle()
    // 티저는 적립 랭킹을 보여준다 — 세 부문 중 누구에게나 값이 있는 축이다
    val myRank = remember(topSpeed, activeSec, balance, meLabel) {
        Leaderboard.build(RankBoard.TOTAL_SUP, meLabel, topSpeed, activeSec, balance)
            .first { it.isMe }
            .rank
    }

    val visible = remember(posts, filter, query) { filterPosts(posts, filter, query) }
    val flashWindow = remember(posts, query) {
        filterPosts(posts, PostCategory.FLASH, query)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                RankingTeaser(
                    rank = myRank,
                    balance = balance,
                    onClick = onOpenRanking,
                )
            }

            item {
                SearchField(query, { query = it })
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        PillChip(
                            text = stringResource(R.string.post_cat_all),
                            selected = filter == null,
                            onClick = { viewModel.selectCategory(null) },
                        )
                    }
                    items(PostCategory.entries.size) { index ->
                        val category = PostCategory.entries[index]
                        PillChip(
                            text = category.label(),
                            selected = filter == category,
                            onClick = { viewModel.selectCategory(category) },
                        )
                    }
                }
            }

            if (filter == null || filter == PostCategory.FLASH) {
                item {
                    SectionHeader(title = stringResource(R.string.community_flash_nearby))
                }
            }

            // 번개러닝 창내창 — 카드 2개 높이만 차지하고 안에서 스크롤한다
            if (filter == null && flashWindow.isNotEmpty()) {
                item {
                    FlashRunWindow(
                        posts = flashWindow,
                        onJoin = { viewModel.toggleJoinFlash(it) },
                        onLike = { viewModel.toggleLike(it) },
                        onComment = { viewModel.openComments(it) },
                        onDelete = { viewModel.deletePost(it) },
                        onOpen = onOpenFlash,
                    )
                }
            }

            // 일반 글 목록 제목 — "가까운 번개러닝"과 짝을 이룬다
            if (filter == null) {
                item {
                    SectionHeader(title = stringResource(R.string.community_board_section))
                }
            }

            if (visible.isEmpty()) {
                item {
                    GlowCard(contentPadding = PaddingValues(26.dp)) {
                        Text(
                            text = if (query.isBlank()) {
                                stringResource(R.string.community_board_empty)
                            } else {
                                stringResource(R.string.community_no_results, query)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Silver,
                        )
                    }
                }
            }

            items(visible, key = { it.id }) { post ->
                if (post.isFlash) {
                    FlashRunCard(
                        post = post,
                        onJoin = { viewModel.toggleJoinFlash(post.id) },
                        onLike = { viewModel.toggleLike(post.id) },
                        onComment = { viewModel.openComments(post.id) },
                        onDelete = { viewModel.deletePost(post.id) },
                        onOpen = { onOpenFlash(post.id) },
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

        WriteFab(
            onClick = onWritePost,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 20.dp)
                .guideTarget(GuideTour.Targets.COMMUNITY_WRITE),
        )
    }
}

/**
 * 번개러닝은 가까운 순, 나머지는 최신 순.
 * 필터가 없으면 번개러닝을 위로 올려 "지금 뛸 사람"이 먼저 보이게 한다.
 */
private fun filterPosts(
    posts: List<Post>,
    filter: PostCategory?,
    query: String,
): List<Post> {
    val matched = posts.filter { post ->
        (filter == null || post.category == filter) &&
            (
                query.isBlank() ||
                    post.title.contains(query, ignoreCase = true) ||
                    post.body.contains(query, ignoreCase = true) ||
                    post.author.contains(query, ignoreCase = true)
                )
    }
    val (flash, rest) = matched.partition { it.isFlash }
    val sortedFlash = flash.sortedWith(compareBy({ it.isClosed }, { it.distanceKm }))
    val sortedRest = rest.sortedByDescending { it.createdAt }
    return when (filter) {
        PostCategory.FLASH -> sortedFlash
        // 전체 보기에서는 번개러닝을 창내창이 따로 보여주므로 일반 글만
        null -> sortedRest
        else -> sortedRest
    }
}

/**
 * 번개러닝 모집 창 — 화면에는 2개 높이만 보이고 안에서 스크롤해 나머지를 본다.
 * (그룹모집을 한눈에, 피드는 그 아래로)
 */
@Composable
private fun FlashRunWindow(
    posts: List<Post>,
    onJoin: (Long) -> Unit,
    onLike: (Long) -> Unit,
    onComment: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpen: (Long) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(452.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Volt.copy(alpha = 0.05f))
            .border(1.dp, Volt.copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(posts, key = { it.id }) { post ->
                FlashRunCard(
                    post = post,
                    onJoin = { onJoin(post.id) },
                    onLike = { onLike(post.id) },
                    onComment = { onComment(post.id) },
                    onDelete = { onDelete(post.id) },
                    onOpen = { onOpen(post.id) },
                )
            }
        }
        // 아래에 더 있음을 알리는 하단 페이드
        if (posts.size > 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                Night.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun RankingTeaser(rank: Int, balance: Double, onClick: () -> Unit) {
    GlowCard(
        modifier = Modifier
            .quietClickable(onClick)
            .guideTarget(GuideTour.Targets.COMMUNITY_RANKING),
        accent = true,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        spacing = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Volt.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.community_ranking),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(
                        R.string.ranking_teaser,
                        rank,
                        "%,.0f".format(balance),
                    ),
                    fontSize = 11.sp,
                    color = Silver,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun WriteFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Volt)
            .quietClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
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

// ─────────────────────────────────────────────────────────────
// 크루 탭
// ─────────────────────────────────────────────────────────────

@Composable
private fun CrewTab(
    viewModel: CommunityViewModel,
    onOpenLobby: (String) -> Unit,
    onOpenCrew: (String) -> Unit,
    onCreateCrew: () -> Unit,
) {
    val crews by viewModel.crews.collectAsStateWithLifecycle()
    val joined by viewModel.joinedCrewIds.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = crews.filter { it.name.contains(query, ignoreCase = true) }
    val myCrews = filtered.filter { joined.contains(it.id) }
    val others = filtered.filterNot { joined.contains(it.id) }.sortedBy { it.kmAway }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item { CreateCrewCard(onClick = onCreateCrew) }

        item { SearchField(query, { query = it }) }

        item {
            SectionHeader(
                title = stringResource(R.string.community_my_crews),
                actionText = "${myCrews.size}",
            )
        }

        if (myCrews.isEmpty()) {
            item {
                GlowCard(contentPadding = PaddingValues(24.dp)) {
                    Text(
                        text = stringResource(R.string.community_no_crew_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                }
            }
        }

        items(myCrews, key = { "my_${it.id}" }) { crew ->
            CrewCard(
                crew = crew,
                joined = true,
                onToggleJoin = { viewModel.toggleJoin(crew.id) },
                onOpenLobby = { onOpenLobby(crew.id) },
                onOpenBoard = { onOpenCrew(crew.id) },
            )
        }

        item { SectionHeader(title = stringResource(R.string.community_nearby)) }

        if (others.isEmpty()) {
            item {
                GlowCard(contentPadding = PaddingValues(24.dp)) {
                    Text(
                        text = if (query.isBlank()) {
                            stringResource(R.string.common_none)
                        } else {
                            stringResource(R.string.community_no_results, query)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                }
            }
        }

        items(others, key = { "near_${it.id}" }) { crew ->
            CrewCard(
                crew = crew,
                joined = false,
                onToggleJoin = { viewModel.toggleJoin(crew.id) },
                onOpenLobby = { onOpenLobby(crew.id) },
                onOpenBoard = { onOpenCrew(crew.id) },
            )
        }
    }
}

/** 당근 그룹처럼 "모임 만들기"를 크루 탭 맨 위에 둔다. */
@Composable
private fun CreateCrewCard(onClick: () -> Unit) {
    GlowCard(
        modifier = Modifier.quietClickable(onClick),
        contentPadding = PaddingValues(16.dp),
        spacing = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Volt.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.crew_create_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.crew_create_sub),
                    fontSize = 11.sp,
                    color = Silver,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CrewCard(
    crew: Crew,
    joined: Boolean,
    onToggleJoin: () -> Unit,
    onOpenLobby: () -> Unit,
    onOpenBoard: () -> Unit,
) {
    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp, accent = joined) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .quietClickable(onOpenBoard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            HexBadge(text = crew.monogram, size = 46.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(crew.name, style = MaterialTheme.typography.titleSmall, color = Snow)
                    if (crew.owned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Volt.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.crew_owner_badge),
                                color = Volt,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
                if (crew.tagline.isNotBlank()) {
                    Text(crew.tagline, fontSize = 11.sp, color = Silver, maxLines = 1)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Slate,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = if (crew.kmAway > 0) {
                            stringResource(R.string.community_km_away, "%.1f".format(crew.kmAway))
                        } else {
                            crew.area.ifBlank { stringResource(R.string.crew_area_here) }
                        },
                        fontSize = 11.sp,
                        color = Slate,
                    )
                    Text("·", fontSize = 11.sp, color = Slate)
                    Text(
                        text = stringResource(R.string.community_members, crew.memberCount),
                        fontSize = 11.sp,
                        color = Silver,
                    )
                }
            }
            AvatarStack(visible = 3, extra = crew.memberCount / 10, dot = 22.dp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (joined) {
                VoltButton(
                    text = stringResource(R.string.crew_open_lobby),
                    onClick = onOpenLobby,
                    modifier = Modifier.weight(1f),
                )
                GhostButton(
                    text = stringResource(R.string.crew_board),
                    onClick = onOpenBoard,
                )
                GhostButton(
                    text = stringResource(R.string.community_leave_crew),
                    onClick = onToggleJoin,
                    accent = Silver,
                )
            } else {
                GhostButton(
                    text = stringResource(R.string.community_join_crew),
                    onClick = onToggleJoin,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonHigh)
            .border(1.dp, Edge, RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier.size(17.dp),
        )
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.community_search_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Snow, fontSize = 13.sp),
                cursorBrush = SolidColor(Volt),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
