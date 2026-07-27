pluginManagement {
    repositories {
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
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

rootProject.name = "KairoWeather"
include(":app")
 