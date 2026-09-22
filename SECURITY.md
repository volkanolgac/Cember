# Security Architecture & Policies

The Volkan Web2Android runtime implements multi-layer defense-in-depth security:

## 1. Origin Sandboxing & WebViewAssetLoader
- The app loads web assets strictly through `WebViewAssetLoader` over the secure virtual origin `https://appassets.androidplatform.net/assets/`.
- `setAllowFileAccess(false)` and `setAllowContentAccess(false)` are strictly enforced to prevent arbitrary local file reads.
- `setAllowUniversalAccessFromFileURLs(false)` and `setAllowFileAccessFromFileURLs(false)` are enforced.

## 2. Modern WebMessageListener RPC Bridge
- Legacy `addJavascriptInterface` is completely removed from the production codebase.
- Communication uses `WebViewCompat.addWebMessageListener` exclusively, binding only to the exact trusted origins (`https://appassets.androidplatform.net` and HTTPS-configured app origin).
- Non-main-frame execution and untrusted origins are immediately rejected.
- All bridge actions are strictly gated by capability flags declared in `app-config.json`.

## 3. Hardware & Sensor Permissions
- Camera (`android.permission.CAMERA`), Microphone (`android.permission.RECORD_AUDIO`), and Geolocation (`android.permission.ACCESS_FINE_LOCATION`) requests are strictly verified against:
  1. `app-config.json` capability settings.
  2. Main-frame trusted origin verification.
  3. Explicit Android runtime permission flows.
- Denied or unconfigured permissions fail fast without prompting or granting access.

## 4. Network & Transport Security
- Mixed content mode is locked to `MIXED_CONTENT_NEVER_ALLOW`.
- Insecure SSL connections are strictly rejected in `onReceivedSslError`.
- Google Safe Browsing is active via `onSafeBrowsingHit`.

## 5. Build-Time Static & Archive Protection
- `ValidateWebAppTask` scans web assets for development server URLs (localhost/127.0.0.1), live-reload hooks (`@vite/client`), and broken local paths.
- `ImportWebAppTask` incorporates ZipSlip path traversal protection, duplicate entry rejection, and maximum file size / count limits.

## 6. Release Signing Integrity
- Release builds strictly require valid signing credentials (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- Fallback to debug keys in release packaging is blocked at build time.
