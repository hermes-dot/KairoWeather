package com.yuzheng.kairoweather.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yuzheng.kairoweather.domain.model.TemperatureUnit
import com.yuzheng.kairoweather.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val temperatureUnitKey = stringPreferencesKey("temperature_unit")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val temperatureUnit: Flow<TemperatureUnit> = context.dataStore.data.map { prefs ->
        // P2-C: 读取时用枚举解析(兼容旧版本小写字符串),无法识别回退默认值
        TemperatureUnit.fromStored(prefs[temperatureUnitKey])
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromStored(prefs[themeModeKey])
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { prefs -> prefs[temperatureUnitKey] = unit.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }
}
