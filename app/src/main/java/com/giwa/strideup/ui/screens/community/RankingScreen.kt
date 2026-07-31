package com.giwa.strideup.ui.screens.community

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.domain.FactionLeaderboard
import com.giwa.strideup.domain.FactionRank
import com.giwa.strideup.domain.Leaderboard
import com.giwa.strideup.domain.RankBoard
import com.giwa.strideup.domain.RankEntry
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/**
 * 랭킹 — 이번 주 걸음 수와 누적 SUP 두 가지 기준.
 *
 * 내 실적은 실제 데이터에서 오고, 나머지 러너는 고정 시드로 만들어 순위가 흔들리지 않는다.
 */
@Composable
fun RankingScreen(
    onBack: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val topSpeed by viewModel.topSpeedKmh.collectAsStateWithLifecycle()
    val activeSec by viewModel.totalActiveSec.collectAsStateWithLifecycle()
    val factionKm by viewModel.factionKm.collectAsStateWithLifecycle()
    val myFaction by viewModel.myFaction.collectAsStateWithLifecycle()
    var boardIndex by rememberSaveable { mutableIntStateOf(0) }

    val personalBoards = RankBoard.entries
    val isFactionTab = boardIndex >= personalBoards.size
    val board = personalBoards[boardIndex.coerceIn(0, personalBoards.lastIndex)]
    val meLabel = stringResource(R.string.rank_me)
    val entries = remember(board, topSpeed, activeSec, balance, meLabel) {
        Leaderboard.build(board, meLabel, topSpeed, activeSec, balance)
    }
    val factions = remember(factionKm, myFaction) {
        FactionLeaderboard.build(factionKm, myFaction)
    }
    val me = entries.first { it.isMe }
    val podium = entries.take(3)

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
            items(factions, key = { it.faction.id }) { row -> FactionRow(row) }
            return@LazyColumn
        }

        item { Podium(podium, board) }

        item {
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
                        entries.size,
                    ),
                    fontSize = 11.sp,
                    color = Silver,
                )
            }
        }

        items(entries, key = { it.name }) { entry ->
            RankRow(entry = entry, board = board)
        }
    }
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
