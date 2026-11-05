# Changes

## 1.0.0 — release candidate prepared 24 October 2026

Resumed Play preparation after moving to Utility apps. Added a full local privacy
policy and support contact, read the displayed version from package metadata,
and explicitly excluded Android backup/device-transfer storage. Completed the
signed release build, packaged-artifact verifier, reproducible store graphics,
listing copy, and public-policy HTML sources. Phone validation of the shrunk
release and Play Console submission remain pending; this is not a published release.

## 0.1.1 — 23 October 2026

Fixed a high-resolution PNG save crash found while testing on a Samsung Galaxy
A55 running Android 16. Opening Android's document picker previously saved the
base64 image payload twice in activity state, causing TransactionTooLargeException.
PNG exports now stage their bytes in private cache and pass only a small file
URI to the pending picker call. The native plugin validates the source and
streams it to the chosen document, then removes the temporary file.

Physical-phone checks passed for text/image share previews and cancellation,
standard PNG saving, 2160 × 3840 transparent PNG saving, save cancellation,
temporary-file cleanup, and native draft/favorite restoration after restart.
The update preserved the existing saved design. All 13 model/browser checks pass.

## 0.1.0 — 23 October 2026

First Android test edition: matching Unicode text and image designs, word-level
style/color overrides, six themes, transparent and high-resolution PNG exports,
offline fonts and processing, local favorites, and native sharing/file saving.
