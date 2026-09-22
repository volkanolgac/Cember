# Google Play Store Readiness & Compliance Specification

The **Volkan Web2Android Master Shell** is engineered from the ground up to meet and exceed Google Play Developer Program policies and technical acceptance criteria.

---

## 📋 Google Play Policy & Technical Verification Matrix

| Acceptance Requirement | Status | Architecture & Enforcement Mechanism |
|---|---|---|
| **Target SDK 36 (Android 16)** | ✅ Pinned | Configured in `app/build.gradle.kts` (`targetSdk = 36`, `compileSdk = 36`). Exceeds annual Google Play minimum requirements. |
| **Minimum SDK 24 (Android 7.0)** | ✅ Pinned | `minSdk = 24` ensures 99.5%+ global Android device reach. |
| **Android App Bundle (.aab)** | ✅ Ready | Produced via `./gradlew bundleRelease`. Optimized with resource and ABI splits for minimal download size. |
| **Deterministic Release Signing** | ✅ Enforced | `verifyReleaseSigning` task blocks build if release credentials are unset. Zero fallback to debug keys. |
| **Least-Privilege Permissions** | ✅ Automated | `syncManifestPermissions` synchronizes `AndroidManifest.xml` with `app-config.json` capability flags. Zero unsolicited permissions requested. |
| **No Dynamic Code Loading (DCL)** | ✅ Compliant | No runtime execution of remote `.dex`, `.jar`, or `.so` files. All web application assets are packaged securely inside the application bundle. |
| **Secure Asset Serving** | ✅ Enforced | `WebViewAssetLoader` serves local assets over `https://appassets.androidplatform.net`. `allowFileAccess` and `allowContentAccess` are permanently `false`. |
| **Modern JS Bridge Security** | ✅ Enforced | `WebViewCompat.addWebMessageListener` with strict origin validation replaces deprecated `addJavascriptInterface`. |
| **Zero-Permission Media Selection** | ✅ Integrated | `FileChooserManager` integrates Android Activity Result Contracts (`GetMultipleContents` / `GetContent`) for zero-storage-permission file uploads. |
| **Data Safety & Privacy Policy** | ✅ Automated | `./gradlew generatePrivacyPolicy` generates Play Console-compliant `privacy-policy.html` matching declared capabilities in `app-config.json`. |
| **Android 12+ SplashScreen API** | ✅ Integrated | `Theme.SplashScreen` with auto-generated 288x288 `ic_splash_icon.png` prevents cold-start blank screen artifacts. |
| **Hardware-Accelerated WebGL/Canvas** | ✅ Active | Hardware layer type enabled for 60 FPS animation, 2D/3D games, and WebGL visualizations. |
| **Multi-Density Adaptive Icons** | ✅ Automated | `./gradlew importIcon` generates standard mipmap assets (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) and API 26+ adaptive icon XML. |
| **Google Safe Browsing** | ✅ Active | WebSettings configures `SAFE_BROWSING_ENABLE` to protect users against malicious URLs. |
| **R8 / ProGuard Optimization** | ✅ Configured | `proguard-rules.pro` shrinks code while preserving WebMessageListener and bridge message schemas. |

---

## 🚀 Release Preparation & Publishing Steps

1. **Configure App Metadata & Privacy**:
   - Update `app-config.json` with your desired capabilities, privacy email, developer name, and data collection declarations.

2. **Run Master Packaging Factory**:
   ```bash
   export KEYSTORE_PATH="/path/to/release.keystore"
   export STORE_PASSWORD="your-keystore-password"
   export KEY_ALIAS="your-key-alias"
   export KEY_PASSWORD="your-key-password"

   ./gradlew packageApp \
     -PwebAppZip=/path/to/dist.zip \
     -PiconPath=/path/to/icon.png
   ```

3. **Verify Generated Artifacts**:
   - **Privacy Policy**: `generated/privacy-policy.html` (Upload URL / host on your public site for Play Console Data Safety requirement)
   - **Production Bundle**: `app/build/outputs/bundle/release/app-release.aab`
   - **In-App Policy**: Embedded at `app/src/main/assets/web/privacy-policy.html`

4. **Submit to Play Console**:
   - Upload `app-release.aab` to your Google Play Console track.
   - Fill out Google Play Data Safety questionnaire using the exact data disclosures in `generated/privacy-policy.html`.
   - Provide the public URL of your privacy policy.
