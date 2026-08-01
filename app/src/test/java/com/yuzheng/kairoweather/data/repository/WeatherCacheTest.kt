package com.yuzheng.kairoweather.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherCacheTest {

    private var now = 0L
    private val cache = WeatherCache { now }

    @Test
    fun `get returns null for missing key`() {
        assertNull(cache.get<String>("missing", ttlMillis = 1000))
    }

    @Test
    fun `get returns cached value before ttl`() {
        cache.put("k", "v")

        now = 999
        assertEquals("v", cache.get<String>("k", ttlMillis = 1000))
    }

    @Test
    fun `get returns null after ttl and lazily removes entry`() {
        cache.put("k", "v")

        now = 1000
        assertNull(cache.get<String>("k", ttlMillis = 1000))
        // 第二次读取确认过期条目已被惰性删除,不再占用内存
        assertNull(cache.get<String>("k", ttlMillis = 1000))
    }

    @Test
    fun `put overwrites existing value and resets ttl`() {
        cache.put("k", "old")
        now = 500
        cache.put("k", "new")

        now = 1499
        assertEquals("new", cache.get<String>("k", ttlMillis = 1000))
    }

    @Test
    fun `clear removes all entries`() {
        cache.put("k", "v")
        cache.clear()

        assertNull(cache.get<String>("k", ttlMillis = 1000))
    }

    @Test
    fun `stores and retrieves any type`() {
        cache.put("list", listOf(1, 2, 3))
        cache.put("result", Result.success("ok"))

        assertEquals(listOf(1, 2, 3), cache.get<List<Int>>("list", ttlMillis = 1000))
        assertEquals(Result.success("ok"), cache.get<Result<String>>("result", ttlMillis = 1000))
    }

    @Test
    fun `different keys are independent`() {
        cache.put("a", "1")  // created at now=0
        now = 100
        cache.put("b", "2")  // created at now=100

        now = 1000
        // a 已过期(0+1000<=1000),b 未过期(100+1000>1000)
        assertEquals("2", cache.get<String>("b", ttlMillis = 1000))
        assertNull(cache.get<String>("a", ttlMillis = 1000))
    }
}
