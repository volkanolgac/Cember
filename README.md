# VOLKAN WEB2ANDROID MASTER SHELL

> **A permanent, hardened, production-grade Android Web App Runtime and Native Host engineered for packaging high-performance web applications, 2D/3D canvas games, WebGL experiences, and PWAs into Google Play-ready Android App Bundles (AABs).**

---

## 🚀 Concept & Master Workflow

The **VOLKAN WEB2ANDROID MASTER SHELL** is designed to be configured and compiled without modifying native Kotlin code for each target web app.

```
+--------------------------------------------------------------------------------+
|                   VOLKAN MASTER FACTORY PACKAGING WORKFLOW                     |
|                                                                                |
|  1. Web Project Build       -->  `npm run build` (produces dist.zip or dist/)  |
|  2. Configure Metadata      -->  `app-config.json` (appName, privacy, splash)  |
|  3. One-Command Packaging   -->  `./gradlew packageApp`                        |
|                                  -PwebAppZip=dist.zip                          |
|                                  -PiconPath=icon.png                           |
|                                  -PappConfigPath=config.json                   |
|                                                                                |
|  Automated Factory Steps:                                                      |
|   • Import & validate configuration                                            |
|   • Extract and sanitize web assets                                            |
|   • Generate multi-density icons & Android 12+ splash assets                   |
|   • Generate Google Play-compliant privacy-policy.html                         |
|   • Synchronize least-privilege AndroidManifest permissions                    |
|   • Validate web assets for broken paths & dev URLs                            |
|   • Execute 19-point baseline engineering health check                         |
|   • Compile Google Play Release AAB (or Debug APK)                             |
+--------------------------------------------------------------------------------+
```

---

## ⚡ Architecture & Security Highlights

- **Standardized Toolchain**: **JDK 17** and **Gradle 9.3.1** (Self-contained wrapper included).
- **Master Factory Automation**: Run `./gradlew packageApp` to execute the full asset import, icon generation, privacy policy compilation, permission sync, static validation, and package generation in a single deterministic pass.
- **Automated Privacy Policy**: `./gradlew generatePrivacyPolicy` builds standalone (`generated/privacy-policy.html`) and in-app (`web/privacy-policy.html`) policies directly from declared capabilities in `app-config.json`.
- **Android 12+ SplashScreen API**: Seamless launcher-to-web loading screen using `androidx.core:core-splashscreen` with auto-generated 288x288 `ic_splash_icon.png` and customizable background color.
- **Secure Local Origin Loading**: Powered by `androidx.webkit:webkit:1.16.0` and `WebViewAssetLoader`, loading local assets securely under `https://appassets.androidplatform.net/` instead of insecure `file://` schemes.
- **Modern Bidirectional RPC Bridge**: Built with `WebViewCompat.addWebMessageListener` (completely removing legacy `addJavascriptInterface`), restricted to authorized origins with asynchronous Promise-based messaging.
- **Strict Release Signing Enforcement**: Release builds require valid keystore configuration (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Fallback to debug keystore is strictly blocked.
- **Hardware & Sensor Permissions**: Camera, microphone, and geolocation requests are strictly gated by `app-config.json` capability flags, origin verification, and Android runtime permission dialogs.
- **Deterministic Static Validation**: Automatically inspects HTML, CSS, JS, and JSON assets at build-time to catch broken paths, localhost/dev server URLs, and dev client runtimes (e.g. `@vite/client`).
- **Fail-Safe Crash Recovery**: Implements `onRenderProcessGone` recovery and user diagnostics without crashing the native OS process.

---

## 🛠️ Step-by-Step Master Factory Packaging Guide

### 1. Build Your Web Application
Generate a production bundle:
```bash
npm run build
cd dist && zip -r ../my-webapp.zip . && cd ..
```

### 2. Configure `app-config.json`
Edit `app/src/main/assets/app-config.json`:
```json
{
  "appName": "My Production App",
  "applicationId": "com.company.mywebapp",
  "versionName": "1.0.0",
  "versionCode": 1,
  "orientationMode": "PORTRAIT",
  "fullscreenMode": "EDGE_TO_EDGE",
  "cameraEnabled": false,
  "microphoneEnabled": false,
  "geolocationEnabled": false,
  "vibrationEnabled": true,
  "shareEnabled": true,
  "clipboardEnabled": true,
  "keepScreenOn": true,
  "downloadPolicyEnabled": true,
  "filePickerEnabled": true,
  "privacy": {
    "contactEmail": "support@example.com",
    "developerName": "Volkan Developer",
    "dataCollection": {
      "personalData": false,
      "accountData": false,
      "email": false,
      "location": false,
      "camera": false,
      "microphone": false,
      "deviceIdentifiers": false,
      "analytics": false,
      "advertising": false,
      "crashReports": false,
      "gameplayData": false
    },
    "dataUse": [
      "Local application rendering and execution",
      "On-device storage of user game states, preferences, and session data"
    ],
    "thirdParties": []
  },
  "splashConfiguration": {
    "backgroundColor": "#0B0F19",
    "iconAnimationDurationMs": 800
  }
}
```

### 3. Package Production App in One Command
```bash
export KEYSTORE_PATH=/path/to/upload-keystore.jks
export STORE_PASSWORD=your_store_password
export KEY_ALIAS=your_key_alias
export KEY_PASSWORD=your_key_password

./gradlew packageApp \
  -PwebAppZip=/path/to/my-webapp.zip \
  -PiconPath=/path/to/icon.png
```

The output AAB is created at `app/build/outputs/bundle/release/app-release.aab` with Google Play-ready privacy policy at `generated/privacy-policy.html`.

---

## 📱 Bridge API Reference (`window.Volkan`)

The bridge client is automatically injected into trusted web origins:

```javascript
// Test connectivity
const ping = await window.Volkan.ping();

// Read network status with validated internet check
const net = await window.Volkan.getNetworkState();
console.log(net.isConnected, net.isValidated, net.status); // "INTERNET_VALIDATED"

// Trigger haptic vibration
await window.Volkan.vibrate(150);

// Native Share Sheet
await window.Volkan.share({ title: "Check this out", url: "https://example.com" });

// Clipboard access
await window.Volkan.copyToClipboard("Hello from Web!");
const text = await window.Volkan.getClipboard();

// Keep screen awake (e.g. for games / video players)
await window.Volkan.keepScreenOn(true);

// Fullscreen & Orientation controls
await window.Volkan.setFullscreen(true);
await window.Volkan.setOrientation("LANDSCAPE");

// External URL navigation
const result = await window.Volkan.openExternal("https://example.com");
console.log(result.outcome); // "OPENED_EXTERNALLY" | "OPENED_IN_WEBVIEW"
```
