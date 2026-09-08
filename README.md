# 天气通 (KairoWeather)

基于和风天气 API 的 Android 天气应用，全量 Jetpack Compose 开发，零 View 体系。

## 功能

- 实时天气、24 小时逐小时预报、未来 7 日趋势（温度区间条可视化）
- 空气质量卡：AQI（中国国标）+ 等级着色刻度条 + 分人群健康建议
- 日出日落轨迹卡片（Compose Canvas + Path 自绘）
- 定位跟随：lastKnown 秒开渲染 + 新鲜度门槛 + 回前台/下拉刷新换城自动复查
- 城市搜索与手动选城、温度单位（℃/℉）、主题切换（偏好 DataStore 持久化）
- 多色天气符号系统（26 枚自绘 vector）

## 技术栈

Jetpack Compose + Material3 · sealed 页面状态 + StateFlow（`collectAsStateWithLifecycle`） · Hilt · Retrofit + OkHttp + kotlinx.serialization · FusedLocationProvider · DataStore Preferences · MockK + 协程测试（105 个单元测试） · KSP · R8

## 模块结构

- `ui/` Compose 界面与组件，页面态 sealed 三态（Loading/Failed/Ready），卡级降级用可空表达
- `domain/` 领域模型（存原始数值，单位换算与格式化在 UI 层）
- `data/repository/` Repository + 15 分钟 TTL 线程安全内存缓存 + 结构化错误分级
- `data/location/` 定位封装：缓存/主动两级策略、新鲜度门槛、换城判定
- `remote/` Retrofit 接口（新版 `/airquality/v1` 与 `/v7` 端点）
- `di/` Hilt 装配，密钥经 `BuildConfig` 注入，不入库

## 构建

1. JDK 17+，Android Studio 或命令行 Gradle
2. 项目根目录新建 `qweather.properties`（已被 `.gitignore` 忽略，勿提交）：
   ```properties
   apiKey=你的和风天气API Key
   baseUrl=https://你的APIHost.qweatherapi.com/
   ```
   Key 与 API Host 在 https://console.qweather.com 免费申请
3. `gradlew assembleDebug`
