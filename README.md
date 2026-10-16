# 天气通 (KairoWeather)

[![CI](https://github.com/hermes-dot/KairoWeather/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/hermes-dot/KairoWeather/actions/workflows/ci.yml)

基于和风天气 API 的 Android 天气应用，全量 Jetpack Compose 开发，零 View 体系。

## 功能

- 实时天气、24 小时逐小时预报、未来 7 日趋势（温度区间条可视化）
- 空气质量卡：AQI（中国国标）+ 等级着色刻度条 + 分人群健康建议
- 日出日落轨迹卡片（Compose Canvas + Path 自绘）
- 定位跟随选城：lastKnown 秒开渲染 + 新鲜度门槛 + 回前台/下拉刷新换城自动复查
- 温度单位（℃/℉）与主题切换（偏好经 DataStore Preferences 持久化）
- 多色天气符号系统（26 枚矢量图标，见下方「图标来源」）

> 多城市管理与手动搜索选城在规划中（见 `tasks/`）。

## 技术栈

Jetpack Compose + Material3 · sealed 页面状态 + StateFlow（`collectAsStateWithLifecycle`） · Hilt · Retrofit + OkHttp + kotlinx.serialization · FusedLocationProvider · DataStore Preferences · MockK + Robolectric + 协程测试 · KSP · R8

## 测试与 CI

单元测试全部跑在 JVM 上（JUnit4 + MockK + Robolectric + kotlinx-coroutines-test），CI 在每次 push / PR 时自动执行：

- **纯函数层**：天气码 → 图标映射（逐一对照和风官方码表）、DTO → domain 数值解析与非法值回退、温度单位格式化边界
- **数据层**：结构化错误映射、15 分钟 TTL 缓存语义、协程取消不被吞；DataStore 默认值与持久化（Robolectric 跑真实 DataStore）
- **状态层**：页面 sealed 状态流转、加载防重入、下拉刷新最小指示时长、跨时区 `isNow` 判定
- **定位层**：系统网络通道 + GMS 兜底的双通道策略、lastKnown 新鲜度门槛、与设备 locale 无关的坐标格式化

- 本地执行：`./gradlew testDebugUnitTest`（Windows：`.\gradlew testDebugUnitTest`）；
  报告输出在 `app/build/reports/tests/testDebugUnitTest/index.html`
- CI：流水线跑的就是同一条命令（`.github/workflows/ci.yml`），并归档测试报告——失败时才是唯一需要看报告的时刻

## 代码结构

单模块（`settings.gradle.kts` 仅 `include(":app")`），按职责分包：

- `ui/` Compose 界面与组件，页面态 sealed 三态（Loading / Failed / Ready），卡级降级用可空表达
- `domain/` 领域模型（存原始数值，单位换算与格式化在 UI 层）
- `data/repository/` Repository + 15 分钟 TTL 线程安全内存缓存 + 结构化错误分级
- `data/location/` 定位封装：缓存/主动两级策略、新鲜度门槛、换城判定
- `remote/` Retrofit 接口（新版 `/airquality/v1` 与 `/v7` 端点）
- `di/` Hilt 装配，密钥经 `BuildConfig` 注入，不入库

## 图标来源

`res/drawable/x_*.xml` 为 **移植自 [PlayWeather](https://github.com/zhujiang521/PlayWeather)（MIT，作者朱江）**，`pathData` 未改动，每个文件头保留来源注释。

天气码到图标的映射表由本仓库按官方码表重写（61 个上游天气码 → 语义枚举 → 矢量资源）；移植过程中修正了上游两处缺陷：

- **昼夜判定**：上游用 `isDarkMode()` 判断夜晚，把「深色模式」误当「夜晚」——白天开深色会把太阳画成月亮；本仓库改由数据源的真实昼夜字段驱动。
- **温度区间为零导致 NaN**：上游用 `(weekMax - weekMin)` 作除数，周内温度恒定时跨度为 0；本仓库以 `coerceAtLeast(1.0)` 兜底，避免坐标映射溢出。

## 构建

1. **JDK 21**（AGP 9.1.1 要求；用 JDK 17 会直接报 `Android Gradle plugin requires Java 21`），Android Studio 或命令行 Gradle 均可
2. 项目根目录新建 `qweather.properties`（已被 `.gitignore` 忽略，勿提交）：
   ```properties
   apiKey=你的和风天气API Key
   baseUrl=https://你的APIHost.qweatherapi.com/
   ```
   Key 与 API Host 在 https://console.qweather.com 免费申请
3. `./gradlew assembleDebug`（Windows 下为 `.\gradlew assembleDebug`）
