#!/usr/bin/env python3
"""
VOLKAN WEB2ANDROID MASTER SHELL
Universal GitHub Repository Auto-Discovery, Build, and Packaging Engine
"""

import sys
import os
import json
import re
import shutil
import subprocess
import tarfile
import zipfile
from pathlib import Path

WORKSPACE_ROOT = Path(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
EXCLUDED_DIRS = {
    "app", "gradle", ".gradle", "build", ".git", ".idea", "generated",
    ".build-outputs", "tools", ".android", "bin", ".gradle_home"
}

def log(msg, emoji="ℹ️"):
    print(f"{emoji} {msg}")

def safe_slug(name):
    clean = re.sub(r'[^a-zA-Z0-9]', '', name).lower()
    return clean if clean else "app"

class UniversalDiscoveryEngine:
    def __init__(self, root_dir=WORKSPACE_ROOT):
        self.root = Path(root_dir)
        self.discovered_app_name = None
        self.discovered_source_path = None
        self.discovered_input_type = None  # "PRODUCTION BUILD" or "SOURCE PROJECT" or "ARCHIVE"
        self.discovered_build_tool = "direct"
        self.discovered_icon_path = None
        self.discovered_config_path = None
        self.detected_features = {
            "camera": False,
            "microphone": False,
            "geolocation": False,
            "notifications": False,
            "vibration": True,
            "clipboard": True,
            "websockets": False,
            "remote_apis": False,
            "analytics": False,
            "ads": False,
            "third_parties": set()
        }

    def run_discovery(self):
        log("Starting Universal GitHub Repository Discovery...", "🔍")
        self._discover_config()
        self._discover_web_source()
        self._discover_icon()
        self._finalize_app_metadata()

    def _discover_config(self):
        config_candidates = [
            "app-config.json",
            "android-config.json",
            "volkan-config.json",
            "mobile-config.json"
        ]
        for name in config_candidates:
            candidate = self.root / name
            if candidate.exists() and candidate.is_file():
                self.discovered_config_path = str(candidate)
                log(f"Found authoritative configuration: {name}", "📄")
                return

        # Check subdirectories (excluding system dirs)
        for path in self.root.iterdir():
            if path.is_dir() and path.name not in EXCLUDED_DIRS:
                for name in config_candidates:
                    cand = path / name
                    if cand.exists() and cand.is_file():
                        self.discovered_config_path = str(cand)
                        log(f"Found configuration in {path.name}/{name}", "📄")
                        return

    def _discover_icon(self):
        icon_names = [
            "icon.png", "app-icon.png", "app_icon.png", "launcher.png",
            "logo.png", "logo.svg", "favicon.png", "android-icon.png",
            "icon.webp", "icon.jpg", "logo-512.png", "icon-512.png"
        ]
        
        # 1. Search root directory
        for name in icon_names:
            cand = self.root / name
            if cand.exists() and cand.is_file():
                self.discovered_icon_path = str(cand)
                log(f"Found application icon: {name}", "🎨")
                return

        # 2. Search inside web source / public / assets directories
        candidate_dirs = ["public", "src/assets", "assets", "public/icons", "web", "webapp_input", "sample-web"]
        for cdir in candidate_dirs:
            target = self.root / cdir
            if target.exists() and target.is_dir():
                for name in icon_names:
                    cand = target / name
                    if cand.exists() and cand.is_file():
                        self.discovered_icon_path = str(cand)
                        log(f"Found application icon in {cdir}/{name}", "🎨")
                        return

        # 3. Recursive search for high-confidence square icon images
        for path in self.root.rglob("*"):
            if path.is_file() and path.suffix.lower() in [".png", ".webp", ".jpg", ".jpeg"]:
                rel_parts = path.relative_to(self.root).parts
                if any(p in EXCLUDED_DIRS for p in rel_parts):
                    continue
                name_lower = path.name.lower()
                if any(keyword in name_lower for keyword in ["icon", "logo", "launcher", "favicon"]):
                    self.discovered_icon_path = str(path)
                    log(f"Found icon candidate: {path.relative_to(self.root)}", "🎨")
                    return

        log("ICON WARNING: No custom application icon found. Using master icon.", "⚠️")

    def _discover_web_source(self):
        # 1. Check for candidate archives (*.zip, *.tar, *.tgz)
        archives = []
        for path in self.root.iterdir():
            if path.is_file() and path.suffix.lower() in [".zip", ".tar", ".tgz"]:
                archives.append(path)

        if archives:
            # Prioritize zip by keywords
            def archive_score(p):
                name = p.name.lower()
                for i, kw in enumerate(["app", "web", "dist", "build", "production", "release", "game"]):
                    if kw in name:
                        return i
                return 99

            archives.sort(key=archive_score)
            selected_archive = archives[0]
            self.discovered_source_path = str(selected_archive)
            self.discovered_input_type = "ARCHIVE"
            log(f"Discovered application archive: {selected_archive.name}", "📦")
            return

        # 2. Check for web project directories in root or subdirectories
        # Is the root directory itself a web project?
        if (self.root / "package.json").exists():
            self.discovered_source_path = str(self.root)
            self.discovered_input_type = "SOURCE PROJECT"
            log(f"Discovered source project in workspace root", "📦")
            return

        # Check subdirectories
        candidate_dirs = []
        for path in self.root.iterdir():
            if path.is_dir() and path.name not in EXCLUDED_DIRS:
                if (path / "package.json").exists():
                    candidate_dirs.append((path, "SOURCE PROJECT", 1))
                elif (path / "index.html").exists():
                    candidate_dirs.append((path, "PRODUCTION BUILD", 2))
                elif (path / "dist" / "index.html").exists():
                    candidate_dirs.append((path / "dist", "PRODUCTION BUILD", 3))

        if candidate_dirs:
            candidate_dirs.sort(key=lambda x: x[2])
            chosen = candidate_dirs[0]
            self.discovered_source_path = str(chosen[0])
            self.discovered_input_type = chosen[1]
            log(f"Discovered web application: {chosen[0].name} ({chosen[1]})", "📦")
            return

        # Fallback to existing web assets directory
        fallback_web = self.root / "app/src/main/assets/web"
        if fallback_web.exists() and (fallback_web / "index.html").exists():
            self.discovered_source_path = str(fallback_web)
            self.discovered_input_type = "PRODUCTION BUILD"
            log(f"Using embedded web assets directory: {fallback_web.relative_to(self.root)}", "📦")

    def _finalize_app_metadata(self):
        # Determine App Name from config, package.json, README, or directory
        if self.discovered_config_path:
            try:
                with open(self.discovered_config_path, "r", encoding="utf-8") as fp:
                    data = json.load(fp)
                    if data.get("appName"):
                        self.discovered_app_name = data["appName"]
            except Exception:
                pass

        if not self.discovered_app_name:
            # Try package.json
            pkg_file = None
            if self.discovered_source_path and Path(self.discovered_source_path).is_dir():
                cand_pkg = Path(self.discovered_source_path) / "package.json"
                if cand_pkg.exists():
                    pkg_file = cand_pkg
            if not pkg_file and (self.root / "package.json").exists():
                pkg_file = self.root / "package.json"

            if pkg_file and pkg_file.exists():
                try:
                    with open(pkg_file, "r", encoding="utf-8") as fp:
                        pdata = json.load(fp)
                        raw_name = pdata.get("name") or pdata.get("displayName")
                        if raw_name:
                            # Format name nicely: e.g. "my-awesome-game" -> "My Awesome Game"
                            formatted = " ".join(word.capitalize() for word in re.split(r'[-_]', raw_name))
                            self.discovered_app_name = formatted
                except Exception:
                    pass

        if not self.discovered_app_name:
            # Try README.md title (# Title)
            readme_file = self.root / "README.md"
            if readme_file.exists():
                try:
                    with open(readme_file, "r", encoding="utf-8") as fp:
                        for line in fp:
                            line = line.strip()
                            if line.startswith("# "):
                                title = line[2:].strip()
                                # Clean emojis or markdown symbols
                                title = re.sub(r'[^\w\s-]', '', title).strip()
                                if title:
                                    self.discovered_app_name = title
                                    break
                except Exception:
                    pass

        if not self.discovered_app_name:
            self.discovered_app_name = "Volkan App"

        log(f"Application Name resolved to: '{self.discovered_app_name}'", "🏷️")

class UniversalBuildAndPackageEngine:
    def __init__(self, discovery: UniversalDiscoveryEngine):
        self.disc = discovery
        self.root = discovery.root
        self.target_web_dir = self.root / "app/src/main/assets/web"
        self.target_config_file = self.root / "app/src/main/assets/app-config.json"
        self.temp_build_dir = self.root / ".build-outputs/temp_extracted"

    def process(self):
        log("Processing and building web application...", "⚡")
        staging_dir = self._prepare_web_staging()
        self._analyze_features_and_security(staging_dir)
        self._adapt_asset_paths(staging_dir)
        self._sync_app_config()
        self._deploy_to_assets(staging_dir)
        log("Web application staged and adapted successfully.", "✅")

    def _prepare_web_staging(self) -> Path:
        staging_dir = self.root / ".build-outputs/web_staged"
        if staging_dir.exists():
            shutil.rmtree(staging_dir)
        staging_dir.mkdir(parents=True, exist_ok=True)

        src_path = Path(self.disc.discovered_source_path) if self.disc.discovered_source_path else None

        if not src_path or not src_path.exists():
            # If nothing was discovered, verify existing web dir
            if (self.target_web_dir / "index.html").exists():
                shutil.copytree(self.target_web_dir, staging_dir, dirs_exist_ok=True)
                return staging_dir
            else:
                raise RuntimeError("❌ No web application source found to package.")

        # CASE 1: ARCHIVE (*.zip, *.tar, *.tgz)
        if src_path.is_file():
            extract_temp = self.root / ".build-outputs/archive_extracted"
            if extract_temp.exists():
                shutil.rmtree(extract_temp)
            extract_temp.mkdir(parents=True, exist_ok=True)

            log(f"Extracting archive: {src_path.name}", "📦")
            if src_path.suffix.lower() == ".zip":
                with zipfile.ZipFile(src_path, 'r') as zip_ref:
                    # Sanitize path extraction against Zip Slip
                    for member in zip_ref.namelist():
                        dest_file = extract_temp / member
                        if not str(dest_file.resolve()).startswith(str(extract_temp.resolve())):
                            raise RuntimeError(f"❌ Malicious zip entry detected: {member}")
                    zip_ref.extractall(extract_temp)
            else:
                with tarfile.open(src_path, 'r:*') as tar_ref:
                    tar_ref.extractall(extract_temp)

            # Check if extracted content has index.html or package.json
            return self._stage_from_directory(extract_temp, staging_dir)

        # CASE 2: DIRECTORY (SOURCE OR PRODUCTION)
        elif src_path.is_dir():
            return self._stage_from_directory(src_path, staging_dir)

        return staging_dir

    def _stage_from_directory(self, src_dir: Path, staging_dir: Path) -> Path:
        # Check if single top-level directory exists inside src_dir
        subdirs = [d for d in src_dir.iterdir() if d.is_dir() and d.name not in ["__MACOSX", ".git"]]
        subfiles = [f for f in src_dir.iterdir() if f.is_file()]
        
        effective_dir = src_dir
        if len(subdirs) == 1 and len(subfiles) == 0:
            effective_dir = subdirs[0]

        # Check if production build already exists (index.html present and no package.json requiring build)
        has_index = (effective_dir / "index.html").exists()
        has_package = (effective_dir / "package.json").exists()

        if has_index and not has_package:
            log("Archive/Directory identified as PRODUCTION BUILD.", "📦")
            self.disc.discovered_input_type = "PRODUCTION BUILD"
            shutil.copytree(effective_dir, staging_dir, dirs_exist_ok=True)
            return staging_dir

        # Check if dist/ or build/ or out/ directory already has index.html
        for build_sub in ["dist", "build", "out", ".output/public", "public"]:
            sub_cand = effective_dir / build_sub
            if sub_cand.exists() and (sub_cand / "index.html").exists():
                log(f"Found production build output in '{build_sub}/'", "📦")
                self.disc.discovered_input_type = "PRODUCTION BUILD"
                shutil.copytree(sub_cand, staging_dir, dirs_exist_ok=True)
                return staging_dir

        if has_package:
            log("Directory identified as SOURCE PROJECT. Initiating automated build...", "⚙️")
            self.disc.discovered_input_type = "SOURCE PROJECT"
            return self._build_source_project(effective_dir, staging_dir)

        if has_index:
            log("Found index.html in source directory. Treating as production static build.", "📦")
            self.disc.discovered_input_type = "PRODUCTION BUILD"
            shutil.copytree(effective_dir, staging_dir, dirs_exist_ok=True)
            return staging_dir

        raise RuntimeError(f"❌ Unable to identify web application entry point (index.html or package.json) in {effective_dir}")

    def _build_source_project(self, project_dir: Path, staging_dir: Path) -> Path:
        # Inspect package.json
        pkg_file = project_dir / "package.json"
        with open(pkg_file, "r", encoding="utf-8") as fp:
            pkg_data = json.load(fp)

        scripts = pkg_data.get("scripts", {})
        
        # Determine package manager
        pm = "npm"
        if (project_dir / "yarn.lock").exists() and shutil.which("yarn"):
            pm = "yarn"
        elif (project_dir / "pnpm-lock.yaml").exists() and shutil.which("pnpm"):
            pm = "pnpm"
        elif shutil.which("npm"):
            pm = "npm"

        self.disc.discovered_build_tool = pm
        log(f"Using package manager: {pm}", "🛠️")

        # Run install if node_modules not present
        if not (project_dir / "node_modules").exists():
            log(f"Running '{pm} install'...", "📦")
            cmd_install = [pm, "install", "--prefer-offline", "--no-audit"] if pm == "npm" else [pm, "install"]
            subprocess.run(cmd_install, cwd=str(project_dir), check=True)

        # Determine build script
        build_script = None
        for script_name in ["build", "generate", "export", "dist"]:
            if script_name in scripts:
                build_script = script_name
                break

        if not build_script:
            raise RuntimeError(f"❌ No suitable build script found in package.json (available: {list(scripts.keys())})")

        log(f"Executing web build: '{pm} run {build_script}'...", "🚀")
        cmd_build = [pm, "run", build_script] if pm != "yarn" else ["yarn", build_script]
        subprocess.run(cmd_build, cwd=str(project_dir), check=True)

        # Detect build output folder
        for out_name in ["dist", "build", "out", ".output/public", ".next/out", "public"]:
            cand = project_dir / out_name
            if cand.exists() and (cand / "index.html").exists():
                log(f"Build output located at: {cand.name}/", "✅")
                shutil.copytree(cand, staging_dir, dirs_exist_ok=True)
                return staging_dir

        if (project_dir / "index.html").exists():
            shutil.copytree(project_dir, staging_dir, dirs_exist_ok=True)
            return staging_dir

        raise RuntimeError(f"❌ Web build succeeded but could not locate output directory containing index.html.")

    def _analyze_features_and_security(self, staging_dir: Path):
        log("Scanning web assets for features, privacy disclosures, and security patterns...", "🔍")
        
        # Secret scan patterns
        secret_patterns = [
            (re.compile(r"-----BEGIN\s+(RSA|EC|OPENSSH|DSA|PGP|PRIVATE)\s+KEY-----"), "Private Key Header"),
            (re.compile(r"\bAIzaSy[A-Za-z0-9_-]{33}\b"), "Google API Key"),
            (re.compile(r"\bAKIA[0-9A-Z]{16}\b"), "AWS Access Key ID"),
            (re.compile(r"\bxox[baprs]-[0-9a-zA-Z]{10,48}\b"), "Slack Token"),
            (re.compile(r"\bgh[pousr]_[0-9a-zA-Z]{36}\b"), "GitHub Token"),
            (re.compile(r"\bsk_live_[0-9a-zA-Z]{24}\b"), "Stripe Live Secret Key")
        ]

        # Feature detection patterns
        feature_patterns = {
            "camera": re.compile(r"navigator\.mediaDevices\.getUserMedia|camera|video\s*:\s*true", re.IGNORECASE),
            "microphone": re.compile(r"navigator\.mediaDevices\.getUserMedia|audio\s*:\s*true|webkitAudioContext|AudioContext", re.IGNORECASE),
            "geolocation": re.compile(r"navigator\.geolocation\.getCurrentPosition|navigator\.geolocation\.watchPosition", re.IGNORECASE),
            "notifications": re.compile(r"Notification\.requestPermission|serviceWorker\.showNotification", re.IGNORECASE),
            "websockets": re.compile(r"new\s+WebSocket\(|wss://|ws://", re.IGNORECASE),
            "analytics": re.compile(r"google-analytics\.com|googletagmanager\.com|mixpanel|amplitude", re.IGNORECASE),
            "ads": re.compile(r"googlesyndication\.com|doubleclick\.net|admob", re.IGNORECASE)
        }

        url_pattern = re.compile(r'https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:/[^\s"\']*)?')

        for file_path in staging_dir.rglob("*"):
            if file_path.is_file() and file_path.suffix.lower() in [".html", ".js", ".mjs", ".css", ".json", ".webmanifest"]:
                try:
                    with open(file_path, "r", encoding="utf-8", errors="ignore") as fp:
                        content = fp.read()
                        
                        # Check secrets
                        for regex, desc in secret_patterns:
                            if regex.search(content):
                                raise RuntimeError(f"❌ Security Block: High-confidence secret/credential detected ({desc}) in {file_path.name}. Production builds must not embed sensitive keys.")

                        # Check features
                        for feat, regex in feature_patterns.items():
                            if regex.search(content):
                                self.disc.detected_features[feat] = True

                        # Check external origins for privacy disclosure
                        for match in url_pattern.finditer(content):
                            url = match.group(0)
                            if not any(ign in url for ign in ["appassets.androidplatform.net", "w3.org", "schema.org", "localhost", "127.0.0.1"]):
                                try:
                                    domain = url.split("/")[2]
                                    self.disc.detected_features["third_parties"].add(domain)
                                except Exception:
                                    pass
                except Exception as e:
                    if "Security Block" in str(e):
                        raise e

    def _adapt_asset_paths(self, staging_dir: Path):
        log("Adapting asset references to embedded WebView origin...", "🔄")
        adapted_count = 0

        for file_path in staging_dir.rglob("*"):
            if not file_path.is_file():
                continue
            ext = file_path.suffix.lower()
            
            # HTML files
            if ext in [".html", ".htm"]:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as fp:
                    content = fp.read()
                original = content
                
                # Replace root-relative attributes: src="/...", href="/...", poster="/...", srcset="/..."
                content = re.sub(r'\b(src|href|poster)\s*=\s*["\']/([^/\'"][^"\']*)["\']', r'\1="./\2"', content)
                content = re.sub(r'\bsrcset\s*=\s*["\']/([^/\'"][^"\']*)["\']', r'srcset="./\1"', content)
                content = re.sub(r'<base\s+href=["\']/(["\'])', r'<base href=".\1', content)

                if content != original:
                    with open(file_path, "w", encoding="utf-8") as fp:
                        fp.write(content)
                    adapted_count += 1

            # CSS files
            elif ext == ".css":
                with open(file_path, "r", encoding="utf-8", errors="ignore") as fp:
                    content = fp.read()
                original = content
                content = re.sub(r'url\(\s*["\']?/([^/\'"][^"\')]+)["\']?\s*\)', r'url("./\1")', content)
                if content != original:
                    with open(file_path, "w", encoding="utf-8") as fp:
                        fp.write(content)
                    adapted_count += 1

            # JS files (safe targeted transformation)
            elif ext in [".js", ".mjs", ".cjs"]:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as fp:
                    content = fp.read()
                original = content
                asset_dirs = ["assets", "images", "img", "fonts", "sounds", "media", "static", "icons", "chunks", "_next"]
                for adir in asset_dirs:
                    content = content.replace(f'"/{adir}/', f'"./{adir}/')
                    content = content.replace(f"'/{adir}/", f"'./{adir}/")
                    content = content.replace(f"`/{adir}/", f"`./{adir}/")
                if content != original:
                    with open(file_path, "w", encoding="utf-8") as fp:
                        fp.write(content)
                    adapted_count += 1

            # Manifest / JSON
            elif ext in [".json", ".webmanifest"]:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as fp:
                    content = fp.read()
                original = content
                content = re.sub(r'"src"\s*:\s*"/([^/"][^"]*)"', r'"src": "./\1"', content)
                if content != original:
                    with open(file_path, "w", encoding="utf-8") as fp:
                        fp.write(content)
                    adapted_count += 1

        log(f"Asset path adaptation completed ({adapted_count} files adapted).", "✅")

    def _sync_app_config(self):
        # Load or generate app-config.json
        base_config = {}
        if self.disc.discovered_config_path and os.path.exists(self.disc.discovered_config_path):
            with open(self.disc.discovered_config_path, "r", encoding="utf-8") as fp:
                base_config = json.load(fp)

        # Merge defaults
        app_name = base_config.get("appName") or self.disc.discovered_app_name or "Volkan App"
        app_slug = safe_slug(app_name)
        app_id = base_config.get("applicationId") or f"com.volkanolgac.{app_slug}"
        version_name = base_config.get("versionName") or "1.0.0"
        version_code = int(base_config.get("versionCode") or 1)

        privacy_map = base_config.get("privacy", {})
        contact_email = privacy_map.get("contactEmail") or "volkanolgac@gmail.com"
        developer_name = privacy_map.get("developerName") or "Volkan Olgaç"

        data_collection = privacy_map.get("dataCollection", {
            "personalData": False,
            "accountData": False,
            "email": False,
            "location": self.disc.detected_features["geolocation"],
            "camera": self.disc.detected_features["camera"],
            "microphone": self.disc.detected_features["microphone"],
            "deviceIdentifiers": False,
            "analytics": self.disc.detected_features["analytics"],
            "advertising": self.disc.detected_features["ads"],
            "crashReports": False,
            "gameplayData": False
        })

        third_parties = list(set(privacy_map.get("thirdParties", []) + list(self.disc.detected_features["third_parties"])[:10]))

        final_config = {
            "appName": app_name,
            "applicationId": app_id,
            "versionCode": version_code,
            "versionName": version_name,
            "webAppOrigin": base_config.get("webAppOrigin", "https://appassets.androidplatform.net"),
            "webAppAssetDirectory": "web",
            "webAppEntry": base_config.get("webAppEntry", "index.html"),
            "orientationMode": base_config.get("orientationMode", "PORTRAIT"),
            "fullscreenMode": base_config.get("fullscreenMode", "EDGE_TO_EDGE"),
            "networkRequired": base_config.get("networkRequired", len(third_parties) > 0),
            "hardwareAcceleration": base_config.get("hardwareAcceleration", True),
            "safeBrowsingEnabled": base_config.get("safeBrowsingEnabled", True),
            "keepScreenOn": base_config.get("keepScreenOn", True),
            "vibrationEnabled": base_config.get("vibrationEnabled", self.disc.detected_features["vibration"]),
            "cameraEnabled": base_config.get("cameraEnabled", self.disc.detected_features["camera"]),
            "microphoneEnabled": base_config.get("microphoneEnabled", self.disc.detected_features["microphone"]),
            "geolocationEnabled": base_config.get("geolocationEnabled", self.disc.detected_features["geolocation"]),
            "notificationsEnabled": base_config.get("notificationsEnabled", self.disc.detected_features["notifications"]),
            "clipboardEnabled": base_config.get("clipboardEnabled", self.disc.detected_features["clipboard"]),
            "downloadPolicyEnabled": base_config.get("downloadPolicyEnabled", True),
            "filePickerEnabled": base_config.get("filePickerEnabled", True),
            "backButtonPolicy": base_config.get("backButtonPolicy", "WEB_HISTORY_FIRST"),
            "splashConfiguration": base_config.get("splashConfiguration", {
                "backgroundColor": "#0B0F19",
                "iconAnimationDurationMs": 800
            }),
            "privacy": {
                "contactEmail": contact_email,
                "developerName": developer_name,
                "dataCollection": data_collection,
                "dataUse": privacy_map.get("dataUse", [
                    "Local application rendering and execution",
                    "On-device storage of user game states, preferences, and session data"
                ]),
                "thirdParties": third_parties,
                "retention": privacy_map.get("retention", "No personal data is collected or retained on remote servers by default. Locally stored web cache and preferences remain on the device until the user clears application storage or uninstalls the application."),
                "deletion": privacy_map.get("deletion", "Users can delete all local application data at any time through Android Settings > Apps > Storage > Clear Data, or by requesting support at the contact email address.")
            }
        }

        # Write to target app-config.json
        self.target_config_file.parent.mkdir(parents=True, exist_ok=True)
        with open(self.target_config_file, "w", encoding="utf-8") as fp:
            json.dump(final_config, fp, indent=2)
        log(f"Synchronized master configuration: {self.target_config_file.relative_to(self.root)}", "📄")

    def _deploy_to_assets(self, staging_dir: Path):
        if self.target_web_dir.exists():
            shutil.rmtree(self.target_web_dir)
        self.target_web_dir.mkdir(parents=True, exist_ok=True)
        shutil.copytree(staging_dir, self.target_web_dir, dirs_exist_ok=True)
        log(f"Copied {len(list(self.target_web_dir.rglob('*')))} assets into app/src/main/assets/web", "📦")

def main():
    discovery = UniversalDiscoveryEngine()
    discovery.run_discovery()

    builder = UniversalBuildAndPackageEngine(discovery)
    builder.process()

    # Pass icon path output for Gradle importIcon if found
    if discovery.discovered_icon_path:
        print(f"::DISCOVERED_ICON::{discovery.discovered_icon_path}")

if __name__ == "__main__":
    main()
