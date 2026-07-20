package com.giwa.strideup

import android.app.Application
import com.giwa.strideup.core.ServiceLocator

class StrideUpApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
