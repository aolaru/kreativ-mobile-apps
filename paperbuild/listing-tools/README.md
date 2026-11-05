# PaperBuild 1.2 listing captures

The capture test runs only through the optional Gradle init script. It does not
join or change the normal 24-test QA suite, production source or signed v6 AAB.
It uses the existing signed review grant, not a fake Google Play purchase.
All documents and the Studio North logo are fictional, authored in the fixture.

With the owner's unlocked phone available, run from `paperbuild/`:

```sh
bash scripts/capture-listing-screenshots.sh DEVICE_SERIAL
node promotion/2026-11-04-listing/build-assets.cjs
```

The script updates only `com.kreativ.paperbuild.v120.qa` and its test package.
It replaces this disposable app's PDF editor draft and adds the Studio branding
preset. It does not clear app data, use a camera, send files or make a purchase.
Never run capture commands concurrently with regression tests or another Gradle
variant. The normal regression command rebuilds its own test APK afterward.

Raw captures are in `store-assets/play/screenshots/1.2/raw/`. Panel 7 uses a
high-resolution render of the actual app-exported PDF instead of enlarging the
small on-screen preview. No app UI is reconstructed or repainted. Panels 1–5
reuse the reviewed, unchanged Free UI assets; panels 6 and 8 use fresh 1.2 UI.
The first five PNGs are committed baseline inputs, with original capture hashes
and crop coordinates in `promotion/2026-11-04-listing/baseline-assets.json`.

Capture run: 1 test passed on Samsung SM-A556B / Android 16. The exported sample
was independently reopened with pypdf and rendered with Poppler before use.
No generated PDF is a user deliverable; it remains an intermediate under `tmp/`.
