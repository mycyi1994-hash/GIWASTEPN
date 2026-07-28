package com.giwa.strideup.ui.screens.community

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.ui.components.AvatarStack
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexBadge
import com.giwa.strideup.ui.components.PillChip
import com.giwa.strideup.ui.components.RouteMap
import com.giwa.strideup.ui.components.SectionHeader
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.Alert
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

private data class Crew(val monogram: String, val name: String, val kmAway: String, val members: Int)

private val demoCrews = listOf(
    Crew("TB", "Trailblazer Crew", "0.8", 128),
    Crew("NR", "Night Runners", "1.3", 86),
    Crew("SS", "Summit Seekers", "2.1", 142),
)

@Composable
fun CommunityScreen() {
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.toast_coming_soon)
    val showComingSoon = { Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show() }

    var query by rememberSaveable { mutableStateOf("") }
    var selectedChip by rememberSaveable { mutableIntStateOf(0) }

    val chipLabels = listOf(
        stringResource(R.string.chip_discover),
        stringResource(R.string.chip_crews),
        stringResource(R.string.chip_events),
        stringResource(R.string.chip_following),
        stringResource(R.string.chip_invites),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
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
                        text = " / PARTY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp,
                        color = Volt,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DarkIconButton(
                        icon = Icons.Filled.Notifications,
                        contentDescription = stringResource(R.string.cd_notifications),
                        onClick = showComingSoon,
                        badge = true,
                    )
                    DarkIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = null,
                        onClick = showComingSoon,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CarbonHigh)
                        .border(1.dp, Edge, RoundedCornerShape(16.dp))
                        .quietClickable(showComingSoon)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = Snow, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.community_filter),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Snow,
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chipLabels.size) { index ->
                    PillChip(
                        text = chipLabels[index],
                        selected = selectedChip == index,
                        onClick = { selectedChip = index },
                        badge = if (index == 4) 3 else 0,
                    )
                }
            }
        }

        if (selectedChip != 0) {
            item {
                GlowCard(contentPadding = PaddingValues(26.dp)) {
                    Text(
                        text = stringResource(R.string.community_section_soon),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                }
            }
        } else {
            item {
                SectionHeader(
                    title = stringResource(R.string.community_nearby),
                    actionText = stringResource(R.string.community_view_map),
                    onAction = showComingSoon,
                )
            }

            item {
                val filtered = demoCrews.filter { it.name.contains(query, ignoreCase = true) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    items(filtered) { crew -> CrewCard(crew) }
                }
            }

            item { PartyRunCard() }

            item { PopularPostCard() }

            item { LookingForRunners(onJoinToast = showComingSoon) }

            item { ActivityFeed() }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CarbonHigh)
            .border(1.dp, Edge, RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Slate, modifier = Modifier.size(17.dp))
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

@Composable
private fun CrewCard(crew: Crew) {
    GlowCard(
        modifier = Modifier.width(168.dp),
        contentPadding = PaddingValues(16.dp),
        spacing = 9.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            HexBadge(text = crew.monogram, size = 44.dp)
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Volt, CircleShape),
            )
        }
        Text(crew.name, style = MaterialTheme.typography.titleSmall, color = Snow)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Slate, modifier = Modifier.size(11.dp))
            Text(
                text = stringResource(R.string.community_km_away, crew.kmAway),
                fontSize = 11.sp,
                color = Slate,
            )
        }
        AvatarStack(visible = 4, extra = crew.members / 10)
        Text(
            text = stringResource(R.string.community_members, crew.members),
            fontSize = 11.sp,
            color = Silver,
        )
    }
}

@Composable
private fun PartyRunCard() {
    var joined by rememberSaveable { mutableStateOf(false) }
    GlowCard(accent = true, contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        Text(
            text = stringResource(R.string.community_tonight),
            style = MaterialTheme.typography.labelSmall,
            color = Volt,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Neon Night Run", style = MaterialTheme.typography.titleLarge, color = Snow)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.NightsStay,
                        contentDescription = null,
                        tint = Volt,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = Silver, modifier = Modifier.size(12.dp))
                    Text("8:00 PM", fontSize = 12.sp, color = Silver)
                    Text("·", fontSize = 12.sp, color = Slate)
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Silver, modifier = Modifier.size(12.dp))
                    Text("Riverside Park", fontSize = 12.sp, color = Silver)
                    Text("·", fontSize = 12.sp, color = Slate)
                    Text("6.2 km", fontSize = 12.sp, color = Silver)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvatarStack(visible = 5, extra = 0, dot = 22.dp)
                    Text(
                        text = stringResource(R.string.community_going, if (joined) 25 else 24),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Volt,
                    )
                }
            }
            RouteMap(
                modifier = Modifier
                    .width(120.dp)
                    .height(96.dp),
            )
        }
        if (joined) {
            GhostButton(
                text = stringResource(R.string.community_joined) + " ✓",
                onClick = { joined = false },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            VoltButton(
                text = stringResource(R.string.community_im_in),
                onClick = { joined = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PopularPostCard() {
    var liked by rememberSaveable { mutableStateOf(false) }
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        SectionHeader(title = stringResource(R.string.community_popular))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.5.dp, Volt, CircleShape)
                    .padding(3.dp)
                    .background(CarbonHigh, CircleShape),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Alex R.", style = MaterialTheme.typography.titleSmall, color = Snow)
                Text(
                    text = stringResource(R.string.time_hours_ago, 2),
                    fontSize = 11.sp,
                    color = Slate,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Volt.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text("Trailblazer Crew", fontSize = 10.sp, color = Volt, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            text = stringResource(R.string.community_post_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Snow,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.quietClickable { liked = !liked },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (liked) Alert else Silver,
                    modifier = Modifier.size(17.dp),
                )
                Text("${if (liked) 46 else 45}", fontSize = 12.sp, color = Silver)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
                Text("12", fontSize = 12.sp, color = Silver)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Share, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LookingForRunners(onJoinToast: () -> Unit) {
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 13.dp) {
        SectionHeader(title = stringResource(R.string.community_looking))
        RecruitRow(
            icon = Icons.Filled.FavoriteBorder,
            tag = stringResource(R.string.community_beginner),
            name = "New Striders",
            desc = stringResource(R.string.community_crew_all_levels),
            count = "8 / 12",
            onJoin = onJoinToast,
        )
        RecruitRow(
            icon = Icons.Filled.NightsStay,
            tag = null,
            name = "Night Owls",
            desc = stringResource(R.string.community_crew_night),
            count = "6 / 10",
            onJoin = onJoinToast,
        )
    }
}

@Composable
private fun RecruitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String?,
    name: String,
    desc: String,
    count: String,
    onJoin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CarbonHigh.copy(alpha = 0.6f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (tag != null) {
            Text(tag, fontSize = 11.sp, color = Volt, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(icon, contentDescription = null, tint = Volt, modifier = Modifier.size(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = Snow)
                Text(desc, fontSize = 11.sp, color = Silver)
            }
            Text(count, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Snow)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AvatarStack(visible = 4, extra = 4, dot = 20.dp)
            GhostButton(text = stringResource(R.string.community_join_crew), onClick = onJoin)
        }
    }
}

@Composable
private fun ActivityFeed() {
    GlowCard(contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
        SectionHeader(title = stringResource(R.string.community_activity))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(CarbonHigh, CircleShape),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = stringResource(R.string.community_feed_joined, "Maya C.", "Night Runners"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.time_hours_ago, 1),
                    fontSize = 11.sp,
                    color = Slate,
                )
            }
            HexBadge(text = "NR", size = 30.dp)
        }
    }
}
