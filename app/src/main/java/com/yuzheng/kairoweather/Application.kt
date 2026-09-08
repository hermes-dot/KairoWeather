package com.yuzheng.kairoweather

import android.app.Application
import com.yuzheng.kairoweather.data.PerfLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DefaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PerfLog.init(this) // 临时诊断(ColorOS 压 logcat),排查完随 PerfLog 一起删
    }
}