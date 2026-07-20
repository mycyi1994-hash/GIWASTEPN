package com.giwa.strideup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.giwa.strideup.core.ServiceLocator
import com.giwa.strideup.ui.StepPermissions
import com.giwa.strideup.ui.StrideUpRoot
import com.giwa.strideup.ui.theme.StrideUpTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
