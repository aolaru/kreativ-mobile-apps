# PaperBuild 1.0 Release Checklist

## Device smoke test

- [ ] Install `app/build/outputs/apk/debug/app-debug.apk` on an Android 8 or newer phone.
- [ ] Capture one page, then capture a five-page document.
- [ ] Add two image files through the system picker, including one sideways photo.
- [ ] Confirm every thumbnail matches its page and rotation.
- [ ] Build A4 colour, A4 high-contrast, and US Letter grayscale PDFs.
- [ ] Open each PDF in an independent PDF viewer and inspect each page.
- [ ] Share a PDF to a messaging or email app.
- [ ] Save a PDF and find it in `Downloads/PaperBuild`.
- [ ] Deny storage permission on Android 8/9 and confirm the app explains why saving cannot continue.

## Store preparation

- [x] Final application ID: `com.kreativ.paperbuild`
- [x] Public privacy-policy URL prepared
- [x] Store listing copy prepared
- [ ] Create a real release keystore and local `release-signing.properties` from the included example.
- [ ] Build a signed App Bundle with `./gradlew bundleRelease`.
- [ ] Capture the four real-device screenshots described in `STORE_LISTING.md`.
- [ ] Complete Google Play content, target audience, data safety, and app access forms.
- [ ] Upload to an internal testing track and invite testers.
- [ ] Resolve tester feedback before creating a production release.

## Boundaries for version 1.0

- [x] No account, analytics, ads, or internet permission.
- [x] No misleading auto-crop, OCR, signing, redaction, or merge claims.
- [x] Pro features remain labelled as planned until they are implemented and tested.
