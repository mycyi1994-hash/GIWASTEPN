package com.giwa.strideup.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt

/**
 * 알림 설정 — 네 개의 토글.
 *
 * 백엔드가 없으므로 상태는 화면 로컬(rememberSaveable)에만 머문다.
 * 스위치를 뒤집을 때마다 "저장됨" 토스트로 즉각적인 피드백만 준다.
 */
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.pref_saved)
    val notifySaved = { Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show() }

    var push by rememberSaveable { mutableStateOf(true) }
    var goalReminder by rememberSaveable { mutableStateOf(true) }
    var partyInvite by rememberSaveable { mutableStateOf(true) }
    var eventNews by rememberSaveable { mutableStateOf(true) }

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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Eyebrow(text = stringResource(R.string.profile_account))
                    Text(
                        text = stringResource(R.string.settings_notifications),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                }
            }
        }

        item {
            ToggleRow(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.pref_push),
                description = stringResource(R.string.pref_push_desc),
                checked = push,
                onCheckedChange = {
                    push = it
                    notifySaved()
                },
            )
        }

        item {
            ToggleRow(
                icon = Icons.Filled.Schedule,
                title = stringResource(R.string.pref_goal_reminder),
                description = stringResource(R.string.pref_goal_reminder_desc),
                checked = goalReminder,
                onCheckedChange = {
                    goalReminder = it
                    notifySaved()
                },
            )
        }

        item {
            ToggleRow(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = stringResource(R.string.pref_party_invite),
                description = stringResource(R.string.pref_party_invite_desc),
                checked = partyInvite,
                onCheckedChange = {
                    partyInvite = it
                    notifySaved()
                },
            )
        }

        item {
            ToggleRow(
                icon = Icons.Filled.EmojiEvents,
                title = stringResource(R.string.pref_event_news),
                description = stringResource(R.string.pref_event_news_desc),
                checked = eventNews,
                onCheckedChange = {
                    eventNews = it
                    notifySaved()
                },
            )
        }
    }
}

/** 아이콘 · 제목 · 설명 + 볼트 스위치 한 줄. */
@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    GlowCard(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
        spacing = 0.dp,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            IconSquare(icon = icon, size = 38.dp, tint = if (checked) Volt else Slate)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Snow,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Night,
                    checkedTrackColor = Volt,
                    uncheckedThumbColor = Slate,
                    uncheckedTrackColor = CarbonHigh,
                    checkedBorderColor = Volt,
                    uncheckedBorderColor = Edge,
                ),
            )
        }
    }
}
