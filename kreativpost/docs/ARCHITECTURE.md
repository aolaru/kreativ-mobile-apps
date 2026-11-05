# Architecture and decisions

## Workspace assessment

Inspected the utility-app workspace and ancestor repository instruction locations before implementation; no applicable AGENTS.md and no Photo App project were found. ShareSafe, Northlight, PaperBuild and Kreativ Font are independent Android apps. Existing modified and untracked work is left untouched, including the root README.

ShareSafe's ImageProcessor was assessed: bounded decoding, source preservation and explicit export copies are useful patterns. Its JPEG alpha-flattening and EXIF-specific pipeline were unsuitable for transparent logos. Kreativ Post instead uses Android ImageDecoder, orientation-aware decode, a software allocator and private originals. Only generic Gradle wrapper/bootstrap configuration is reused; no store identity, billing or branded product assets are copied.

## Layers

- `Project`: versioned explicit JSON schema. Shared words/media/brand, independent square and vertical normalized element geometry, crop, alignment, text colour and spacing. Unknown versions are rejected without rewriting files.
- `Templates`: five original art-directed compositions, with separate format layouts. They are arrangements of editable photo, headline, description, price, business footer and logo elements.
- `Geometry`: density-independent bounds, cover-crop positioning, centre/margin snapping.
- `History`: bounded 60-snapshot undo/redo, including geometry/media/text. A new edit invalidates redo; history is session-only.
- `ProjectStore` and `Brand`: AtomicFile writes, project IDs and controlled private media references. Duplication gives a new identity while safely sharing immutable media. Autosave and lifecycle save; the last project is recovered on launch.
- `MediaLibrary`: byte-for-byte import, validation of JPEG/PNG/WebP, max 40 MB source size, max decoded dimension 2048 and pixel budget 4 MP. A 24 MB LRU stores working bitmaps. Software sRGB decoding handles orientation and alpha; originals are never rewritten.
- `DesignRenderer`: one canonical Canvas scene, 1080×1080 or 1080×1920. StaticLayout handles Unicode, wrapping, alignment, tracking and leading. Bisection fits text to geometry; warnings identify clipping/small type and contrast. Preview scales the entire scene instead of recomputing text at screen density. Export renders that same scene at full resolution.
- `MainActivity`/`EditorCanvas`: native accessible controls, scrollable phone/tablet layout, gestures plus labeled position/crop controls, system document pickers. No broad media/storage permission. Worker-thread import and export disable edits during operations. Cancellation and failures preserve project state.
- `StaticExport`: opaque PNG / 95-quality JPEG. Render to a private temporary file before destination selection. Missing media blocks exports; failed destination writes attempt to remove partial documents, with a useful retry message.
- `MotionExport`: isolated codec feasibility experiment, using a snapshot of shared content and one original story layout.

## Assumptions and risks

Android API 28+ allows the platform image decoder and modern text layout. Initial scope is still photos, not short video import. Display colour management differs between devices; inputs are converted to sRGB, and JPEG is lossy. Preview and exports have identical scene geometry but screen resampling/colour can affect pixels seen on the display.

Saved originals cost storage; no silent cleanup destroys project media. A failed import may leave an unreferenced private copy if the app is killed after validation. Cache exports can similarly survive a killed picker session; they are never counted as successful exports. Editable-project backup/export, project deletion and recoverable migration are future work.

Media decode bounds and sequential export limit memory, but physical-device stress testing is still required, particularly huge input photos, small RAM and OEM document providers. Text/background warnings do not assess arbitrary overlaps created by dragging other elements across text. Font coverage outside the bundled fonts relies on Android fallback. Undo clears after reopening.

The native scroll layout centres the preview on wider screens; it is usable on tablets but is not a bespoke tablet split-pane editor. Large-text and tablet tests are listed in validation.

## Commercial boundary

Editing contains no payment state. Initial static templates, essential editing, local saving and useful exports are free. Pro is a future separate store billing/entitlement module for template breadth, multiple brands, animation styles and campaign variants. €7.99 lifetime is only a hypothesis. Genuine store billing and restoration, including pending/cancelled/refunded purchases, must precede any purchase UI. No simulated successful purchase exists.

## Expansion gates

Complete static workflow validation before video feasibility. Keep the single-template motion experiment isolated until representative physical devices, including lower-powered hardware, pass export/cancel/error tests. Phase 3 (more templates, trimmed video input, user or licensed audio, multiple brands/presets) starts only after that gate. Full multi-track editing, AI, cloud collaboration and a stock marketplace remain outside initial scope.
