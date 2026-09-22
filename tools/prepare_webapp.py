#!/usr/bin/env python3
"""
Volkan Web2Android - Static Web App Validation and Packaging Tool
Validates production static build integrity and packages into Android assets.
"""

import sys
import os
import shutil
import re

def validate_and_package(source_dir, dest_dir):
    print("=" * 60)
    print("⚡ VOLKAN WEB2ANDROID - WEBAPP PACKAGING PIPELINE")
    print("=" * 60)

    if not os.path.exists(source_dir):
        print(f"❌ Error: Source directory does not exist: {source_dir}")
        return False

    index_html = os.path.join(source_dir, "index.html")
    if not os.path.exists(index_html):
        print(f"❌ Error: 'index.html' not found in source directory: {source_dir}")
        return False

    print(f"📁 Source directory: {source_dir}")
    print(f"🎯 Target directory: {dest_dir}")

    # Inspect files and run sanity checks
    warnings = []
    file_count = 0
    total_size = 0

    suspicious_patterns = [
        (re.compile(r"http://localhost(:\d+)?", re.IGNORECASE), "Detected localhost reference"),
        (re.compile(r"http://127\.0\.0\.1(:\d+)?", re.IGNORECASE), "Detected 127.0.0.1 reference"),
        (re.compile(r"file:///", re.IGNORECASE), "Detected absolute file:/// path"),
        (re.compile(r"webpack-dev-server|vite/client", re.IGNORECASE), "Detected development server bundle artifacts")
    ]

    for root, _, files in os.walk(source_dir):
        for f in files:
            file_count += 1
            file_path = os.path.join(root, f)
            rel_path = os.path.relpath(file_path, source_dir)
            size = os.path.getsize(file_path)
            total_size += size

            # Check text files for broken dev patterns
            if f.endswith(('.html', '.js', '.css', '.json')):
                try:
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as fp:
                        content = fp.read()
                        for pat, desc in suspicious_patterns:
                            if pat.search(content):
                                warnings.append(f"[{rel_path}] {desc}")
                except Exception as e:
                    pass

    print(f"📊 Validated {file_count} files ({total_size / 1024:.2f} KB total).")

    if warnings:
        print("\n⚠️ WARNINGS DETECTED (Review recommended):")
        for w in warnings[:10]:
            print(f"  - {w}")
        if len(warnings) > 10:
            print(f"  ... and {len(warnings) - 10} more warnings.")
    else:
        print("✅ Zero critical development server artifacts detected.")

    # Clean target directory and copy
    print("\n📦 Copying assets to Android asset directory...")
    if os.path.exists(dest_dir):
        shutil.rmtree(dest_dir)
    os.makedirs(dest_dir, exist_ok=True)

    for item in os.listdir(source_dir):
        s = os.path.join(source_dir, item)
        d = os.path.join(dest_dir, item)
        if os.path.isdir(s):
            shutil.copytree(s, d)
        else:
            shutil.copy2(s, d)

    print("✅ Packaging complete! Assets are ready for Android build.")
    print("=" * 60)
    return True

if __name__ == "__main__":
    src = sys.argv[1] if len(sys.argv) > 1 else "webapp_input"
    dst = sys.argv[2] if len(sys.argv) > 2 else "app/src/main/assets/web"
    success = validate_and_package(src, dst)
    sys.exit(0 if success else 1)
