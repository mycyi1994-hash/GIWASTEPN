package com.giwa.strideup

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.StrideUpRoot
import com.giwa.strideup.ui.theme.StrideUpTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 딥 블랙 다크 테마 고정 — 시스템 설정과 무관하게 밝은 상태바 아이콘을 쓴다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // 이미 권한이 있으면 바로 추적 시작 (첫 요청은 StrideUpRoot에서 처리)
        if (StepPermissions.hasActivityRecognition(this)) {
            ServiceLocator.stepRepository.startTracking()
        }
        setContent {
            StrideUpTheme {
                StrideUpRoot()
            }
        }
    }
}
