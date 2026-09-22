# Build & Release Engineering Guide

## System Requirements
- **JDK 17** (Standardized requirement across project toolchain and AGP 9.1.1)
- **Android SDK API 36** (`compileSdk 36`, `targetSdk 36`, `minSdk 24`)
- **Gradle 9.3.1** (Self-contained wrapper included in repository via `./gradlew`)

---

## Production Build Workflow

| Command | Purpose |
|---|---|
| `./gradlew packageApp` | **Master Factory Command**: Runs full packaging pipeline (config import, asset extraction, icon & splash generation, privacy policy generation, permission sync, static validation, health check, and bundle build) |
| `./gradlew generatePrivacyPolicy` | Compiles Google Play-compliant standalone (`generated/privacy-policy.html`) and in-app (`web/privacy-policy.html`) privacy policies from `app-config.json` |
| `./gradlew importAppConfig -PappConfigPath=config.json` | Imports and validates custom application metadata and privacy settings |
| `./gradlew importWebApp -PwebAppZip=dist.zip` | Extracts web application from ZIP with path-traversal & size protection |
| `./gradlew importWebApp -PwebAppDir=dist` | Copies web application from directory |
| `./gradlew importIcon -PiconPath=icon.png` | Generates multi-density (mdpi-xxxhdpi), adaptive launcher, and Android 12+ splash icons |
| `./gradlew syncManifestPermissions` | Synchronizes AndroidManifest with `app-config.json` hardware capability flags |
| `./gradlew validateWebApp` | Runs static verification on HTML, JS, CSS, JSON for broken references & dev artifacts |
| `./gradlew healthCheck` | Executes deterministic 19-point baseline engineering health check |
| `./gradlew test` | Executes local JVM and Robolectric unit tests |
| `./gradlew assembleDebug` | Compiles debug testing APK |
| `./gradlew assembleRelease` | Compiles production release APK (requires release signing) |
| `./gradlew bundleRelease` | Generates signed Google Play Android App Bundle (`.aab`) |

---

## Master Factory Automation (`packageApp`)

The master packaging command orchestrates the entire release pipeline with deterministic task ordering:

```bash
export KEYSTORE_PATH="/path/to/upload-keystore.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"

./gradlew packageApp \
  -PwebAppZip=/path/to/dist.zip \
  -PiconPath=/path/to/icon.png \
  -PappConfigPath=/path/to/app-config.json
```

Task Execution Order:
1. `importAppConfig`: Validates and writes configuration to `app-config.json`.
2. `importWebApp`: Unpacks web assets with sanitization into `app/src/main/assets/web`.
3. `importIcon`: Generates multi-density launcher, round, foreground, and `ic_splash_icon.png` assets.
4. `generatePrivacyPolicy`: Builds compliant privacy policy HTML matching declared data practices.
5. `syncManifestPermissions`: Adjusts `AndroidManifest.xml` permissions to least-privilege state.
6. `validateWebApp`: Statically verifies web asset paths, scripts, styles, and dev server references.
7. `healthCheck`: Runs deterministic 19-check verification pass.
8. `bundleRelease` (or `assembleDebug`): Produces final signed binary.
9. `packageApp`: Prints production verification summary and artifact paths.

---

## Release Signing Configuration

Release builds enforce **strict signing validation**. Fallback to debug keystores in release is strictly blocked.

Set the following credentials via environment variables or Gradle project properties:

```bash
export KEYSTORE_PATH="/path/to/upload-keystore.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"

# Build production signed bundle
./gradlew bundleRelease
```

Or via Gradle properties:
```bash
./gradlew bundleRelease \
  -PKEYSTORE_PATH=/path/to/upload-keystore.jks \
  -PSTORE_PASSWORD=your_keystore_password \
  -PKEY_ALIAS=your_key_alias \
  -PKEY_PASSWORD=your_key_password
```

Artifact output location:
`app/build/outputs/bundle/release/app-release.aab`
