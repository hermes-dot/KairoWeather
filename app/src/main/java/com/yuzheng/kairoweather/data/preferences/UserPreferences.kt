package com.yuzheng.kairoweather.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    val temperatureUnit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[temperatureUnitKey] ?: "celsius"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[themeModeKey] ?: "system"
    }

    suspend fun setTemperatureUnit(unit: String) {
        context.dataStore.edit { prefs -> prefs[temperatureUnitKey] = unit }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[themeModeKey] = mode }
    }
}
