// 构建仓库分两套：CI 的 runner 在境外，本地开发在国内，网络环境正好相反。
// 实测（2026-10-16，CI 首次跑红）：
//   · 腾讯 google-maven 镜像 → 404（它只镜像 Google 系构件，不含只在 Maven Central 上的 KSP）
//   · aliyun 两个镜像 → 502 Bad Gateway（境外访问被拒）
//   · dl.google.com → 404（KSP 不在 Google Maven 上）
//   · repo.maven.apache.org / plugins.gradle.org → 200（KSP 在这里）
// 关键在于 Gradle 的行为：撞上 502 会直接判定"插件解析失败"，
// **不会继续去查后面的官方源** —— 于是 Central 上存在的 KSP 也装不上，BUILD FAILED。
// ⇒ CI 只走官方源；本地保持"国内镜像优先 + 官方兜底"，冷启动更快。
// 注意：pluginManagement 必须是 settings 文件里的第一个语句，所以条件只能内联在这里。
pluginManagement {
    repositories {
        if (System.getenv("CI") == "true") {
            google()
            mavenCentral()
            gradlePluginPortal()
        } else {
            // === 国内镜像（优先） ===
            // 腾讯云 - Google Maven 镜像
            maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/google-maven/") }
            // 阿里云 - Maven Central 镜像
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            // 阿里云 - Gradle Plugin 镜像
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

            // === 官方源（兜底） ===
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (System.getenv("CI") == "true") {
            google()
            mavenCentral()
        } else {
            // === 国内镜像（优先） ===
            // 腾讯云 - Google Maven 镜像
            maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/google-maven/") }
            // 阿里云 - Maven Central 镜像
            maven { url = uri("https://maven.aliyun.com/repository/public") }

            // === 官方源（兜底） ===
            google()
            mavenCentral()
        }
    }
}

rootProject.name = "KairoWeather"
include(":app")
 