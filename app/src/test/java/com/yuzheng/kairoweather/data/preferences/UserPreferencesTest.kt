package com.yuzheng.kairoweather.data.preferences

import android.content.Context
import android.content.ContextWrapper
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import com.yuzheng.kairoweather.domain.model.ThemeMode
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * UserPreferences 的 DataStore 默认值/持久化测试。
 *
 * `Context.dataStore` 扩展属性在 classloader 内是单例,底层委托只在首次访问时
 * 创建一次 DataStore 并缓存实例(后续 Context 会被忽略),导致用例间内存状态残留。
 * 因此这里在每次用例前反射重置委托缓存的 DataStore 实例,并让新实例指向独立
 * 临时目录,保证每个用例都从空 DataStore 开始、互不污染。
 */
@RunWith(RobolectricTestRunner::class)
class UserPreferencesTest {

    private lateinit var context: Context
    private lateinit var preferences: UserPreferences

    @Before
    fun setUp() {
        val dataDir = Files.createTempDirectory("kairo-datastore").toFile()
        resetDataStoreSingleton()
        context = IsolatedContext(RuntimeEnvironment.getApplication(), dataDir)
        preferences = UserPreferences(context)
    }

    @After
    fun tearDown() {
        context.filesDir.deleteRecursively()
    }

    @Test
    fun `defaults to celsius when no value stored`(): Unit = runBlocking {
        assertEquals(TemperatureUnit.CELSIUS, preferences.temperatureUnit.first())
    }

    @Test
    fun `defaults to system theme when no value stored`(): Unit = runBlocking {
        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
    }

    @Test
    fun `setTemperatureUnit persists and is read back`(): Unit = runBlocking {
        assertEquals(TemperatureUnit.CELSIUS, preferences.temperatureUnit.first())

        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)

        assertEquals(TemperatureUnit.FAHRENHEIT, preferences.temperatureUnit.first())
        assertTrue(prefsFile().exists())
    }

    @Test
    fun `setThemeMode persists and is read back`(): Unit = runBlocking {
        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())

        preferences.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, preferences.themeMode.first())
        assertTrue(prefsFile().exists())
    }

    @Test
    fun `theme mode flow emits DARK to already-active collector after write`(): Unit = runBlocking {
        val emissions = Channel<ThemeMode>(capacity = Channel.UNLIMITED)
        val job = launch {
            preferences.themeMode.collect { emissions.send(it) }
        }

        // 先确认持续订阅的收集器已拿到 DataStore 当前值(SYSTEM 默认)
        assertEquals(ThemeMode.SYSTEM, emissions.receive())

        preferences.setThemeMode(ThemeMode.DARK)

        // 回归:持续 collect 的收集器必须收到后续切换值;一次性 first() 订阅只会停在首值
        assertEquals(ThemeMode.DARK, emissions.receive())
        job.cancel()
    }

    @Test
    fun `temperature unit and theme mode are independent`(): Unit = runBlocking {
        preferences.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        preferences.setThemeMode(ThemeMode.LIGHT)

        assertEquals(TemperatureUnit.FAHRENHEIT, preferences.temperatureUnit.first())
        assertEquals(ThemeMode.LIGHT, preferences.themeMode.first())
    }

    private fun prefsFile(): File =
        context.filesDir.resolve("datastore/settings.preferences_pb")

    /** 把顶层委托缓存的数据存储实例清空,让下一次访问按当前 Context 重新创建。 */
    private fun resetDataStoreSingleton() {
        val ktClass = Class.forName("com.yuzheng.kairoweather.data.preferences.UserPreferencesKt")
        val delegateField = ktClass.getDeclaredField("dataStore\$delegate")
        delegateField.isAccessible = true
        val delegate = delegateField.get(null)
        val instanceField = delegate.javaClass.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(delegate, null)
    }

    /** 让 DataStore 委托指向每个用例独立的 filesDir,避免单例状态跨用例残留。 */
    private class IsolatedContext(
        base: Context,
        private val isolatedFilesDir: File,
    ) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = isolatedFilesDir
    }
}
