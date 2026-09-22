# Volkan Web2Android R8 / ProGuard Configuration

# Keep WebKit and WebMessageListener interfaces
-keep class androidx.webkit.** { *; }
-dontwarn androidx.webkit.**

# Keep Volkan Bridge & Config model classes
-keep class com.example.volkan.config.** { *; }
-keep class com.example.volkan.bridge.** { *; }
-keep class com.example.volkan.network.** { *; }
-keep class com.example.volkan.util.** { *; }

-keepclassmembers class com.example.volkan.** {
    public *;
}

# Preserve line numbers for release diagnostics
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
