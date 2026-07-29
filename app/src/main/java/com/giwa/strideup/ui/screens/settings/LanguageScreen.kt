package com.giwa.strideup.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.core.AppLocale
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.components.DarkIconButton
import com.giwa.strideup.ui.components.Eyebrow
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.CarbonHigh
import com.giwa.strideup.ui.theme.Edge
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 고를 수 있는 언어 — 이름은 그 언어 자체로 적어 어느 언어에서도 알아볼 수 있게 한다 */
private data class LanguageOption(val tag: String, val nativeName: String, val labelRes: Int)

private val OPTIONS = listOf(
    LanguageOption(AppLocale.SYSTEM, "", R.string.language_system),
    LanguageOption("en", "English", R.string.language_en),
    LanguageOption("ko", "한국어", R.string.language_ko),
    LanguageOption("zh", "中文", R.string.language_zh),
    LanguageOption("ja", "日本語", R.string.language_ja),
)

/**
 * 언어 설정.
 *
 * 고르는 즉시 저장하고 화면을 새 언어로 다시 그린다.
 * Android 13 이상에서는 OS의 앱별 언어 설정에도 그대로 반영된다.
 */
@Composable
fun LanguageScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    var selected by remember { mutableStateOf(AppLocale.tag) }

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
                Column {
                    Eyebrow(text = stringResource(R.string.profile_account))
                    Text(
                        text = stringResource(R.string.settings_language),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                }
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 11.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconSquare(icon = Icons.Filled.Language, size = 38.dp, tint = Volt)
                    Text(
                        text = stringResource(R.string.language_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                        lineHeight = 18.sp,
                    )
                }
            }
        }

        items(OPTIONS.size) { index ->
            val option = OPTIONS[index]
            LanguageRow(
                option = option,
                checked = option.tag == selected,
                onClick = {
                    if (option.tag != selected) {
                        selected = option.tag
                        // 화면이 곧 재생성되므로 저장은 화면 수명과 무관한 스코프에서 한다
                        CoroutineScope(Dispatchers.IO).launch {
                            ServiceLocator.userPrefs.setLanguage(option.tag)
                        }
                        if (AppLocale.change(context, option.tag)) activity?.recreate()
                    }
                },
            )
        }
    }
}

@Composable
private fun LanguageRow(
    option: LanguageOption,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(option.labelRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CarbonHigh)
            .border(
                width = 1.dp,
                color = if (checked) Volt.copy(alpha = 0.6f) else Edge,
                shape = RoundedCornerShape(18.dp),
            )
            .quietClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (checked) Volt else Snow,
            )
            if (option.nativeName.isNotEmpty() && option.nativeName != label) {
                Text(text = option.nativeName, fontSize = 11.sp, color = Slate)
            }
        }
        if (checked) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Volt),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Night,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
