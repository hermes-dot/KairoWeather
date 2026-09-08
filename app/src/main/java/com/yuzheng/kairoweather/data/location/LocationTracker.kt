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

        /**
         * lastKnown 新鲜度门槛:fused 缓存的 lastLocation 可能是几小时甚至几天前的
         * 定位(初定位钉死旧城市 bug 的一半根源),超过该时长视为过期,改走主动定位。
         */
        private const val LAST_KNOWN_MAX_AGE_MS = 10 * 60 * 1000L

        /**
         * [Location.getTime] 是定位产生时刻(UTC 毫秒)。时间戳缺失(<=0)或时钟回拨
         * (age < 0)都按过期处理——宁可多花一次主动定位,不拿陈旧坐标钉死页面。
         */
        internal fun isFresh(fixTimeMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
            val age = nowMillis - fixTimeMillis
            return fixTimeMillis > 0 && age in 0..LAST_KNOWN_MAX_AGE_MS
        }
    }

    @SuppressLint("MissingPermission") // 权限由调用方在 UI 层处理
    suspend fun getCurrentLocation(): Result<Location> {
        // 优先使用最近一次已知位置(快、省电),但必须过了新鲜度门槛
        val lastKnown = resultCatching { awaitLocation(fusedClient.lastLocation) }.getOrNull()
        if (lastKnown != null && isFresh(lastKnown.time)) return Result.success(lastKnown)

        // 冷启动 lastLocation 通常为 null、或已过期(用户可能已换城):走主动定位。
        // resultCatching 会把 Task 失败转为 Result.failure,同时保证协程取消信号不被吞。
        val active = resultCatching {
            withTimeoutOrNull(ACTIVE_LOCATION_TIMEOUT_MS) {
                awaitLocation(fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null))
            } ?: throw IllegalStateException("定位超时,请重试")
        }
        if (active.isSuccess || lastKnown == null) return active
        // 主动定位失败时,陈旧定位仍好过整页回退默认城市(旧坐标至少曾是真实位置)
        return Result.success(lastKnown)
    }

    /** 两次定位间的位移(米)。上层据此判断"是否大概率换了城市"。 */
    fun distanceMeters(from: Location, to: Location): Float = from.distanceTo(to)

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
