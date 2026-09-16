package com.stepup.android

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stepup.android.core.AppLocale
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.StepUpRoot
import com.stepup.android.ui.theme.StepUpTheme

class MainActivity : ComponentActivity() {

    /** 선택한 언어로 리소스를 읽도록 액티비티 컨텍스트를 감싼다 (API 33 미만) */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 13+에서는 OS가 앱 언어를 기억하므로 그 값을 정본으로 삼는다.
        AppLocale.syncFromSystem(this)
        // 딥 블랙 다크 테마 고정 — 시스템 설정과 무관하게 밝은 상태바 아이콘을 쓴다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // 이미 권한이 있으면 바로 추적 시작 (첫 요청은 StepUpRoot에서 처리)
        if (StepPermissions.hasActivityRecognition(this)) {
            ServiceLocator.stepRepository.startTracking()
        }
        setContent {
            StepUpTheme {
                StepUpRoot()
            }
        }
    }
}
