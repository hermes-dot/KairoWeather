package com.yuzheng.kairoweather.data.location

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationTracker @Inject constructor(
    private val fusedClient: FusedLocationProviderClient,
) {
    @SuppressLint("MissingPermission") // 权限由调用方在 UI 层处理
    suspend fun getCurrentLocation(): Result<Location> =
        suspendCancellableCoroutine { continuation ->
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(Result.success(location))
                } else {
                    continuation.resume(Result.failure(Exception("无法获取位置")))
                }
            }.addOnFailureListener {
                continuation.resume(Result.failure(it))
            }
        }

    fun formatLocation(location: Location): String {
        val lat = "%.2f".format(location.latitude)
        val lon = "%.2f".format(location.longitude)
        return "$lon,$lat"
    }
}
