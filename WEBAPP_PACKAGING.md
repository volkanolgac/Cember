# Web App Packaging Guide

## Supported Web Frameworks & Engines
- **Vanilla HTML5 / CSS3 / JavaScript (ES6+)**
- **Vite (React, Vue, Svelte, Solid, Preact)**
- **Next.js (Static Export mode: `output: 'export'`)**
- **Nuxt.js (Static Generate mode: `npx nuxi generate`)**
- **Astro (Static mode: `output: 'static'`)**
- **Phaser / Pixi.js / Three.js / Babylon.js (2D Canvas & 3D WebGL)**
- **Tailwind CSS / PostCSS / Sass / Bootstrap**

---

## 1. Web Framework Configuration

### Vite (`vite.config.js` / `vite.config.ts`)
Ensure relative or root base path is used for static packaging:
```javascript
import { defineConfig } from 'vite';

export default defineConfig({
  base: './', // or '/' with WebViewAssetLoader
  build: {
    outDir: 'dist'
  }
});
```

### Next.js (`next.config.js` / `next.config.mjs`)
Configure static export mode:
```javascript
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  trailingSlash: true,
  images: { unoptimized: true }
};

module.exports = nextConfig;
```

---

## 2. Packaging with the Master Factory (`packageApp`)

You can package and verify your web application in a single automated pass:

1. **Build your production web bundle**:
   ```bash
   npm run build
   cd dist && zip -r ../my-app.zip . && cd ..
   ```

2. **Configure app settings in `app/src/main/assets/app-config.json`**:
   Set `appName`, `applicationId`, `privacy`, `splashConfiguration`, and hardware permissions.

3. **Execute One-Step Packaging**:
   ```bash
   ./gradlew packageApp \
     -PwebAppZip=my-app.zip \
     -PiconPath=icon.png
   ```

This master factory task automatically:
- Unpacks and sanitizes your web assets
- Generates adaptive launcher, round, and Android 12+ splash icons
- Generates a Google Play Data Safety compliant `privacy-policy.html`
- Synchronizes `AndroidManifest.xml` permissions to least-privilege
- Runs static link, script, and development artifact checks
- Executes the 19-check baseline health validation
- Builds the production signed AAB or debug APK

---

## 3. JavaScript RPC Bridge API (`window.Volkan`)

The bridge client is auto-initialized on trusted origins:

```javascript
// Test RPC connection
const ping = await window.Volkan.ping();

// Read network state (including validated internet capability)
const net = await window.Volkan.getNetworkState();
console.log('Online:', net.isConnected, 'Validated:', net.isValidated, 'Status:', net.status);

// Trigger native haptics
await window.Volkan.vibrate(100);

// Open system share sheet
await window.Volkan.share({ title: 'App Title', text: 'Share text', url: 'https://example.com' });

// Clipboard read/write
await window.Volkan.copyToClipboard('Hello from JavaScript!');
const copied = await window.Volkan.getClipboard();

// Fullscreen and Orientation
await window.Volkan.setFullscreen(true);
await window.Volkan.setOrientation('LANDSCAPE');

// Keep screen awake (e.g. gameplay or video playback)
await window.Volkan.keepScreenOn(true);

// External navigation
const res = await window.Volkan.openExternal('https://example.com');
console.log('Outcome:', res.outcome); // "OPENED_EXTERNALLY" | "OPENED_IN_WEBVIEW"
```
