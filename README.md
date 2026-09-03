# 天气通 (KairoWeather)

基于和风天气 API 的 Android 天气应用,全量 Jetpack Compose 开发。

## 功能
- 实时天气、24 小时逐小时预报、未来 7 日趋势
- 日出日落轨迹卡片(Compose Canvas + Path 自绘)
- 城市切换、温度单位、主题设置(偏好持久化)

## 技术栈
Jetpack Compose + Material3 · MVVM/MVI(StateFlow 单一 UiState + update 原子更新) · Hilt · Retrofit + OkHttp + kotlinx.serialization · DataStore Preferences · MockK + Robolectric 单元测试(68 用例) · KSP · R8

## 模块结构
- `ui/` Compose 界面与组件(`collectAsStateWithLifecycle` 收集 StateFlow)
- `domain/` 领域模型与格式化
- `data/repository/` Repository + 15 分钟 TTL 线程安全内存缓存 + 错误包装
- `data/network/` Retrofit 接口与序列化
- `data/location/` FusedLocationProvider 协程化封装(`suspendCancellableCoroutine` + 超时兜底)
- `di/` Hilt 装配

## 构建
1. JDK 17+,Android Studio 或命令行 Gradle
2. 项目根目录新建 `qweather.properties`(已被 .gitignore 忽略,勿提交):
   ```properties
   apiKey=你的和风天气API Key
   baseUrl=https://devapi.qweather.com/v7/
   ```
   Key 在 https://console.qweather.com 免费申请
3. `gradlew assembleDebug`

> 定位依赖 Google Play Services,需带 GMS 的设备/模拟器;无定位权限时自动回退默认城市。

本项目采用「人主导架构、AI 辅助编码」方式:架构与策略自主设计,AI 辅助实现,全部产出经人工逐行审查与单测/真机验证后合入。