# Volkan Web2Android — Troubleshooting & Diagnostics Guide

This document details common development, packaging, and runtime issues along with exact diagnosis and resolution steps.

---

## 🛠️ Common Issues and Solutions

### 1. 🏥 Health Check Failures (`./gradlew healthCheck`)
- **Symptom**: Task `:app:healthCheck` fails with specific check errors.
- **Cause**: Toolchain version mismatch, missing documentation, or banned development artifacts in web assets.
- **Resolution**:
  - Run `./gradlew healthCheck` to view the specific failing check.
  - If a banned URL (e.g. `localhost:5173`) was detected, re-run `npm run build` in production mode and re-import via `./gradlew importWebApp -PwebAppZip=...`.
  - Ensure all 8 baseline specification markdown files are present in the project root.

---

### 2. 🔐 Release Signing Verification Failure (`RELEASE_SIGNING_NOT_CONFIGURED`)
- **Symptom**: `./gradlew assembleRelease` or `./gradlew bundleRelease` terminates with:
  `❌ Volkan Master Shell: RELEASE SIGNING NOT CONFIGURED`
- **Cause**: Release builds strictly prohibit debug key fallback. No valid keystore was provided via environment variables or Gradle properties.
- **Resolution**:
  - Export required signing credentials in your terminal or CI environment:
    ```bash
    export KEYSTORE_PATH="/path/to/my-release-key.jks"
    export STORE_PASSWORD="your-store-password"
    export KEY_ALIAS="your-key-alias"
    export KEY_PASSWORD="your-key-password"
    ```
  - For local debug testing, run `./gradlew assembleDebug` which signs with standard debug credentials.

---

### 3. 🌐 Web Application Blank / White Screen on Launch
- **Symptom**: Native splash screen dismisses to a blank white screen.
- **Diagnosis**:
  - Verify that `app/src/main/assets/web/index.html` exists.
  - Run `./gradlew validateWebApp` to scan for broken script or stylesheet links.
  - In your web build tool (Vite, Webpack, Next.js, Rollup), ensure `base: './'` or `base: '/'` produces relative asset paths (`href="assets/index.css"`, not absolute `/assets/...` rooted to machine root).
  - Open Chrome DevTools (`chrome://inspect`) on a debug build to view client-side JavaScript console errors.

---

### 4. 🌉 JavaScript Bridge Not Responding (`window.Volkan`)
- **Symptom**: Calls to `VolkanBridge.postMessage` or `Volkan.showToast()` do not resolve or reject.
- **Diagnosis**:
  - WebMessageListener binds strictly to configured origins. Ensure your web app runs under `https://appassets.androidplatform.net` or that your remote URL is listed in `allowedRemoteOrigins` in `app-config.json`.
  - Import and use the official promise SDK: `sample-web/bridge.js`.
  - In debug builds, inspect logcat with tag filter `Volkan` (`adb logcat -s Volkan:D`).

---

### 5. 🎨 App Icon Appears as Default Green Android
- **Symptom**: Custom icon is not visible on the home screen after install.
- **Resolution**:
  - Provide a high-resolution PNG (512x512 recommended) and run:
    ```bash
    ./gradlew importIcon -PiconPath=/path/to/my-app-icon.png
    ```
  - This automatically generates multi-density launcher bitmaps (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) and adaptive icon XML descriptors in `mipmap-anydpi-v26`.

---

### 6. 📝 Privacy Policy Validation Failure
- **Symptom**: Build fails with `Invalid 'privacy.contactEmail'` or `Missing required 'privacy' object`.
- **Cause**: Google Play compliance requires explicit privacy contact email and data collection declarations in `app-config.json`.
- **Resolution**:
  - Add or update the `"privacy"` block in `app/src/main/assets/app-config.json`:
    ```json
    "privacy": {
      "contactEmail": "your-support-email@domain.com",
      "developerName": "Your Name / Company",
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
      }
    }
    ```
  - Run `./gradlew generatePrivacyPolicy` to verify HTML output generation.

---

### 7. 📱 Missing Hardware Capabilities (Camera, Microphone, Geolocation)
- **Symptom**: Web application navigator API (`navigator.mediaDevices.getUserMedia` or `navigator.geolocation.getCurrentPosition`) fails or is rejected.
- **Resolution**:
  - Update `app/src/main/assets/app-config.json` to enable the required capability:
    ```json
    {
      "cameraEnabled": true,
      "microphoneEnabled": true,
      "geolocationEnabled": true
    }
    ```
  - Run `./gradlew packageApp` or `./gradlew preBuild`. The `syncManifestPermissions` task will automatically insert the necessary `<uses-permission>` tags into `AndroidManifest.xml`.
