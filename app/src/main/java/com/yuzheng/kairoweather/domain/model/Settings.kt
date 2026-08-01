package com.yuzheng.kairoweather.domain.model

/**
 * 温度单位枚举。
 *
 * - [apiValue] 是传给 QWeather API 的单位参数值(小写),与旧版本持久化的字符串一致;
 * - [name]("CELSIUS"/"FAHRENHEIT")用于 DataStore 存储。
 */
enum class TemperatureUnit(val apiValue: String) {
    CELSIUS("celsius"),
    FAHRENHEIT("fahrenheit");

    companion object {
        /** 从 DataStore 原始字符串解析,兼容旧版本小写写法;无法识别时回退默认值。 */
        fun fromStored(value: String?): TemperatureUnit = when (value) {
            "celsius", CELSIUS.name -> CELSIUS
            "fahrenheit", FAHRENHEIT.name -> FAHRENHEIT
            else -> CELSIUS
        }
    }
}

/** 主题模式枚举,与 DataStore 存储值一一对应(存储用 [name])。 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** 从 DataStore 原始字符串解析,兼容旧版本小写写法;无法识别时回退默认值。 */
        fun fromStored(value: String?): ThemeMode = when (value) {
            "system", SYSTEM.name -> SYSTEM
            "light", LIGHT.name -> LIGHT
            "dark", DARK.name -> DARK
            else -> SYSTEM
        }
    }
}
