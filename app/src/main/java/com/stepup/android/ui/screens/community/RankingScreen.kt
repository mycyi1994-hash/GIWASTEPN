package com.stepup.android.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.repo.FactionRankingState
import com.stepup.android.data.repo.RankingProblem
import com.stepup.android.data.repo.RankingState
import com.stepup.android.domain.FactionRank
import com.stepup.android.domain.RankBoard
import com.stepup.android.domain.RankEntry
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt

/**
 * 랭킹.
 *
 * 줄 세우는 축은 넷이다 — 쾌속·지구력·적립, 그리고 종족.
 *
 * **여기 보이는 사람은 전부 실제 사용자다.** 예전에는 상대 15명이 코드에
 * 박혀 있었다. 그러면 "당신은 3등입니다"는 거짓말이 된다. 지금은 서버가
 * 계산하고, 못 가져오면 지어내지 않고 못 가져왔다고 말한다.
 */
@Composable
fun RankingScreen(
    onBack: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    var boardIndex by rememberSaveable { mutableIntStateOf(0) }

    val personalBoards = RankBoard.entries
    val isFactionTab = boardIndex >= personalBoards.size
    val board = personalBoards[boardIndex.coerceIn(0, personalBoards.lastIndex)]
    val meLabel = stringResource(R.string.rank_me)

    val ranking by viewModel.ranking.collectAsStateWithLifecycle()
    val factionRanking by viewModel.factionRanking.collectAsStateWithLifecycle()

    // 탭을 옮길 때마다 그 부문을 받아 온다. 이미 받아 둔 부문은 다시 묻지
    // 않는다 — 같은 답을 받으려고 네트워크를 쓸 이유가 없다.
    LaunchedEffect(boardIndex) {
        if (isFactionTab) viewModel.loadFactionRanking() else viewModel.loadRanking(board, meLabel)
    }

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
                Text(
                    text = stringResource(R.string.community_ranking),
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Snow,
                )
            }
        }

        item {
            SegmentedTabs(
                labels = listOf(
                    stringResource(R.string.rank_board_speed),
                    stringResource(R.string.rank_board_time),
                    stringResource(R.string.rank_board_sup),
                    stringResource(R.string.rank_board_faction),
                ),
                selected = boardIndex,
                onSelect = { boardIndex = it },
            )
        }

        if (isFactionTab) {
            item {
                GlowCard(contentPadding = PaddingValues(16.dp), spacing = 6.dp) {
                    Text(
                        text = stringResource(R.string.rank_faction_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                    Text(
                        text = stringResource(R.string.rank_faction_body),
                        fontSize = 11.sp,
                        color = Silver,
                        lineHeight = 17.sp,
                    )
                }
            }
            when (val state = factionRanking) {
                is FactionRankingState.Loading -> item { RankingNotice(R.string.rank_loading) }
                is FactionRankingState.Failed -> item {
                    RankingNotice(state.problem.message()) { viewModel.loadFactionRanking(force = true) }
                }
                is FactionRankingState.Ready ->
                    items(state.rows, key = { it.faction.id }) { row -> FactionRow(row) }
            }
            return@LazyColumn
        }

        val ready = ranking as? RankingState.Ready
        if (ready == null) {
            item {
                val state = ranking
                if (state is RankingState.Failed) {
                    RankingNotice(state.problem.message()) {
                        viewModel.loadRanking(board, meLabel, force = true)
                    }
                } else {
                    RankingNotice(R.string.rank_loading)
                }
            }
            return@LazyColumn
        }

        val entries = ready.entries
        val me = ready.me

        // 시상대는 3명이 모여야 성립한다. 아직 두 명뿐일 때 빈 자리를 세워
        // 두면 순위표가 아니라 공사장처럼 보인다.
        if (entries.size >= 3) item { Podium(entries.take(3), board) }

        if (me == null) {
            // 순위에 오르려면 한 번은 뛰어야 한다. 0으로 채운 줄을 만들어
            // "전체 1명 중 1등"이라고 하는 것보다 이렇게 말하는 편이 낫다.
            item { RankingNotice(R.string.rank_no_record) }
        } else item {
            GlowCard(accent = true, contentPadding = PaddingValues(16.dp), spacing = 4.dp) {
                Text(
                    text = stringResource(R.string.rank_my_position),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Slate,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "#${me.rank}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp,
                        color = Volt,
                    )
                    Text(
                        text = valueLabel(me, board),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Snow,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.rank_total_runners,
                        ready.totalRunners,
                    ),
                    fontSize = 11.sp,
                    color = Silver,
                )
            }
        }

        items(entries, key = { it.rank }) { entry ->
            RankRow(entry = entry, board = board)
        }
    }
}

/**
 * 순위 대신 보여 줄 한 줄.
 *
 * 비워 두면 사용자는 화면이 고장 난 줄 안다. 왜 비었는지 말하고, 다시
 * 해 볼 수 있는 것이면 누를 자리를 준다.
 */
@Composable
private fun RankingNotice(@StringRes message: Int, onRetry: (() -> Unit)? = null) {
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 8.dp) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = Silver,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (onRetry != null) {
            Text(
                text = stringResource(R.string.rank_retry),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Volt,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .quietClickable(onRetry)
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@StringRes
private fun RankingProblem.message(): Int = when (this) {
    RankingProblem.OFFLINE -> R.string.rank_offline
    RankingProblem.SIGN_IN_REQUIRED -> R.string.rank_sign_in
    RankingProblem.REJECTED -> R.string.rank_failed
}

@Composable
private fun Podium(top: List<RankEntry>, board: RankBoard) {
    if (top.size < 3) return
    // 2등 - 1등 - 3등 순으로 세운다
    val order = listOf(top[1] to 78, top[0] to 104, top[2] to 62)
    GlowCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp), spacing = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            order.forEach { (entry, barHeight) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .border(
                                width = if (entry.isMe) 2.dp else 1.dp,
                                color = if (entry.isMe) Volt else medalColor(entry.rank),
                                shape = CircleShape,
                            )
                            .background(CarbonHigh, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = entry.monogram,
                            color = Snow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        text = entry.name,
                        color = if (entry.isMe) Volt else Silver,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(medalColor(entry.rank).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "${entry.rank}",
                                color = medalColor(entry.rank),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = valueLabel(entry, board),
                                color = Silver,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankRow(entry: RankEntry, board: RankBoard) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (entry.isMe) Volt.copy(alpha = 0.11f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "${entry.rank}",
                color = if (entry.rank <= 3) medalColor(entry.rank) else Slate,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(if (entry.isMe) Volt else CarbonHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.monogram,
                color = if (entry.isMe) Night else Silver,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = entry.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (entry.isMe) Snow else Silver,
            fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = valueLabel(entry, board),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (entry.isMe) Volt else Snow,
        )
    }
}

private fun medalColor(rank: Int): Color = when (rank) {
    1 -> Color(0xFFFFC24F)
    2 -> Color(0xFFC8D2DA)
    3 -> Color(0xFFD08A5A)
    else -> Color(0xFF6B7480)
}

@Composable
private fun valueLabel(entry: RankEntry, board: RankBoard): String = when (board) {
    RankBoard.TOP_SPEED -> "%.1f km/h".format(entry.topSpeedKmh)
    RankBoard.LONGEST_TIME -> durationLabel(entry.activeSec)
    RankBoard.TOTAL_SUP -> "%,.0f SUP".format(entry.sup)
}

/** 누적 시간을 "12h 30m" / "45m" 로 — 랭킹 줄에 들어갈 만큼 짧게 */
private fun durationLabel(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/** 종족 한 줄 — 순위 · 이름 · 누적 거리 · 내 기여 비중 막대 */
@Composable
private fun FactionRow(row: FactionRank) {
    GlowCard(
        accent = row.isMine,
        contentPadding = PaddingValues(14.dp),
        spacing = 9.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(Modifier.width(24.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = "${row.rank}",
                    color = if (row.rank <= 3) medalColor(row.rank) else Slate,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(row.faction.accent).copy(alpha = 0.18f), CircleShape)
                    .border(1.dp, Color(row.faction.accent), CircleShape),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = row.faction.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.rank_faction_km, "%,.1f".format(row.km)),
                    fontSize = 11.sp,
                    color = Silver,
                )
            }
            if (row.isMine) {
                Text(
                    text = stringResource(R.string.rank_faction_mine),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Volt,
                )
            }
        }
        // 내 기여 — 종족 누적 대비 얼마나 보탰는지
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CarbonHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(row.myShare.coerceAtLeast(0.012f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(row.faction.accent)),
                )
            }
            Text(
                text = stringResource(R.string.rank_faction_my_km, "%,.2f".format(row.myKm)),
                fontSize = 10.sp,
                color = Slate,
            )
        }
    }
}
