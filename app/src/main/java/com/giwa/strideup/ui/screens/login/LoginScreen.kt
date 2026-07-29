package com.giwa.strideup.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.giwa.strideup.R
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.components.HexEmblem
import com.giwa.strideup.ui.components.Wordmark
import com.giwa.strideup.ui.components.quietClickable
import com.giwa.strideup.ui.theme.Night
import com.giwa.strideup.ui.theme.Silver
import com.giwa.strideup.ui.theme.Slate
import com.giwa.strideup.ui.theme.Snow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 첫 진입 로그인.
 *
 * 구글 로그인 버튼과, 그 아래 작은 회색 "게스트 모드"를 둔다.
 * 아직 백엔드가 없어 구글 로그인은 데모 흐름(잠시 후 완료)으로 처리하고,
 * 선택 결과만 저장해 다음 실행부터는 이 화면을 건너뛴다.
 */
@Composable
fun LoginScreen(onDone: () -> Unit) {
    var signingIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 구글 로그인 데모 흐름 — 스피너를 잠깐 보여주고 완료 처리
    LaunchedEffect(signingIn) {
        if (signingIn) {
            delay(1400)
            ServiceLocator.userPrefs.setLoginMethod("google")
            onDone()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.9f))

            HexEmblem(size = 74.dp)
            Spacer(Modifier.height(18.dp))
            Wordmark(fontSize = 42.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Silver,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = stringResource(R.string.login_headline),
                style = MaterialTheme.typography.titleMedium,
                color = Snow,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))

            // 구글 로그인 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .quietClickable { if (!signingIn) signingIn = true }
                    .padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (signingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF4285F4),
                        strokeWidth = 2.4.dp,
                    )
                } else {
                    GoogleGlyph()
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(
                        if (signingIn) R.string.login_google_progress else R.string.login_google
                    ),
                    color = Color(0xFF1F1F1F),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))

            // 게스트 모드 — 작고 회색으로
            Text(
                text = stringResource(R.string.login_guest),
                modifier = Modifier
                    .quietClickable {
                        if (!signingIn) {
                            scope.launch {
                                ServiceLocator.userPrefs.setLoginMethod("guest")
                                onDone()
                            }
                        }
                    }
                    .padding(8.dp),
                fontSize = 12.sp,
                color = Slate,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.login_terms),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = Slate.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 구글 'G' 글리프 */
@Composable
private fun GoogleGlyph() {
    Row {
        Text("G", color = Color(0xFF4285F4), fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}
