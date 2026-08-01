package com.yuzheng.kairoweather.data.repository

import java.util.concurrent.ConcurrentHashMap

/**
 * 内存 TTL 缓存(线程安全)。
 *
 * - 只缓存 [Result.isSuccess] 的成功结果,失败结果不写入,避免错误长时间滞留;
 * - 用 [ConcurrentHashMap] 保证 get/put 的并发可见性,命中过期条目时惰性删除(用
 *   [ConcurrentHashMap.remove] 条件删除,避免并发写入时误删新条目);
 * - 同 key 并发未命中时会各自回源,属可接受的良性竞态——避免加锁把不同 key 的请求串行化。
 *
 * [nowMillis] 允许注入时钟,便于测试验证过期逻辑;默认使用系统墙钟。
 */
internal class WeatherCache(
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private class Entry(val value: Any, val createdAtMillis: Long)

    private val map = ConcurrentHashMap<String, Entry>()

    /** 命中且未过期返回缓存值;未命中或已过期返回 null。 */
    fun <T> get(key: String, ttlMillis: Long): T? {
        val entry = map[key] ?: return null
        if (nowMillis() - entry.createdAtMillis >= ttlMillis) {
            map.remove(key, entry)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return entry.value as T
    }

    fun <T : Any> put(key: String, value: T) {
        map[key] = Entry(value, nowMillis())
    }

    fun clear() = map.clear()
}
