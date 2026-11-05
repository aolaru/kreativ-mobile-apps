# Kreativ Post

An original, on-device Android promotion editor for cafés and bakeries. Working name.

**Your photo, your words, your brand — ready to post.**

## Run

Open this directory in Android Studio. Requires Android SDK 36 and JDK 17 or newer supported by Gradle 8.7 (the Android Studio bundled JDK works). Android 9 / API 28 minimum.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest
./gradlew :app:lintDebug :app:assembleRelease
```

The debug application ID is `com.kreativ.post.debug`. Release builds are unsigned and unpublished. No external accounts, store listings or releases are created by this project.

## Use

1. Choose one of five original templates: Oven Notes, Daily Pour, Weekend Table, The New Favourite, Open Doors.
2. Edit Words. Import photos and an optional logo using Photos. The first photo is used for static designs.
3. Crop with zoom and horizontal/vertical positioning. Use Layout to move, resize, align and space each element; drag handles and snapping guides are also available. All changes support undo/redo within the current editing session.
4. Edit Brand, save one profile, then reuse business information, colours, headline font and logo. New projects apply that profile automatically. Body text uses DM Sans for consistency.
5. Switch Square / Story. Each format has its own geometry and crop, while wording and brand stay coordinated.
6. Export PNG or JPEG through the system save picker. The editable project autosaves locally; Projects lists saved designs, supports reopening and duplication. Explicit Save is also available. Reopen tomorrow and change the price or photo.

The preview includes selection outlines; those and alignment guides are excluded from exports. Text fits automatically, with warnings for very small or clipped text and low text/background contrast. Review photo overlaps after manual layout changes.

## Scope

All initial static editing and export features are free and watermark-free. No ads, Internet permission, telemetry, login, billing, AI or hidden upload. The initial editor has six fixed editable element types rather than unrestricted layers. Fonts are Lora and DM Sans, included under SIL OFL 1.1 with notices bundled and available from the app's Privacy & licences screen.

Imported originals are copied unchanged to private storage; edit/export uses decoded sRGB working copies. Removing an image from a project does not destroy its managed original (which may be referenced by undo, another project or the brand). Uninstalling deletes private projects and media. Exported images are not editable backups. Editable project interchange and media garbage collection are deferred.

See [architecture](docs/ARCHITECTURE.md), [validation](docs/VALIDATION.md), [motion prototype](docs/MOTION.md), and [asset notices](app/src/main/assets/licenses/SOURCES.txt).
