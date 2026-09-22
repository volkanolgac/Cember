# VOLKAN WEB2ANDROID — UPDATE POLICY & MAINTENANCE PROTOCOL

This document defines the official engineering update policy for the **Volkan Web2Android Master Shell**.

---

## 🛑 Fundamental Golden Rule

> **DO NOT update the master shell merely because a newer library version exists.**
> 
> The baseline health check (`./gradlew healthCheck`) is an **internal baseline verification**, not an automatic upgrade mechanism.
> Every update introduces supply chain, behavioral, or toolchain regression risks. Updates must strictly adhere to the triage matrix below.

---

## 🎯 Update Classification Matrix

### 1. 🚨 CRITICAL UPDATE (Immediate / High Priority)
Triggers an immediate, verified patch release to the master shell:
- **Security Vulnerability**: High or Critical CVE discovered in bundled dependencies (e.g. WebKit, Kotlin runtime, Coroutines).
- **Google Play Mandatory Target SDK Requirement**: Annual Google Play policy deadlines requiring targetSdk increment for store updates.
- **Android Platform Compatibility Break**: Breaking OS behavior change on upcoming Android releases affecting WebView, asset loader, or background tasks.
- **Deprecated Android API Blocking Release**: Hard deprecation that prevents build compilation or store acceptance.

*Action Required*: Pin to the minimum patched stable version, run `./gradlew healthCheck`, `./gradlew test`, and perform full release verification.

---

### 2. 📅 PLANNED UPDATE (Scheduled Review Cycles)
Evaluated during scheduled master template maintenance windows:
- **New Stable Platform API**: Introduction of an Android standard capability that benefits the web runtime (e.g. improved WebKit asset streaming or memory optimization).
- **Important Bug Fix**: Resolves a verified edge-case bug in web asset loading, orientation switching, or predictive back navigation.
- **Performance / Stability Improvements**: Measurable decrease in startup time, memory footprint, or APK/AAB bundle size.

*Action Required*: Benchmark before and after, verify backward compatibility down to `minSdk 24`, ensure zero regressions in existing web apps.

---

### 3. ⏸️ OPTIONAL UPDATE (Deferred / Ignored)
Do **NOT** apply:
- **Minor / Patch version bump without functional impact**: Version increments that contain no relevant fixes for WebView hosts.
- **Experimental Features**: Preview or alpha/beta dependencies.
- **Unrequested Feature Volume**: Additional SDKs or third-party wrappers that violate the single-source shell architecture.

---

## 🔒 Verification Gate for Any Approved Update

Before committing any dependency change to the master shell, the following gates must be completed:

1. Update `DEPENDENCY_HEALTH.md` with new version and justification.
2. Update `libs.versions.toml` or `build.gradle.kts`.
3. Run `./gradlew clean`
4. Run `./gradlew test` (Robolectric & unit tests)
5. Run `./gradlew healthCheck` (Deterministic baseline check)
6. Run `./gradlew assembleDebug`
7. Verify release signing enforcement blocks unsigned builds and packages properly with valid credentials.
