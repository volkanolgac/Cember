# VOLKAN WEB2ANDROID — DEPENDENCY HEALTH BASELINE

This document establishes the approved baseline toolchain and dependencies for the **Volkan Web2Android Master Shell**. All versions are pinned for deterministic, reproducible production builds.

---

## 📋 Approved Toolchain Baseline

| Component / Tool | Approved Pinned Version | Purpose & Architectural Rationale | Justification Required for Upgrade |
|---|---|---|---|
| **JDK (Java Development Kit)** | `17` (LTS) | Standard LTS Java baseline supported across all modern Android Gradle Plugins (AGP 8.x / 9.x) and Android SDK Build Tools. Provides high compilation throughput and JVM stability. | Upgraded only when a future AGP or Google Play build-tool requirement mandates JDK 21+. |
| **Android Gradle Plugin (AGP)** | `9.1.1` | Production build system driving compilation, resource packaging (AAPT2), R8 obfuscation/shrinking, and Android App Bundle (.aab) generation. | Mandatory Google Play API target changes, major AGP security patch, or breaking Gradle platform migration. |
| **Gradle Distribution** | `9.3.1` (via Wrapper) | Build automation runtime with Configuration Cache compliance, incremental compilation, and daemon isolation. | AGP compatibility requirements or critical build-system security fixes. |
| **Kotlin Language & Toolchain** | `2.2.10` | High-performance modern Kotlin compiler with standard coroutine and JVM target compatibility. | Android platform Kotlin standard alignment or critical compiler bug fixes. |
| **AndroidX WebKit** | `1.16.0` | Powers `WebViewAssetLoader` (secure virtual origin `https://appassets.androidplatform.net`) and `WebViewCompat.addWebMessageListener` (modern bidirectional RPC bridge). | Security patches in WebKit or critical Android WebView compatibility updates. |
| **Android SDK compileSdk** | `36` (Android 16 preview / API 36) | Target compile SDK ensuring full forward compatibility with newest Android APIs and OS behaviors. | Android annual SDK platform increments mandated by Google Play. |
| **Android SDK targetSdk** | `36` | Target runtime behavior SDK ensuring full compatibility with Android platform privacy, permissions, and security restrictions. | Google Play annual target API mandates (typically August/November deadlines). |
| **Android SDK minSdk** | `24` (Android 7.0 Nougat) | Covers 99.5%+ of all active global Android devices while supporting modern WebKit and Java 8/17 desugaring. | Dropped to higher version only if Google Play or WebKit dependencies drop support for API 24. |

---

## 📦 Production Runtime Dependencies

| Library | Version | Purpose & Architectural Rationale | Upgrade Justification |
|---|---|---|---|
| `androidx.webkit:webkit` | `1.16.0` | Core native WebView runtime, secure asset loading via `WebViewAssetLoader`, modern WebMessageListener bridge. | Critical CVE in WebKit or required new platform API. |
| `androidx.core:core-splashscreen` | `1.0.1` | Android 12+ standard backward-compatible Splash Screen API with seamless transition to web content. | Bug fixes in splash screen dismissal or theming. |
| `androidx.core:core-ktx` | `1.15.0` | Kotlin extensions for Android framework APIs. | Platform compatibility updates. |
| `androidx.activity:activity-ktx` | `1.10.0` | Predictive Back Navigation handling (`OnBackPressedDispatcher`) and activity result contracts. | Changes to predictive back behavior. |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.8.7` | Lifecycle-aware coroutine scopes (`lifecycleScope`) and state observation. | Lifecycle management improvements. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.10.1` | Asynchronous task scheduling and non-blocking IO for bridge RPC dispatching. | Kotlin compiler compatibility or coroutines engine fix. |

---

## 🧪 Testing Dependencies

| Library | Version | Purpose | Upgrade Justification |
|---|---|---|---|
| `junit:junit` | `4.13.2` | Core JVM test execution engine. | Stable industry baseline. |
| `androidx.test.ext:junit` | `1.2.1` | AndroidX JUnit test runner extensions. | AndroidX test framework updates. |
| `org.robolectric:robolectric` | `4.14.1` | Fast, headless Android unit test execution without physical emulator or device. | AGP/SDK 36 compatibility. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.10.1` | Coroutine testing utilities (`runTest`, test dispatchers). | Coroutines version alignment. |
