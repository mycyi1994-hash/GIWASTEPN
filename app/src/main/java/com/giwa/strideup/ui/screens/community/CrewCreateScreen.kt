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
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HexBadge
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow

/** 모임 만들기 — 당근 그룹처럼 이름·소개·활동 지역만 받고 바로 개설한다. */
@Composable
fun CrewCreateScreen(
    onBack: () -> Unit = {},
    onCreated: (String) -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    var name by rememberSaveable { mutableStateOf("") }
    var tagline by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }

    val monogram = name.trim()
        .split(" ", "-", "_")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "ST" }

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
                Text(
                    text = stringResource(R.string.crew_create_title),
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Snow,
                )
            }
        }

        item {
            GlowCard(accent = true, contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    HexBadge(text = monogram, size = 52.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = name.ifBlank { stringResource(R.string.crew_create_preview_name) },
                            style = MaterialTheme.typography.titleMedium,
                            color = Snow,
                        )
                        Text(
                            text = tagline.ifBlank { stringResource(R.string.crew_create_preview_tag) },
                            fontSize = 11.sp,
                            color = Silver,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.crew_create_preview_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                )
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 14.dp) {
                LabeledField(
                    label = stringResource(R.string.crew_field_name),
                    value = name,
                    onValueChange = { name = it.take(28) },
                    placeholder = stringResource(R.string.crew_field_name_hint),
                )
                LabeledField(
                    label = stringResource(R.string.crew_field_tagline),
                    value = tagline,
                    onValueChange = { tagline = it.take(60) },
                    placeholder = stringResource(R.string.crew_field_tagline_hint),
                )
                LabeledField(
                    label = stringResource(R.string.crew_field_area),
                    value = area,
                    onValueChange = { area = it.take(28) },
                    placeholder = stringResource(R.string.crew_field_area_hint),
                )
            }
        }

        item {
            VoltButton(
                text = stringResource(R.string.crew_create_submit),
                enabled = name.isNotBlank(),
                onClick = { viewModel.createCrew(name, tagline, area, onCreated) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
