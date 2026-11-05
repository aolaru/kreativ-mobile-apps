#!/usr/bin/env python3
"""Package the existing free release and approved artwork for Samsung review.

Does not sign binaries, upload files, register accounts, or submit an app.
Requires Pillow for image validation. Re-run after a production assembleRelease
and bundleRelease; QA/debug artifacts are intentionally never selected.
"""
import hashlib
import html
import json
import shutil
import zipfile
from pathlib import Path

from PIL import Image

project = Path(__file__).resolve().parents[1]
root = project / "store-assets/samsung/2026-10-25"
root.mkdir(parents=True, exist_ok=True)
for directory in ["screenshots", "binaries", "evidence"]:
    (root / directory).mkdir(exist_ok=True)
approved = json.loads((project.parent / "review/play-listing-refresh-2026-10-25/prepared-copy.json").read_text())["photofeather"]
metadata = {
    "title": approved["title"],
    "shortDescription": "Compress and resize photos offline",
    "description": approved["fullDescription"] + "\n\nPrivacy: https://madebykreativ.com/privacy/photofeather/",
    "defaultLanguage": "English",
    "defaultLanguageCode": "ENG",
    "packageName": "com.kreativ.photofeather",
    "versionName": "1.0.0",
    "versionCode": 3,
    "minimumAndroidApi": 26,
    "targetAndroidApi": 36,
    "price": "Free",
    "ads": False,
    "inAppPurchases": False,
    "subscriptions": False,
    "loginRequired": False,
    "supportEmail": "info@madebykreativ.com",
    "website": "https://madebykreativ.com/apps/photofeather/",
    "privacyPolicy": "https://madebykreativ.com/privacy/photofeather/",
    "tutorial": "https://madebykreativ.com/news/compress-photos-android-photofeather/",
    "categorySuggestion": "Photo / Photography (confirm available portal label)",
    "keywordsSuggested": ["photo compressor", "resize photos", "reduce photo size", "JPEG", "offline"],
    "releaseNotes": "First Galaxy Store release. Compress and resize photos on-device, compare measured results, and save or share new JPEG copies. All features are free, with no ads or account.",
    "ageRating": "Complete Samsung's questionnaire; not certified in this pack",
    "countries": "Not selected; confirm initial launch countries in Seller Portal",
    "publication": "Preparation only; no review submission or publication performed",
}
assert len(metadata["shortDescription"].encode()) <= 40
assert len(metadata["description"].encode()) <= 4000
(root / "listing.json").write_text(json.dumps(metadata, indent=2) + "\n")
for key, filename in [("title", "01-title.txt"), ("shortDescription", "02-short-description.txt"), ("description", "03-full-description.txt"), ("releaseNotes", "04-release-notes.txt")]:
    (root / filename).write_text(metadata[key] + "\n")

promotion = project / "promotion/2026-10-25"
shutil.copy2(promotion / "sources/existing-icon-512.png", root / "icon-512.png")
shots = sorted((promotion / "listing/screenshots").glob("*.png"))
assert 4 <= len(shots) <= 8
checks = []
for source in shots:
    destination = root / "screenshots" / source.name
    shutil.copy2(source, destination)
    with Image.open(destination) as image:
        w, h = image.size
        assert 320 <= min(w, h) and max(w, h) <= 3840 and max(w, h) / min(w, h) <= 2
        assert image.mode == "RGB", (source.name, image.mode)
        checks.append({"file": str(destination.relative_to(root)), "width": w, "height": h, "mode": image.mode})
with Image.open(root / "icon-512.png") as icon:
    assert icon.size == (512, 512)
assert (root / "icon-512.png").stat().st_size <= 1024 * 1024

artifacts = [
    (project / "app/build/outputs/apk/release/app-release-unsigned.apk", root / "binaries/PhotoFeather-1.0.0-code3-UNSIGNED.apk"),
    (project / "app/build/outputs/bundle/release/app-release.aab", root / "binaries/PhotoFeather-1.0.0-code3-UNSIGNED.aab"),
]
for source, destination in artifacts:
    with zipfile.ZipFile(source) as archive:
        names = archive.namelist()
        assert not any(name.endswith(".so") for name in names), "Native libraries need separate page-size verification"
        assert not any(name.upper().startswith("META-INF/") and name.upper().endswith((".RSA", ".DSA", ".EC", ".SF")) for name in names), "Unexpected JAR signature; inspect signing provenance"
        assert not any("google/android/gms" in name for name in names)
    shutil.copy2(source, destination)

validation = {
    "date": "2026-10-25",
    "shortDescriptionUtf8Bytes": len(metadata["shortDescription"].encode()),
    "descriptionUtf8Bytes": len(metadata["description"].encode()),
    "screenshots": checks,
    "icon": {"width": 512, "height": 512, "format": "PNG", "bytes": (root / "icon-512.png").stat().st_size},
    "aiGeneratedImagery": False,
    "provenance": "Existing app icon and the six approved Play listing graphics. Genuine app UI from promotion/2026-10-25 sources; no new image generation, no UI repainting, no AI marks added.",
    "nativeLibrariesInBinaries": 0,
    "productionSigning": "Missing: APK and AAB are unsigned preparation artifacts, not upload-ready signed releases",
    "runtimeSourceChanged": False,
    "requirements": "https://developer.samsung.com/galaxy-store/galaxy-store-developer-api/content-publish-api/reference.html",
}
(root / "validation.json").write_text(json.dumps(validation, indent=2) + "\n")
cards = "".join(f'<figure><img src="screenshots/{html.escape(p.name)}" alt="PhotoFeather screenshot {i + 1}"><figcaption>{html.escape(p.name)}</figcaption></figure>' for i, p in enumerate(shots))
preview = '''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>PhotoFeather · Samsung submission pack</title><style>body{margin:0;background:#f2f7f4;color:#173d34;font:17px/1.6 system-ui}main{max-width:1100px;margin:auto;padding:36px 24px}header{display:flex;gap:24px;align-items:center}header img{width:96px;height:96px}.notice{padding:16px 20px;background:#fff3d7;border-left:4px solid #bc7d16}.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}figure{margin:0}figure img{width:100%;height:auto}figcaption{font-size:12px;overflow-wrap:anywhere}pre{white-space:pre-wrap;font:16px/1.6 system-ui}a{color:#17634d}@media(max-width:650px){.grid{grid-template-columns:repeat(2,1fr)}header{align-items:flex-start}}</style><main><header><img src="icon-512.png" alt="PhotoFeather icon"><div><p>MADE BY KREATIV · SAMSUNG GALAXY STORE</p><h1>PhotoFeather: Photo Compressor</h1><p>Compress and resize photos offline</p></div></header><p class="notice"><strong>Prepared, not submitted.</strong> Free edition · no ads · no purchases. Production signing and Samsung seller approval remain release gates. Binaries labelled UNSIGNED must not be uploaded as signed APKs.</p><p><a href="START-HERE.txt">Submission handoff</a> · <a href="listing.json">All listing fields</a> · <a href="REVIEWER-NOTES.txt">Reviewer instructions</a> · <a href="validation.json">Asset validation</a></p><h2>Six genuine screenshot-based graphics · no AI marks</h2><div class="grid">''' + cards + '''</div><h2>Full description</h2><pre>''' + html.escape(metadata["description"]) + '''</pre><h2>Support and privacy</h2><p><a href="https://madebykreativ.com/apps/photofeather/">App website</a> · <a href="https://madebykreativ.com/privacy/photofeather/">Privacy policy</a> · info@madebykreativ.com</p></main></html>'''
(root / "preview.html").write_text(preview)
manifest = []
for file in sorted(root.rglob("*")):
    if file.is_file() and file.name != "SHA256SUMS.txt":
        manifest.append(hashlib.sha256(file.read_bytes()).hexdigest() + "  " + str(file.relative_to(root)))
(root / "SHA256SUMS.txt").write_text("\n".join(manifest) + "\n")
print(json.dumps({"pack": str(root), "screenshots": len(shots), "shortBytes": validation["shortDescriptionUtf8Bytes"], "descriptionBytes": validation["descriptionUtf8Bytes"], "signed": False}, indent=2))
