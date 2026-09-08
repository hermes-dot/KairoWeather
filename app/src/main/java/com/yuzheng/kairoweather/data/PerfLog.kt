package com.yuzheng.kairoweather.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 临时诊断通道(首进耗时排查,2026-09-08):本机 ColorOS 全局压制 logcat
 * (persist.sys.log.ctrl=no),adb 侧拿不到应用日志,改写入 app 私有文件,
 * 用 `adb exec-out run-as <pkg> cat files/perf.log` 读回。排查完即删。
 */
object PerfLog {
    private var file: File? = null
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        file = File(context.filesDir, "perf.log").apply { delete() }
    }

    @Synchronized
    fun log(msg: String) {
        runCatching {
            file?.appendText("${timeFmt.format(Date())} $msg\n")
        }
    }
}
