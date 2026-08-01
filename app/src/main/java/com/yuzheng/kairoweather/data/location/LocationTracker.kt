package com.yuzheng.kairoweather.data.location

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.yuzheng.kairoweather.data.resultCatching
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class LocationTracker @Inject constructor(
    private val fusedClient: FusedLocationProviderClient,
) {
    companion object {
        /** 主动定位超时时间,防止冷启动时无限转圈 */
        private const val ACTIVE_LOCATION_TIMEOUT_MS = 15_000L
    }

    @SuppressLint("MissingPermission") // 权限由调用方在 UI 层处理
    suspend fun getCurrentLocation(): Result<Location> {
        // 优先使用最近一次已知位置(快、省电)
        val lastKnown = resultCatching { awaitLocation(fusedClient.lastLocation) }.getOrNull()
        if (lastKnown != null) return Result.success(lastKnown)

        // 冷启动时 lastLocation 通常为 null,改用主动定位;15 秒超时或失败则回退失败
        // (resultCatching 会把 Task 失败转为 Result.failure,同时保证协程取消信号不被吞)
        return resultCatching {
            withTimeoutOrNull(ACTIVE_LOCATION_TIMEOUT_MS) {
                awaitLocation(fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null))
            } ?: throw IllegalStateException("定位超时,请重试")
        }
    }

    private suspend fun awaitLocation(task: Task<Location>): Location? =
        suspendCancellableCoroutine { continuation ->
            var cancelled = false
            continuation.invokeOnCancellation { cancelled = true }
            // Task API 没有 removeOnXxxListener 公开方法,用 OnCompleteListener 统一处理:
            // 协程取消后置 cancelled 标志,回调不再 resume,监听器随 Task 完成由内部释放。
            task.addOnCompleteListener { t ->
                if (cancelled || !continuation.isActive) return@addOnCompleteListener
                if (t.isSuccessful) {
                    continuation.resume(t.result)
                } else {
                    continuation.resumeWithException(t.exception ?: Exception("定位失败"))
                }
            }
        }

    fun formatLocation(location: Location): String {
        // 显式指定 Locale.US,避免德/俄等地区用逗号作为小数分隔符导致坐标解析失败
        val lat = String.format(Locale.US, "%.2f", location.latitude)
        val lon = String.format(Locale.US, "%.2f", location.longitude)
        return "$lon,$lat"
    }
}
