package com.stepup.android.ui.screens.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.GoogleIdResult
import com.stepup.android.data.remote.TokenResult
import com.stepup.android.ui.components.HexEmblem
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.Night
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import kotlinx.coroutines.launch

/**
 * 첫 진입 로그인.
 *
 * 로그인 수단은 구글 하나다. 기록이 서버에 쌓이려면 누구의 기록인지가 정해져야
 * 하고, 그걸 정할 방법이 이것뿐이다.
 *
 * 로그인에 성공하기 전에는 이 화면을 벗어날 수 없다. 예전에는 "게스트로
 * 둘러보기"가 있었지만, 게스트가 쌓은 기록은 기기를 바꾸는 순간 사라진다 —
 * 그 상태로 몇 달 모은 뒤에 잃는 것보다 지금 한 번 탭하는 편이 낫다.
 */
@Composable
fun LoginScreen(onDone: () -> Unit) {
    var signingIn by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun signIn() {
        if (signingIn) return
        val activity = context.findActivity()
        if (activity == null) {
            // 계정 선택 창을 띄울 화면이 없다. 정상 실행에서는 일어나지 않는다.
            error = R.string.login_failed
            return
        }

        signingIn = true
        error = null
        scope.launch {
            try {
                when (val id = ServiceLocator.googleSignIn.requestIdToken(activity)) {
                    is GoogleIdResult.Ok -> {
                        when (val session =
                            ServiceLocator.sessionHolder.signInWithGoogle(id.idToken, id.nonce)) {
                            is TokenResult.Ok -> {
                                ServiceLocator.userPrefs.setLoginMethod("google")
                                onDone()
                            }
                            // 서버가 토큰을 거절했다. 대개 설정 문제다 —
                            // 클라이언트 ID 가 서버에 등록되지 않은 경우가 가장 흔하다.
                            is TokenResult.SignInRequired -> error = R.string.login_failed
                            is TokenResult.Unavailable -> error = R.string.login_offline
                        }
                    }

                    // 사용자가 창을 닫았다. 본인이 한 일이므로 오류를 띄우지 않는다.
                    GoogleIdResult.Cancelled -> Unit

                    is GoogleIdResult.NoAccount -> error = R.string.login_no_account
                    is GoogleIdResult.Failed -> error = R.string.login_failed
                }
            } finally {
                // 성공하면 화면이 바뀌지만, 그 사이 무엇이 터져도 버튼이
                // 영원히 도는 상태로 남으면 안 된다.
                signingIn = false
            }
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .quietClickable { signIn() }
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
                        if (signingIn) R.string.login_google_progress else R.string.login_google,
                    ),
                    color = Color(0xFF1F1F1F),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(14.dp))

            // 실패했으면 이유를 보여준다. 아무 말 없이 제자리면 사용자는
            // 버튼이 고장 난 줄 안다.
            error?.let { message ->
                Text(
                    text = stringResource(message),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
            }

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

/**
 * 계정 선택 창을 띄우려면 화면(Activity)이 필요하다.
 *
 * Compose 의 LocalContext 가 늘 Activity 인 것은 아니다 — 테마를 감싸는
 * 래퍼가 끼면 ContextWrapper 가 온다. 벗겨 내며 찾는다.
 */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** 구글 'G' 글리프 */
@Composable
private fun GoogleGlyph() {
    Row {
        Text("G", color = Color(0xFF4285F4), fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}
