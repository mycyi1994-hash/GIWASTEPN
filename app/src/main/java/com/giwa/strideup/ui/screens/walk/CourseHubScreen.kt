package com.giwa.strideup.ui.screens.walk

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.domain.CourseRewards
import com.giwa.strideup.domain.RunCourse
import com.giwa.strideup.domain.trackDistanceKm
import com.giwa.strideup.service.WalkSessionService
import com.giwa.strideup.ui.components.CourseTrackMap
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.screens.community.LabeledField
import com.giwa.strideup.ui.screens.community.SegmentedTabs
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/**
 * 러닝 코스 허브 — 코스 선택 · 코스 만들기(마지막 GPS 트랙 등록) · 코스 게시판.
 *
 * 게시판은 shared 플래그가 켜진 코스들이다. 백엔드가 없으므로 다른 러너의
 * 코스는 시드로 채우고, 내 코스는 공유 토글로 게시판에 올린다.
 */
@Composable
fun CourseHubScreen(
    onBack: () -> Unit = {},
    viewModel: CourseHubViewModel = viewModel(factory = CourseHubViewModel.Factory),
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()
    val lastTrack by WalkSessionService.lastTrack.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }

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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Column {
                    Eyebrow(text = stringResource(R.string.run_live))
                    Text(
                        text = stringResource(R.string.courses_title),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                }
            }
        }

        item {
            SegmentedTabs(
                labels = listOf(
                    stringResource(R.string.courses_select),
                    stringResource(R.string.courses_make),
                    stringResource(R.string.courses_board),
                ),
                selected = tab,
                onSelect = { tab = it },
            )
        }

        when (tab) {
            // ── 코스 선택 — 달릴 수 있는 모든 코스 ─────────────
            0 -> {
                val list = courses
                if (list.isEmpty()) {
                    item { EmptyCard(stringResource(R.string.courses_empty)) }
                } else {
                    items(list.size, key = { list[it].id }) { index ->
                        val course = list[index]
                        CourseCard(
                            course = course,
                            selected = course.id == selectedId,
                            onSelect = { viewModel.select(course.id) },
                            onLike = { viewModel.toggleLike(course.id) },
                            onShareToggle = if (course.mine) {
                                { viewModel.setShared(course.id, !course.shared) }
                            } else {
                                null
                            },
                            onDelete = if (course.mine) {
                                { viewModel.delete(course.id) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            // ── 코스 만들기 — 마지막 러닝 GPS 트랙을 등록 ───────
            1 -> {
                item { CourseMaker(lastTrack = lastTrack, onCreate = { name, area, shared ->
                    viewModel.create(name, area, lastTrack, shared)
                    tab = 0
                }) }
            }

            // ── 코스 게시판 — 러너들이 공유한 코스 ─────────────
            else -> {
                val shared = courses.filter { it.shared }
                if (shared.isEmpty()) {
                    item { EmptyCard(stringResource(R.string.courses_board_empty)) }
                } else {
                    items(shared.size, key = { shared[it].id }) { index ->
                        val course = shared[index]
                        CourseCard(
                            course = course,
                            selected = course.id == selectedId,
                            onSelect = { viewModel.select(course.id) },
                            onLike = { viewModel.toggleLike(course.id) },
                            showAuthor = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    GlowCard(contentPadding = PaddingValues(24.dp)) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Silver)
    }
}

/** 코스 한 장 — 미니 지도 + 이름 · 거리 · 보상 · 선택 */
@Composable
private fun CourseCard(
    course: RunCourse,
    selected: Boolean,
    onSelect: () -> Unit,
    onLike: () -> Unit,
    showAuthor: Boolean = false,
    onShareToggle: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    GlowCard(
        accent = selected,
        contentPadding = PaddingValues(14.dp),
        spacing = 10.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Night)
                    .border(1.dp, Edge, RoundedCornerShape(16.dp)),
            ) {
                CourseTrackMap(
                    points = remember(course.id) { course.normalized() },
                    seed = course.id.toInt(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (course.mine && course.shared) {
                        Badge(stringResource(R.string.course_shared_badge))
                    }
                }
                Text(
                    text = buildString {
                        if (course.area.isNotBlank()) append(course.area).append(" · ")
                        append("%.2f km".format(course.distanceKm))
                    },
                    fontSize = 11.sp,
                    color = Silver,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    HexEmblem(size = 13.dp, glow = false)
                    Text(
                        text = stringResource(
                            R.string.course_reward_value,
                            "%.1f".format(course.reward),
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                    Text(
                        text = stringResource(R.string.course_runs, course.runCount),
                        fontSize = 10.sp,
                        color = Slate,
                    )
                }
                if (showAuthor && course.author.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.course_by, course.author),
                        fontSize = 10.sp,
                        color = Slate,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                SelectDot(selected = selected, onClick = onSelect)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.quietClickable(onLike),
                ) {
                    Icon(
                        imageVector = if (course.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (course.liked) Volt else Slate,
                        modifier = Modifier.size(13.dp),
                    )
                    Text("${course.likes}", fontSize = 10.sp, color = Silver)
                }
            }
        }

        if (onShareToggle != null || onDelete != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onShareToggle != null) {
                    GhostButton(
                        text = stringResource(
                            if (course.shared) R.string.course_unshare else R.string.course_share,
                        ),
                        onClick = onShareToggle,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (onDelete != null) {
                    GhostButton(
                        text = stringResource(R.string.post_delete),
                        onClick = onDelete,
                        accent = Slate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Volt.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(text = text, color = Volt, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

/** 선택 라디오 — 체크되면 볼트 원 */
@Composable
private fun SelectDot(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (selected) Volt else CarbonHigh)
            .border(1.dp, if (selected) Volt else Edge, CircleShape)
            .quietClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.course_selected),
                tint = Night,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Icon(
                Icons.Filled.Flag,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** 코스 만들기 — 마지막 러닝의 GPS 트랙을 이름 붙여 등록한다 */
@Composable
private fun CourseMaker(
    lastTrack: List<com.giwa.strideup.domain.GeoPoint>,
    onCreate: (name: String, area: String, shared: Boolean) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf(true) }
    val km = remember(lastTrack) { lastTrack.trackDistanceKm() }

    GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Route, contentDescription = null, tint = Volt, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.course_make_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Silver,
                lineHeight = 18.sp,
            )
        }

        if (lastTrack.size < 2 || km < 0.2) {
            Text(
                text = stringResource(R.string.course_make_none),
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Night)
                    .border(1.dp, Edge, RoundedCornerShape(18.dp)),
            ) {
                CourseTrackMap(
                    points = remember(lastTrack) {
                        RunCourse(
                            id = 0, name = "", area = "", distanceKm = km, elevationM = 0,
                            points = lastTrack, author = "", mine = true, shared = false,
                            likes = 0, liked = false, runCount = 0, createdAt = 0,
                        ).normalized()
                    },
                    seed = lastTrack.size,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = stringResource(R.string.course_make_last, "%.2f".format(km)),
                style = MaterialTheme.typography.titleSmall,
                color = Volt,
            )
            LabeledField(
                label = stringResource(R.string.course_name_hint),
                value = name,
                onValueChange = { name = it },
            )
            LabeledField(
                label = stringResource(R.string.course_area_hint),
                value = area,
                onValueChange = { area = it },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.course_share_toggle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Snow,
                )
                Switch(
                    checked = share,
                    onCheckedChange = { share = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Night,
                        checkedTrackColor = Volt,
                        uncheckedThumbColor = Silver,
                        uncheckedTrackColor = CarbonHigh,
                    ),
                )
            }
            VoltButton(
                text = stringResource(R.string.course_register),
                onClick = { onCreate(name, area, share) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.width(1.dp))
        Text(
            text = stringResource(
                R.string.course_per_km,
                "%.0f".format(CourseRewards.SUP_PER_KM),
                "%.0f".format(CourseRewards.MAX_REWARD),
            ),
            fontSize = 10.sp,
            color = Slate,
        )
    }
}
