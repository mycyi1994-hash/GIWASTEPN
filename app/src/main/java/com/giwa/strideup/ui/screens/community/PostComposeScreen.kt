package com.giwa.strideup.ui.screens.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giwa.strideup.R
import com.giwa.strideup.domain.PostCategory
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.PillChip
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow

/**
 * 글쓰기.
 *
 * 번개러닝을 고르면 장소·거리·출발까지 남은 시간·정원 입력이 함께 열린다.
 * crewId가 비어 있으면 전체 게시판에, 값이 있으면 그 크루 게시판에 올라간다.
 */
@Composable
fun PostComposeScreen(
    crewId: String = "",
    crewName: String = "",
    onBack: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    var category by rememberSaveable { mutableStateOf(PostCategory.FLASH.id) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var distance by rememberSaveable { mutableStateOf("1.0") }
    var startsIn by rememberSaveable { mutableStateOf("60") }
    var capacity by rememberSaveable { mutableStateOf("6") }

    val selected = PostCategory.of(category)
    val author = stringResource(R.string.rank_me)
    val canSubmit = title.isNotBlank() &&
        (selected != PostCategory.FLASH || place.isNotBlank())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
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
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.post_write),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                    Text(
                        text = crewName.ifBlank { stringResource(R.string.community_seg_board) },
                        fontSize = 11.sp,
                        color = Slate,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PostCategory.entries.forEach { entry ->
                    PillChip(
                        text = entry.label(),
                        selected = selected == entry,
                        onClick = { category = entry.id },
                    )
                }
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 14.dp) {
                LabeledField(
                    label = stringResource(R.string.post_field_title),
                    value = title,
                    onValueChange = { title = it },
                    placeholder = stringResource(R.string.post_field_title_hint),
                )
                LabeledField(
                    label = stringResource(R.string.post_field_body),
                    value = body,
                    onValueChange = { body = it },
                    placeholder = stringResource(R.string.post_field_body_hint),
                    singleLine = false,
                    minHeight = 92,
                )
            }
        }

        if (selected == PostCategory.FLASH) {
            item {
                GlowCard(contentPadding = PaddingValues(16.dp), spacing = 14.dp) {
                    Text(
                        text = stringResource(R.string.post_flash_details),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                    LabeledField(
                        label = stringResource(R.string.post_field_place),
                        value = place,
                        onValueChange = { place = it },
                        placeholder = stringResource(R.string.post_field_place_hint),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledField(
                            label = stringResource(R.string.post_field_distance),
                            value = distance,
                            onValueChange = { distance = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier.weight(1f),
                            placeholder = "1.0",
                        )
                        LabeledField(
                            label = stringResource(R.string.post_field_starts_in),
                            value = startsIn,
                            onValueChange = { startsIn = it.filter(Char::isDigit).take(4) },
                            modifier = Modifier.weight(1f),
                            placeholder = "60",
                        )
                        LabeledField(
                            label = stringResource(R.string.post_field_capacity),
                            value = capacity,
                            onValueChange = { capacity = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.weight(1f),
                            placeholder = "6",
                        )
                    }
                    Text(
                        text = stringResource(R.string.post_flash_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                }
            }
        }

        item {
            VoltButton(
                text = stringResource(R.string.post_submit),
                enabled = canSubmit,
                onClick = {
                    viewModel.writePost(
                        category = selected,
                        title = title,
                        body = body,
                        author = author,
                        crewId = crewId,
                        place = place,
                        distanceKm = distance.toDoubleOrNull() ?: 0.0,
                        meetInMinutes = startsIn.toIntOrNull() ?: 60,
                        capacity = capacity.toIntOrNull() ?: 6,
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
