# Temporary instrumentation for the exact signed release

This helper is separate from the shipping APK/AAB. It uses Android instrumentation
with the same upload certificate, so a non-debuggable release can be tested without
changing its code or enabling DevTools. Its dynamic command receiver requires the
sender to hold Android's DUMP permission (the adb shell does).

Select the user-authorized device explicitly. A differently signed debug install
cannot be updated with the release. Before any approved replacement, back up its
installed APK and complete app data, verify the backup, and retain a rollback path.
Do not uninstall merely to bypass a signing conflict without preserving designs.

Build from the app directory:

```sh
node scripts/build-release-harness.mjs PATH_TO_PRIVATE_BACKUP/CapacitorStorage.xml
adb -s AUTHORIZED_SERIAL install -r .local/release-harness/release-checks.apk
adb -s AUTHORIZED_SERIAL shell am instrument -w -e restoreBefore true com.kreativfont.studio.releasechecks/com.kreativfont.studio.releasechecks.ReleaseChecks
```

The final command waits while tests run. In another terminal:

```sh
python3 scripts/release-device.py AUTHORIZED_SERIAL info
printf 'document.title' | python3 scripts/release-device.py AUTHORIZED_SERIAL eval
python3 scripts/release-device.py AUTHORIZED_SERIAL finish
```

`restoreBefore true` restores the snapshot before the Activity/WebView opens.
Restore at this startup boundary, not while a fixture design is live: a live
page can otherwise overwrite restored preferences when it backgrounds. Finish
and force-stop the test process, then start fresh with `restoreBefore true` to
restore original designs after testing. Verify preference value hashes against
the backup, finish the helper, and relaunch the normal app to check the result.

The harness APK contains the private saved-design snapshot. Keep all generated
files in ignored, owner-only `.local/release-harness/`; never publish or commit
that APK. Uninstall `com.kreativfont.studio.releasechecks` after testing, remove
only test-generated exports, and restore any temporary device settings. Never
select a share recipient unless the user has explicitly authorized sending.
