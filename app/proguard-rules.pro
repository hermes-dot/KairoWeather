# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ──
# 保留序列化器及其描述符，开启混淆时保证反射仍可用
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.yuzheng.kairoweather.**$$serializer { *; }
-keepclassmembers class com.yuzheng.kairoweather.** {
    *** Companion;
}
-keepclasseswithmembers class com.yuzheng.kairoweather.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# ── Compose ──
# Compose 依赖库自带 consumer rules;此处兜底仅保留 runtime 内部类,
# 避免整包 keep 影响超过 100 类(IDE/Shrinker 检查报警)。
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.internal.** { *; }

# ── Retrofit / OkHttp ──
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**
