package com.giwa.strideup

import android.app.Application
import com.giwa.strideup.core.AppLocale
import com.giwa.strideup.core.ServiceLocator
import kotlinx.coroutines.runBlocking

class StrideUpApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        // 첫 화면이 그려지기 전에 언어를 확정해야 리소스가 한 번에 맞는 언어로 읽힌다.
        // DataStore 한 건 읽기라 시작 시간에 미치는 영향은 미미하다.
        val saved = runBlocking { ServiceLocator.userPrefs.languageNow() }
        AppLocale.bootstrap(this, saved)
    }
}
