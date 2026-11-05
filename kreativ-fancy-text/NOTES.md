# First-edition decisions

The WordPress plugin and Chrome extension stay separate from this Android app.
No live website deployment is part of this implementation.

A design stores the source text, theme, base Unicode mapping, token-level style
and color overrides, image format, transparency, resolution, size, alignment,
and signature. Favorites store the full design, rather than a flattened PNG.

Word selection uses tap targets below the textarea. This avoids brittle
contenteditable selection on mobile keyboards while still supporting per-word
styling. Editing aligns unchanged tokens so their styling stays attached.

Theme selection supplies the shared text style, palette, and artwork; explicit
word overrides remain. To remove a word override, select it and use Reset
selected word. Whitespace is kept in the matching copyable text.

All displayed user text is inserted with textContent. Saved data is bounded and
validated before use. Native exports require no broad storage access. Sharing
uses a FileProvider restricted to the app's export cache. The test build has no
INTERNET permission; asset requests are intercepted by Capacitor's local server.

Future work intentionally outside this first edition: animated exports,
creator kits, custom font imports, a keyboard service, external app text-selection
integration, cloud sync, and store publication.

Native export fix in 0.1.1: Android saves a pending plugin call's options when
opening the document picker. Base64 image bytes exceeded the activity-state
transaction limit for a 2× transparent story PNG. The web layer now writes the
PNG to private cache before opening the picker. ExportFile receives only its
file URI and a filename (161 bytes in the phone test), validates the canonical
cache path and PNG signature, then streams to the selected document URI. Normal
completion and cancellation remove the temporary cache source.
