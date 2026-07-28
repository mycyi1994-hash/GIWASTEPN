package com.giwa.strideup.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.HairlineDivider
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.theme.Alert
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import kotlinx.coroutines.launch

/**
 * 개인정보 · 보안 — 저장 데이터 안내, 사용 중인 권한, 로컬 데이터 초기화.
 *
 * 모든 데이터는 기기 로컬에만 머문다. 초기화 버튼은 알림함만 비운다
 * (걸음 · SUP · 스니커는 유지).
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resetDoneMessage = stringResource(R.string.privacy_reset_done)

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
                        text = stringResource(R.string.settings_privacy),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                }
            }
        }

        item {
            GlowCard(spacing = 12.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    IconSquare(icon = Icons.Filled.Shield, size = 38.dp)
                    Text(
                        text = stringResource(R.string.privacy_data_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                }
                Text(
                    text = stringResource(R.string.privacy_data_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
            }
        }

        item {
            GlowCard(spacing = 12.dp) {
                Text(
                    text = stringResource(R.string.privacy_perm_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                PermissionRow(text = stringResource(R.string.privacy_perm_activity))
                HairlineDivider()
                PermissionRow(text = stringResource(R.string.privacy_perm_notification))
            }
        }

        item {
            GlowCard(spacing = 12.dp) {
                Text(
                    text = stringResource(R.string.privacy_reset_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Snow,
                )
                Text(
                    text = stringResource(R.string.privacy_reset_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Silver,
                )
                GhostButton(
                    text = stringResource(R.string.privacy_reset_button),
                    onClick = {
                        scope.launch {
                            ServiceLocator.notificationRepository.clear()
                            Toast.makeText(context, resetDoneMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                    accent = Alert,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 볼트 체크 + 권한 설명 한 줄. */
@Composable
private fun PermissionRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Volt,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Silver,
        )
    }
}
