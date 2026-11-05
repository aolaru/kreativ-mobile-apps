#!/usr/bin/env python3
"""Prepare Amazon copy and original icons; never manufacture Fire screenshots."""
import hashlib
import json
import shutil
from pathlib import Path
from PIL import Image

project=Path(__file__).resolve().parents[1]
root=project/'store-assets/amazon/2026-10-26'
for d in ['icons','binaries','evidence']:(root/d).mkdir(parents=True,exist_ok=True)
old=json.loads((project/'store-assets/samsung/2026-10-25/listing.json').read_text())
description=old['description'].replace("No accounts, photo uploads, advertising SDKs, analytics SDKs or subscriptions. PhotoFeather has no internet permission. Android's system picker may retrieve photos from a cloud provider you choose; the provider handles that retrieval.","Photo processing stays on your device. No accounts, advertising, purchases or subscriptions. Our source app has no internet permission or analytics SDK. Amazon applies its own store wrapper and may process store-related diagnostics under its policies. The system picker may retrieve photos from a cloud provider you choose; that provider handles retrieval.")
meta={k:old[k] for k in ['title','packageName','versionName','versionCode','minimumAndroidApi','targetAndroidApi','price','ads','inAppPurchases','subscriptions','loginRequired','supportEmail','website','privacyPolicy','tutorial']}
meta.update(shortDescription='Compress and resize photos for forms, email and sharing. Check measured file sizes, compare before and after, and save new JPEG copies while keeping your originals.',longDescription=description,featureBullets=['Choose 200 KB, 500 KB, 1 MB or a custom file-size target','Resize one photo or a batch and preserve aspect ratio','Compare measured sizes and dimensions before saving','Save and share new JPEG copies without overwriting originals','Process photos on-device, with no ads, account or purchases'],keywords=['photo compressor','resize photos','reduce image size','JPEG','offline','file size'],language='English',category='Photo & Video',refinements=['Photo Editing','Photo Sharing'],devices='Fire tablets only; exclude Fire TV and Android phones. minSdk 26 excludes Fire OS 6 (API 25) and older. Verify Fire OS 7/API 28 and Fire OS 8/API 30 in portal.',drmRecommendation='No automatically applied DRM for the free offline utility. Amazon still wraps/re-signs the APK.',ageRating='Questionnaire pending; do not assume certified rating',status='Draft created; not submitted',appId='amzn1.devportal.mobileapp.3120e04df3c74381a4db5e3616141a76',releaseId='amzn1.devportal.apprelease.06d7d75970e04242a5422cb09001a229')
assert len(description)<=4000 and len(meta['shortDescription'].encode())<=2000 and 3<=len(meta['featureBullets'])<=5
(root/'listing.json').write_text(json.dumps(meta,indent=2)+'\n')
for k,f in [('title','01-title.txt'),('shortDescription','02-short-description.txt'),('longDescription','03-full-description.txt')]: (root/f).write_text(meta[k]+'\n')
(root/'04-feature-bullets.txt').write_text('\n'.join(meta['featureBullets'])+'\n')
source=project/'promotion/2026-10-25/sources/existing-icon-512.png'
shutil.copy2(source,root/'icons/icon-512.png')
with Image.open(source) as img:
 assert img.size==(512,512)
 img.convert('RGBA').resize((114,114),Image.Resampling.LANCZOS).save(root/'icons/icon-114.png')
binary=project/'app/build/outputs/apk/release/app-release-unsigned.apk'
shutil.copy2(binary,root/'binaries/PhotoFeather-1.0.0-code3-UNSIGNED.apk')
(root/'FIRE-TEST-PLAN.txt').write_text('''PENDING — FIRE TABLET COMPATIBILITY AND SCREENSHOTS

Existing Android 9 and Galaxy A55 tests are supporting evidence, not Fire testing.
Test the store-processed APK on Fire OS 7 and Fire OS 8 tablets, or limit targeting
to verified devices. minSdk 26 excludes Fire OS 6/API 25 and older. No Fire TV.

1. Install, cold launch, offline relaunch; no account/Google services required.
2. Choose photos through the real system document picker: JPEG, PNG, static WebP,
   multi-select, cancellation, unreadable/unsupported inputs. Test with Wi-Fi off.
3. Compress to 200 KB/500 KB/1 MB; confirm measured file size and final dimensions.
4. Resize-only, custom settings, batch, cancellation and retry.
5. Before/after compare: portrait/landscape, zoom/pan, large text, dark mode.
6. Fire OS 7/API 28: real folder chooser, create JPEG, reopen and verify saved bytes.
   Fire OS 8/API 30: MediaStore gallery save and reopening. Originals unchanged.
7. Real share sheet to a local receiving app; cancel cleanly; no unintended sending.
8. Rotate during processing; background/restore; low-memory and storage errors.
9. Save 3–10 genuine Fire tablet screenshots of home, settings, results/compare.
   Allowed dimensions include 800x1280, 1200x1920, 1600x2560 or landscape reverse.
   Capture at an accepted native tablet resolution; do not repaint Android phone
   captures into a Fire experience. Current Play phone graphics are not included
   as Amazon screenshots. No AI generation or AI marks.
10. Recheck permissions, Amazon wrapper behavior, privacy text and labels against
    the downloaded Amazon-processed APK before final publication.

If no physical Fire tablet is available, check this account's Appstore Quality
Central device access. Physical remote access is restricted to selected partners.
Do not pay for hardware/testing without a separately agreed budget.
''')
(root/'REVIEWER-NOTES.txt').write_text('''PhotoFeather 1.0.0 (3), com.kreativ.photofeather. Free, no purchases, ads or login.
Select JPEG/PNG/static WebP using Choose photos. Choose Resize or Maximum file
size, process, inspect measured results, use Compare before & after, then save
or share new JPEG copies. Output is lossy. Unsupported inputs and unmet targets
are identified. Originals are never overwritten. On older Android-based Fire OS,
use the folder chooser to save. Use only non-sensitive demo photos for testing.

Fire compatibility and tablet screenshots are pending; this pack alone is not
a declaration that the release has passed Amazon review.
''')
(root/'PRIVACY-AND-SIGNING.txt').write_text('''Developer source: no INTERNET permission, ads, analytics, Google services, IAP,
account or native libraries. All photo processing is local. User-initiated share
and cloud-provider selection follow their destination/provider's policies.

Amazon removes the developer signature, applies its account certificate and wraps
the app, including store analytics, even if automatic DRM is off. Official Amazon
documentation says signed or unsigned APKs can be submitted. Do not create a new
production key just for this pilot. Record the Amazon certificate after upload.
The Amazon certificate normally differs from Google Play's signing certificate:
same-package cross-store replacements may be blocked. No package change made.

Privacy questionnaire must be completed before submission. Proposed developer-
source answer is no developer data collection/transfer, subject to current form
definitions. Verify Amazon wrapper disclosures and processed binary first. Do not
promise zero networking/zero telemetry for Amazon's processed edition.
Current published policy describes the original edition; add the Amazon store
wrapper distinction before Amazon publication. No home-address changes included.

Sources, checked 26 October 2026:
https://developer.amazon.com/docs/app-submission/understanding-submission.html
https://developer.amazon.com/docs/in-app-purchasing/ja-iap-get-started.html
https://developer.amazon.com/docs/app-submission/appstore-details.html
https://developer.amazon.com/docs/app-submission/device-filtering-and-compatibility.html
https://developer.amazon.com/docs/app-submission/appstore-privacy-labels.html
https://developer.amazon.com/docs/app-testing/live-device-interaction-physical.html
''')
(root/'START-HERE.txt').write_text('''PHOTOFEATHER AMAZON FIRE-TABLET PILOT — 26 OCTOBER 2026

Existing Amazon developer account signed in. PhotoFeather draft created under
Photo & Video > Photo Editing, Photo Sharing. Public contact is
info@madebykreativ.com and the dedicated app page; legacy phone omitted.
See portal-status.json for subsequent saved fields/uploads, never infer success
from preparation files alone. No review submission or publication yet.

Prepared: Amazon short/full copy, five feature bullets, keywords, original 512px
and deterministically scaled 114px icons, existing universal unsigned APK,
Fire test plan, reviewer steps, signing/wrapper/privacy notes, hashes.

Still needed: verified Fire-tablet workflow, genuine tablet screenshots, final
device targeting/content/privacy questionnaires, policy wrapper clarification,
and review of the Amazon-processed binary. No AI assets or marks added.
''')
manifest={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(root.rglob('*')) if p.is_file() and p.name!='SHA256SUMS.json'}
(root/'SHA256SUMS.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps({'pack':str(root),'longCharacters':len(description),'screenshots':0,'signed':False,'submission':'not submitted'},indent=2))
