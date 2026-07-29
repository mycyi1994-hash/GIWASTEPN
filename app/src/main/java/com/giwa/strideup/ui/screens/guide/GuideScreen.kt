package com.giwa.strideup.ui.screens.guide

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.components.GhostButton
import com.giwa.strideup.ui.components.GlowCard
import com.giwa.strideup.ui.components.IconSquare
import com.giwa.strideup.ui.components.VoltButton
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import com.giwa.strideup.ui.theme.Volt
import kotlinx.coroutines.launch

/**
 * 초보자용 앱 가이드 — 검정 배경 위에 카드(창)가 뜨고 좌우로 넘긴다.
 * 탭별 안내 총 7장. 마지막 장에서 "시작하기".
 */
private data class GuidePage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val tipRes: Int,
)

private val PAGES = listOf(
    GuidePage(Icons.Filled.Hexagon, R.string.guide1_title, R.string.guide1_body, R.string.guide1_tip),
    GuidePage(Icons.AutoMirrored.Filled.DirectionsWalk, R.string.guide2_title, R.string.guide2_body, R.string.guide2_tip),
    GuidePage(Icons.Filled.AutoAwesome, R.string.guide3_title, R.string.guide3_body, R.string.guide3_tip),
    GuidePage(Icons.Filled.Forum, R.string.guide4_title, R.string.guide4_body, R.string.guide4_tip),
    GuidePage(Icons.Filled.Groups, R.string.guide5_title, R.string.guide5_body, R.string.guide5_tip),
    GuidePage(Icons.Filled.EmojiEvents, R.string.guide6_title, R.string.guide6_body, R.string.guide6_tip),
    GuidePage(Icons.Filled.Person, R.string.guide7_title, R.string.guide7_body, R.string.guide7_tip),
)

@Composable
fun GuideScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()

    fun finish() {
        scope.launch {
            ServiceLocator.userPrefs.setGuideSeen()
            onDone()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Night)
            .padding(vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark(fontSize = 20.sp)

        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.guide_heading),
            style = MaterialTheme.typography.titleMedium,
            color = Silver,
        )

        // 카드(창) 페이저
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 30.dp, vertical = 22.dp),
            pageSpacing = 14.dp,
        ) { index ->
            val page = PAGES[index]
            GlowCard(
                modifier = Modifier.fillMaxSize(),
                accent = true,
                contentPadding = PaddingValues(26.dp),
                spacing = 16.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    IconSquare(icon = page.icon, size = 66.dp)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "${index + 1} / ${PAGES.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Volt,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Snow,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(13.dp))
                    Text(
                        text = stringResource(page.bodyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = stringResource(page.tipRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = Volt,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // 페이지 점
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(PAGES.size) { i ->
                val active = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .size(if (active) 8.dp else 5.dp)
                        .background(if (active) Volt else Slate.copy(alpha = 0.5f), CircleShape),
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (pagerState.currentPage < PAGES.size - 1) {
                GhostButton(
                    text = stringResource(R.string.guide_skip),
                    onClick = { finish() },
                    accent = Silver,
                )
                VoltButton(
                    text = stringResource(R.string.guide_next),
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                VoltButton(
                    text = stringResource(R.string.guide_start),
                    onClick = { finish() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
