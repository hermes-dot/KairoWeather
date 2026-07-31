package com.yuzheng.kairoweather.data.location

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationTracker @Inject constructor(
    private val fusedClient: FusedLocationProviderClient,
) {
    @SuppressLint("MissingPermission") // 权限由调用方在 UI 层处理
    suspend fun getCurrentLocation(): Result<Location> {
        // 优先使用最近一次已知位置（快、省电）
        val lastKnown = runCatching { awaitLocation(fusedClient.lastLocation) }.getOrNull()
        if (lastKnown != null) return Result.success(lastKnown)

        // 冷启动时 lastLocation 通常为 null，改用主动定位
        return runCatching {
            awaitLocation(fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null))
                ?: throw IllegalStateException("无法获取位置")
        }
    }

    private suspend fun awaitLocation(task: Task<Location>): Location? =
        suspendCancellableCoroutine { continuation ->
            task.addOnSuccessListener { location ->
                if (continuation.isActive) continuation.resume(location)
            }.addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resumeWithException(exception)
            }
        }

    fun formatLocation(location: Location): String {
        val lat = "%.2f".format(location.latitude)
        val lon = "%.2f".format(location.longitude)
        return "$lon,$lat"
    }
}
