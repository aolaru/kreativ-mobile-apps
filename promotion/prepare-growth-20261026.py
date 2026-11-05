#!/usr/bin/env python3
"""Prepare a small organic campaign using existing approved assets."""
import hashlib
import html
import json
from datetime import date, timedelta
from pathlib import Path
from urllib.parse import urlencode

base = Path(__file__).resolve().parents[1]
out = base / "promotion/2026-10-26"
out.mkdir(parents=True, exist_ok=True)
campaign = "kreativ_growth_20261026"
apps = [
    ("photofeather", "PhotoFeather", "com.kreativ.photofeather", "photofeather/promotion/2026-10-25", "compress-photos-android-photofeather", "Photo too large for a form or email?", "Choose a file-size target or resize setting, compare the measured result, then save a new JPEG copy. Your originals stay untouched. All processing is on your device, with no account, ads or purchases. JPEG output is lossy; quality and savings vary, and some targets cannot be met.", 0, None),
    ("sharesafe", "ShareSafe", "com.kreativ.sharesafe", "sharesafe/promotion/2026-10-17", "remove-photo-metadata-android-sharesafe", "Check what travels with your photo.", "ShareSafe creates new JPEG copies with embedded metadata removed. Cleaning and selected EXIF checks are free; Pro editing tools are optional. Photo processing stays on-device. Visible faces, addresses and text still need your review. The Play edition includes ML Kit diagnostics; photos are not uploaded to the developer.", 2, 1),
    ("paperbuild", "PaperBuild", "com.kreativ.paperbuild", "paperbuild/promotion/2026-10-18", "photos-to-pdf-android-paperbuild", "A few page photos. One PDF.", "Select photos, crop and arrange the pages, then export a PDF on your Android phone. The free version supports up to 10 pages; optional one-time Pro adds more tools. Documents stay on-device, with no account or ads.", 21, 33),
    ("northlight", "Northlight", "com.kreativ.northlight", "northlight/promotion/2026-10-17", "offline-compass-android-northlight", "A clear compass heading, even offline.", "Northlight combines a magnetic compass, spoken headings and practical light tools. Core features are free, with optional one-time Pro tools. Compass accuracy depends on your phone and nearby interference; it is not route navigation. No account or ads.", 12, 25),
    ("kreativ-fancy-text", "Kreativ Fancy Text", "com.kreativfont.studio", "kreativ-fancy-text/promotion/2026-10-25", "fancy-text-png-designs-android-kreativ", "Make the words look like you.", "Style text to copy and paste, or export matching PNG artwork. Create on your Android phone, offline, with no account, ads or purchases. Copied text depends on the receiving app; use PNG to preserve the artwork. This does not change your phone's system font.", 0, 0),
    ("kreativ-font", "Kreativ Font", "com.kreativ.fontapp", "kreativfont/promotion/2026-10-18", "discover-fonts-android-kreativ-font", "Build a better font shortlist.", "Browse the Kreativ Font catalogue, inspect previews and keep favorites on your Android phone. This is a font discovery app, not a phone-font installer. Browsing needs internet; linked fonts have their own prices and licences.", 18, 3),
]
sources = ["linkedin", "facebook", "instagram", "youtube"]
records = []
for slug, name, package, promo, tutorial, hook, body, audience, acquisition in apps:
    graphic = base / promo / "social/01.png"
    assert graphic.is_file()
    videos = sorted((base / promo).glob("video/*.mp4")) or sorted((base / promo).glob("demo/*.mp4"))
    channels = {}
    for source in sources:
        tags = dict(utm_source=source, utm_medium="organic_social", utm_campaign=campaign, utm_content=slug)
        play = "https://play.google.com/store/apps/details?" + urlencode(dict(id=package, **tags, referrer=urlencode(tags)))
        guide = f"https://madebykreativ.com/news/{tutorial}/?" + urlencode(tags)
        channels[source] = {"play": play, "tutorial": guide}
    records.append(dict(slug=slug, name=name, package=package, graphic=str(graphic), video=str(videos[0]) if videos else None, hook=hook, body=body, channels=channels, graphicSha256=hashlib.sha256(graphic.read_bytes()).hexdigest(), status="prepared_unpublished"))

(out / "campaign.json").write_text(json.dumps({"prepared": "2026-10-26", "campaign": campaign, "start": None, "paidSpend": 0, "apps": records}, indent=2) + "\n")
baseline = dict(captured="2026-10-26", source="Signed-in Play Console home; evidence/play-home.txt and photofeather-dashboard.txt", homeWindow="Home cards compare with previous 30 days; exact date endpoints not displayed. These snapshots are not a fixed-period campaign report.", apps=[dict(slug=a[0], installedAudienceHomeSnapshot=a[-2], userAcquisitionHomeSnapshot=a[-1], note="Detailed reports not yet available; acquisition is unknown, not zero." if a[0]=="photofeather" else None) for a in apps], fontGooglePlayRatingHome=2.348, campaignVisitors=None, campaignInstallClicks=None, campaignAcquisitions=None)
(out / "baseline.json").write_text(json.dumps(baseline, indent=2) + "\n")
schedule = [(1,"photofeather","linkedin","File-size problem: graphic 01"),(3,"sharesafe","facebook","Photo metadata: graphic 01"),(5,"paperbuild","linkedin","Photo-to-PDF workflow: graphic 01"),(7,"photofeather","youtube","Existing screenshot tour; label it as screenshot footage"),(9,"kreativ-fancy-text","facebook","Text versus PNG: graphic 01"),(11,"northlight","linkedin","Offline compass: graphic 01"),(13,"kreativ-font","facebook","Discovery, not system-font installation; first review older low ratings")]
(out / "schedule.json").write_text(json.dumps({"dates": "Relative to the first actual publication; no posts scheduled or sent", "posts": [dict(day=d, app=a, channel=c, creative=x, status="draft") for d,a,c,x in schedule]}, indent=2)+"\n")
captions=[]
for app in records:
    captions.append(f"{app['name']} — GRAPHIC 01\n\n{app['hook']}\n\nI built {app['name']} for Android. {app['body']}\n\nTry it and tell me which step could be clearer.\n\n— Andrei, Made by Kreativ\n\n" + "\n\n".join(f"{s.title()} direct Play link:\n{app['channels'][s]['play']}\n{s.title()} tutorial link (use one destination per post):\n{app['channels'][s]['tutorial']}" for s in sources))
(out / "READY-TO-POST.txt").write_text("UNPUBLISHED — CHOOSE THE OWNED ACCOUNT BEFORE POSTING\nNo emails, direct messages or community outreach authorized by this pack.\n\n" + "\n\n".join(captions)+"\n")
(out / "MEASUREMENT.txt").write_text("""TWO-WEEK ORGANIC PILOT — no paid spend, no in-app analytics added

The start is the first real post, not the preparation date. Log publication URL,
time, account, channel, creative and the exact tagged link in campaign.json.
Use the account you own; check community promotion rules before any community post.

Starting evidence: baseline.json contains only observed Console home snapshots.
Installed audience is not cumulative downloads, and is not an acquisition total.
PhotoFeather detailed reporting is not ready; missing metrics remain null.
Kreativ Font's displayed rating is 2.348: review the low-rating themes before
spending promotion time on that app. No rating requests or incentives are planned.

Day 7 and day 14: export equal fixed date ranges in the same Console timezone.
Grow users > Store performance > Store analysis / Conversion analysis:
filter Ads and referrals and use UTM source / UTM campaign kreativ_growth_20261026.
Current store reports emphasize visitors, install clicks and click-through rate.
A store install click is not a completed installation.
Use Statistics / export reports for actual acquisitions separately. Low-volume
campaigns can appear under Other or be unavailable; do not fill those gaps with zero.

Website: existing Plausible App Store Click events record outbound clicks, not
downloads. Confirm that this custom event exists as a goal in the analytics account
before relying on its dashboard. Analytics account access has not been verified.
The campaign code forwards only allowlisted source/campaign labels to Play.
Cloudflare page views do not prove either store visits or completed downloads.

At day 14: compare reach -> website visits -> outbound clicks -> Play visitors ->
install clicks -> acquisitions, keeping each measure and date window separate.
Use findings to choose the next creative. Small counts are exploratory; don't
claim a winner or spend on ads without a repeatable signal.
No automatic checks, reminders or future publication have been scheduled.

Google's metric definitions:
https://support.google.com/googleplay/android-developer/answer/9859173?hl=en
""")
cards=[]
for app in records:
    links=" · ".join(f'<a href="{html.escape(app["channels"][s]["play"],quote=True)}">{s.title()} Play link</a>' for s in sources)
    cards.append(f'<article><h2>{html.escape(app["name"])}</h2><a href="{Path(app["graphic"]).as_uri()}"><img src="{Path(app["graphic"]).as_uri()}" alt="{html.escape(app["name"])} approved promotional graphic"></a><h3>{html.escape(app["hook"])}</h3><p>{html.escape(app["body"])}</p><p>{links}</p></article>')
(out / "index.html").write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Kreativ apps · organic promotion pilot</title><style>body{font:17px/1.6 system-ui;background:#f3f6fb;color:#18283e;margin:0}main{max-width:1100px;margin:auto;padding:32px 24px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(270px,1fr));gap:24px}article{background:white;border:1px solid #d6dfeb;border-radius:16px;padding:20px}img{width:100%;height:auto}a{color:#2153ad}h1{font-size:36px;line-height:1.2}aside{padding:18px;background:#fff0c8;margin:24px 0}</style><main><h1>Six useful apps. One measurable pilot.</h1><p>Prepared 26 October 2026 · €0 ad budget · Existing genuine app graphics · No AI marks</p><aside><strong>Prepared, unpublished.</strong> The two-week window starts with the first actual post. Amazon is a separate Fire-tablet pilot; it is not live.</aside><p><a href="READY-TO-POST.txt">Captions and links</a> · <a href="MEASUREMENT.txt">Measurement instructions</a> · <a href="baseline.json">Observed baseline</a> · <a href="schedule.json">Suggested posting order</a></p><div class="grid">'+"".join(cards)+'</div></main></html>')
(out / "START-HERE.txt").write_text("KREATIV APP GROWTH PILOT — 26 OCTOBER 2026\n\nPrepared six posts with existing approved graphics/videos and distinct links for LinkedIn, Facebook, Instagram and YouTube. See index.html and READY-TO-POST.txt. No social posts sent or scheduled. Pick the first owned account before publication.\n\nBaseline captured from the signed-in Google Play Console. See baseline.json, evidence and MEASUREMENT.txt. Detailed PhotoFeather reporting is pending.\n\nWebsite work: related-app links and campaign preservation are being prepared separately in madebykreativ.com. See website-status.json for actual deployment state.\n\nAmazon: PhotoFeather draft created in the existing developer account. See photofeather/store-assets/amazon/2026-10-26 for exact status and remaining Fire-device requirements.\n")
print(json.dumps({"folder":str(out),"apps":len(records),"channelLinks":len(records)*len(sources)*2,"postsSent":0},indent=2))
