package com.kreativ.photofeather;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.util.AtomicFile;
import com.kreativ.imageengine.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/**
 * Process-local worker survives rotation. Atomic disk journal recovers results after process death.
 * Incomplete jobs are stopped, never silently restarted or exported.
 */
final class Session {
  final Context context;
  final CopyOnWriteArrayList<PhotoItem> items = new CopyOnWriteArrayList<>();
  final ExecutorService worker = Executors.newSingleThreadExecutor();
  final Handler main = new Handler(Looper.getMainLooper());
  final File imports, exports;
  final AtomicFile state;
  volatile boolean busy = true, cancelled;
  volatile String message = "Opening your local session…",
      pending = "",
      pendingItemId = "",
      pendingDigest = "";
  volatile long target = 500_000, preparedTarget = -1;
  volatile int preparedEdge, preparedQuality;
  volatile int edge = 2560, quality = 85, background = 0xFFFFFFFF;
  volatile boolean advanced, resizeOnly;
  volatile long previewGeneration;
  volatile Runnable listener;
  private boolean restoring = true;
  private boolean pendingRemoved;

  Session(Context context) {
    this.context = context.getApplicationContext();
    imports = new File(context.getFilesDir(), "imports");
    exports = new File(context.getFilesDir(), "exports");
    imports.mkdirs();
    exports.mkdirs();
    state = new AtomicFile(new File(context.getFilesDir(), "session.json"));
    worker.execute(
        () -> {
          restore();
          prune();
          restoring = false;
          busy = false;
          notifyUi();
        });
  }

  void whenReady(Runnable action) {
    main.post(
        new Runnable() {
          public void run() {
            if (busy) main.postDelayed(this, 100);
            else action.run();
          }
        });
  }

  void notifyUi() {
    main.post(
        () -> {
          Runnable l = listener;
          if (l != null) l.run();
        });
  }

  void check() {
    if (cancelled || Thread.currentThread().isInterrupted()) throw new CancellationException();
  }

  boolean begin(String text) {
    if (busy) return false;
    busy = true;
    cancelled = false;
    message = text;
    notifyUi();
    return true;
  }

  void cancel() {
    cancelled = true;
    message = "Cancelling after the current decode or encode…";
    notifyUi();
  }

  void finish(String text) {
    busy = false;
    message = text;
    persist();
    notifyUi();
  }

  void persistLater() {
    worker.execute(this::persist);
  }

  synchronized boolean persist() {
    FileOutputStream out = null;
    try {
      JSONArray data = new JSONArray();
      for (PhotoItem p : items) data.put(p.json());
      JSONObject j =
          new JSONObject()
              .put("items", data)
              .put("preparedTarget", preparedTarget)
              .put("preparedEdge", preparedEdge)
              .put("preparedQuality", preparedQuality)
              .put("target", target)
              .put("edge", edge)
              .put("quality", quality)
              .put("background", background)
              .put("advanced", advanced)
              .put("resizeOnly", resizeOnly)
              .put("message", message)
              .put("pending", pending)
              .put("pendingItemId", pendingItemId)
              .put("pendingDigest", pendingDigest)
              .put("previewGeneration", previewGeneration)
              .put("busy", busy && !restoring)
              .put("updated", System.currentTimeMillis());
      out = state.startWrite();
      out.write(j.toString().getBytes(StandardCharsets.UTF_8));
      state.finishWrite(out);
      return true;
    } catch (Exception e) {
      if (out != null) state.failWrite(out);
      message = "Session could not be saved. Free device space; keep the app open until export.";
      return false;
    }
  }

  void restore() {
    try {
      JSONObject j = new JSONObject(new String(state.readFully(), StandardCharsets.UTF_8));
      pending = j.optString("pending");
      pendingItemId = j.optString("pendingItemId");
      pendingDigest = j.optString("pendingDigest");
      previewGeneration = j.optLong("previewGeneration");
      preparedTarget = j.optLong("preparedTarget", -1);
      preparedEdge = j.optInt("preparedEdge");
      preparedQuality = j.optInt("preparedQuality");
      target = j.optLong("target", 500_000);
      edge = j.optInt("edge", 2560);
      quality = j.optInt("quality", 85);
      background = j.optInt("background", 0xFFFFFFFF);
      advanced = j.optBoolean("advanced");
      resizeOnly = j.optBoolean("resizeOnly");
      JSONArray data = j.optJSONArray("items");
      boolean expired = System.currentTimeMillis() - j.optLong("updated") >= 86_400_000;
      if (data != null)
        for (int i = 0; i < data.length(); i++) {
          PhotoItem p = PhotoItem.read(data.getJSONObject(i));
          // Ignore private paths outside our own managed directories.
          if (!p.source.isEmpty()
              && !new File(p.source)
                  .getCanonicalPath()
                  .startsWith(imports.getCanonicalPath() + File.separator)) continue;
          if (!p.output.isEmpty()
              && !new File(p.output)
                  .getCanonicalPath()
                  .startsWith(exports.getCanonicalPath() + File.separator)) continue;
          if (p.status.equals("Processing") || p.status.equals("Importing")) {
            p.status = "Interrupted";
            p.error = "The app stopped before this photo finished. Tap Preview copies to retry.";
          }
          if (!p.output.isEmpty() && !p.hasOutput()) {
            p.output = "";
            p.status = "Interrupted";
            p.error = "The temporary copy is unavailable. Preview again.";
          }
          if (p.hasOutput() && p.requestTarget < 0) {
            p.requestTarget = preparedTarget;
            p.requestEdge = preparedEdge;
            p.requestQuality = preparedQuality;
          }
          items.add(p);
        }
      message =
          j.optBoolean("busy")
              ? "Previous task interrupted. Completed copies are still available."
              : items.isEmpty() ? "" : j.optString("message");
      if (!pending.isEmpty()) recoverPending();
      if (expired) {
        items.clear();
        persist();
      }
    } catch (Exception ignored) {
      message = "";
    }
  }

  void prune() {
    Set<String> keep = new HashSet<>();
    for (PhotoItem p : items) {
      keep.add(p.source);
      keep.add(p.output);
    }
    for (File dir : new File[] {imports, exports}) {
      File[] files = dir.listFiles();
      if (files != null)
        for (File f : files)
          if (!keep.contains(f.getPath())
              && (dir.equals(imports)
                  || System.currentTimeMillis() - f.lastModified() > 86_400_000)) f.delete();
    }
  }

  void clear() {
    if (!begin("Clearing this local session…")) return;
    worker.execute(
        () -> {
          items.clear();
          prune();
          finish("");
        });
  }

  void remove(String id) {
    if (!begin("Removing this working copy…")) return;
    worker.execute(
        () -> {
          if (!recoverPending()) {
            busy = false;
            notifyUi();
            return;
          }
          PhotoItem item = null;
          for (PhotoItem p : items) if (p.id.equals(id)) item = p;
          if (item == null) {
            finish("");
            return;
          }
          int index = items.indexOf(item);
          items.remove(item);
          if (!persist()) {
            items.add(index, item);
            busy = false;
            message = "Could not update this session. Free storage and retry removing the photo.";
            notifyUi();
            return;
          }
          // Only unreferenced private imports are deleted. Recent shared outputs stay available.
          prune();
          finish(items.isEmpty() ? "" : "Photo removed. Originals and saved copies are untouched.");
        });
  }

  Set<String> selectedIds() {
    Set<String> ids = new HashSet<>();
    for (PhotoItem p : items) if (p.exportable() && p.selectedForExport) ids.add(p.id);
    return ids;
  }

  Set<String> retryIds() {
    Set<String> ids = new HashSet<>();
    for (PhotoItem p : items) if (p.needsRetry()) ids.add(p.id);
    return ids;
  }

  void add(List<Uri> uris) {
    if (!begin("Importing selected photos…")) return;
    worker.execute(
        () -> {
          int added = 0, failed = 0;
          for (Uri uri : uris) {
            if (items.size() >= 100) {
              message = "A batch can contain up to 100 photos. Start a new session to add more.";
              break;
            }
            try {
              check();
            } catch (CancellationException e) {
              break;
            }
            String id = UUID.randomUUID().toString();
            PhotoItem p = new PhotoItem(id, "Photo " + (items.size() + 1));
            p.status = "Importing";
            items.add(p);
            persist();
            notifyUi();
            File input = new File(imports, id + ".source");
            try {
              if (!"content".equals(uri.getScheme()))
                throw new IOException(
                    "This share source is unsupported. Select the photo with Choose photos.");
              try (Cursor c =
                  context
                      .getContentResolver()
                      .query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst() && !c.isNull(0)) p.name = c.getString(0);
              }
              new ImageEngine().requireSpace(imports, 8_000_000);
              try (InputStream in = context.getContentResolver().openInputStream(uri);
                  FileOutputStream out = new FileOutputStream(input)) {
                if (in == null)
                  throw new IOException("This photo is no longer accessible. Select it again.");
                byte[] buf = new byte[65536];
                int n;
                long count = 0;
                while ((n = in.read(buf)) != -1) {
                  check();
                  count += n;
                  if (count > 200_000_000)
                    throw new IOException("This file exceeds the 200 MB input limit.");
                  new ImageEngine().requireSpace(imports, n);
                  out.write(buf, 0, n);
                }
                out.getFD().sync();
              }
              ImageEngine.Info info = new ImageEngine().inspect(input);
              p.source = input.getPath();
              p.before = info.bytes;
              p.ow = info.width;
              p.oh = info.height;
              p.orientation = info.orientation;
              p.alpha = info.mayHaveAlpha;
              p.status = "Selected";
              added++;
            } catch (CancellationException e) {
              input.delete();
              p.status = "Cancelled";
              p.error = "Import cancelled. Select this photo again.";
              break;
            } catch (Exception | OutOfMemoryError e) {
              input.delete();
              p.status = "Unavailable";
              p.error = friendly(e);
              failed++;
            }
            persist();
            notifyUi();
          }
          String result =
              cancelled
                  ? "Import cancelled. Photos already imported are kept."
                  : "Added " + added + " · " + failed + " unavailable";
          if (items.size() >= 100) result += ". Batch limit: 100 photos.";
          finish(result);
        });
  }

  void process(CompressionOptions options) {
    process(options, null);
  }

  void process(CompressionOptions options, Set<String> ids) {
    List<PhotoItem> jobs = new ArrayList<>();
    for (PhotoItem p : items)
      if (!p.source.isEmpty() && (ids == null || ids.contains(p.id))) jobs.add(p);
    if (jobs.isEmpty()) return;
    if (!begin("Preparing your copies…")) return;
    target = options.targetBytes == 0 ? target : options.targetBytes;
    resizeOnly = options.targetBytes == 0;
    edge = options.longestEdge;
    quality = options.maxQuality;
    background = options.background;
    worker.execute(
        () -> {
          int done = 0, failures = 0;
          preparedTarget = options.targetBytes;
          preparedEdge = options.longestEdge;
          preparedQuality = options.maxQuality;
          // Old outputs are the committed state. Never erase them to begin a replacement job.
          persist();
          for (PhotoItem p : jobs) {
            if (cancelled) break;
            boolean previous = p.hasOutput();
            if (!previous) {
              p.status = "Processing";
              p.error = "";
            }
            message = "Processing " + (++done) + " of " + jobs.size() + " · " + p.name;
            persist();
            notifyUi();
            try {
              ImageEngine.Result result =
                  new ImageEngine()
                      .process(
                          new File(p.source),
                          new File(exports, UUID.randomUUID() + ".jpg"),
                          options,
                          this::check);
              if (previous && p.exportable() && !result.targetReached) {
                result.file.delete();
                p.error =
                    "Retry did not reach the target. The final measured attempt was "
                        + result.bytes
                        + " bytes at "
                        + result.width
                        + " × "
                        + result.height
                        + " px and quality "
                        + result.quality
                        + ". Your previous preview and its settings are kept.";
                failures++;
                persist();
                notifyUi();
                continue;
              }
              // Commit this image only after encoding, byte measurement and verification all
              // succeed.
              p.output = result.file.getPath();
              p.after = result.bytes;
              p.w = result.width;
              p.h = result.height;
              p.quality = result.quality;
              p.attempts = result.attempts;
              p.requestTarget = options.targetBytes;
              p.requestEdge = options.longestEdge;
              p.requestQuality = options.maxQuality;
              p.savedUri = "";
              p.saveError = "";
              p.allowLarger = false;
              p.error = "";
              p.status =
                  !result.targetReached
                      ? "Target not reached"
                      : !result.smaller
                          ? (p.after == p.before ? "Same size" : "Larger copy")
                          : "Ready";
              if (!result.targetReached)
                p.error =
                    "This attempt cannot be exported. Increase the target or choose Resize only. "
                        + "The search stopped within the allowed quality and resolution limits.";
              else if (!result.smaller)
                p.error =
                    p.after == p.before
                        ? "This JPEG is the same size as the original. Export is off until you"
                            + " explicitly allow it."
                        : "This JPEG is "
                            + (p.after - p.before)
                            + " bytes larger than the original. Export is off until you explicitly"
                            + " allow it.";
            } catch (CancellationException e) {
              if (previous)
                p.error = "Retry cancelled. Your previous preview and its settings are kept.";
              else {
                p.status = "Cancelled";
                p.error = "Cancelled. Completed previews are kept.";
              }
              break;
            } catch (Exception | OutOfMemoryError e) {
              if (previous)
                p.error =
                    "Retry failed: "
                        + friendly(e)
                        + " Your previous preview and its settings are kept.";
              else {
                p.status = "Failed";
                p.error = friendly(e);
              }
              failures++;
            }
            persist();
            notifyUi();
          }
          if (cancelled)
            for (PhotoItem p : jobs)
              if (!p.source.isEmpty()
                  && !p.hasOutput()
                  && (p.status.equals("Selected") || p.status.equals("Processing"))) {
                p.status = "Cancelled";
                p.error = "Cancelled before this photo was processed. Tap Preview copies to retry.";
              }
          prune();
          previewGeneration++;
          finish(
              cancelled
                  ? "Cancelled. Completed and previous previews are still available."
                  : "Preview complete · "
                      + eligible()
                      + " ready to export · "
                      + attention()
                      + " need attention"
                      + (failures > 0
                          ? " · "
                              + failures
                              + " retries failed; previous previews kept where available"
                          : ""));
        });
  }

  int attention() {
    int count = 0;
    for (PhotoItem p : items) if (!p.exportable()) count++;
    return count;
  }

  int eligible() {
    int count = 0;
    for (PhotoItem p : items) if (p.exportable()) count++;
    return count;
  }

  private boolean clearPending() {
    String uri = pending, id = pendingItemId, hash = pendingDigest;
    boolean removed = pendingRemoved;
    pending = "";
    pendingItemId = "";
    pendingDigest = "";
    pendingRemoved = false;
    if (persist()) return true;
    pending = uri;
    pendingItemId = id;
    pendingDigest = hash;
    pendingRemoved = removed;
    return false;
  }

  /** Idempotent recovery: a completed copy is adopted, never rolled back due to stale state. */
  boolean recoverPending() {
    if (pending.isEmpty()) return true;
    PhotoItem owner = null;
    for (PhotoItem p : items)
      if (p.id.equals(pendingItemId)) {
        owner = p;
        break;
      }
    // Migrate v0.1 journals, which recorded only a URI. Match the neutral destination name.
    if (owner == null && pendingItemId.isEmpty()) {
      try (Cursor c =
          context
              .getContentResolver()
              .query(
                  Uri.parse(pending),
                  new String[] {OpenableColumns.DISPLAY_NAME},
                  null,
                  null,
                  null)) {
        if (c != null && c.moveToFirst()) {
          String name = c.getString(0);
          for (PhotoItem p : items)
            if (name != null && name.startsWith("PhotoFeather_" + p.id)) {
              owner = p;
              break;
            }
        }
      } catch (Exception ignored) {
      }
      if (owner == null && items.size() == 1) owner = items.get(0);
    }
    if (pendingDigest.isEmpty() && owner != null && owner.hasOutput()) {
      try {
        ImageEngine.verifyOutput(new File(owner.output), owner.w, owner.h);
        pendingDigest = ExportManager.fingerprint(new File(owner.output), () -> {});
        pendingItemId = owner.id;
      } catch (Exception ignored) {
      }
    }
    ExportManager.Recovery recovery =
        pendingRemoved
            ? ExportManager.Recovery.REMOVED
            : ExportManager.recover(context, Uri.parse(pending), pendingDigest);
    if (recovery == ExportManager.Recovery.RETAINED) {
      message =
          "An earlier export could not be verified. Its destination has been kept. Check access or"
              + " free storage and retry saving; your prepared copies are still available to"
              + " share.";
      return false;
    }
    if (recovery == ExportManager.Recovery.SAVED && owner != null) {
      owner.savedUri = pending;
      owner.saveError = "";
    }
    if (recovery == ExportManager.Recovery.SAVED)
      message = "Recovered a saved and verified photo. Completed copies were kept.";
    else
      message = "An incomplete export was cleaned up. Prepared copies are available to save again.";
    if (!clearPending()) {
      message =
          recovery == ExportManager.Recovery.SAVED
              ? "A verified saved copy has been kept, but recovery state could not be updated. Free"
                  + " storage and retry."
              : "The incomplete export was removed, but recovery state could not be updated. No new"
                  + " copy was saved. Free storage and retry.";
      return false;
    }
    if (recovery == ExportManager.Recovery.SAVED)
      message = "Recovered a saved and verified photo. Completed copies were kept.";
    return true;
  }

  void save(Uri tree) {
    save(tree, null);
  }

  void save(Uri tree, Set<String> ids) {
    Set<String> chosen = ids == null ? null : new HashSet<>(ids);
    if (!begin("Saving new copies…")) return;
    worker.execute(
        () -> {
          if (!recoverPending()) {
            busy = false;
            notifyUi();
            return;
          }
          int saved = 0, failed = 0;
          for (PhotoItem p : items) {
            if (!p.exportable()
                || !p.savedUri.isEmpty()
                || (chosen != null && !chosen.contains(p.id))) continue;
            if (cancelled) break;
            message = "Saving · " + p.name;
            notifyUi();
            try {
              Uri uri =
                  ExportManager.save(
                      context,
                      p,
                      tree,
                      this::check,
                      new ExportManager.Journal() {
                        public void pending(Uri newUri) throws IOException {
                          pendingRemoved = false;
                          pending = newUri.toString();
                          pendingItemId = p.id;
                          pendingDigest =
                              ExportManager.fingerprint(new File(p.output), Session.this::check);
                          if (!persist()) throw new IOException("Export journal failed.");
                        }

                        public void removed(Uri removed) {
                          // Rollback confirmed deletion. Do not leave an unverifiable stale record
                          // blocking later saves on providers that throw for deleted documents.
                          if (pending.equals(removed.toString())) {
                            pendingRemoved = true;
                            clearPending();
                          }
                        }
                      });
              p.savedUri = uri.toString();
              p.saveError = "";
              saved++;
              if (!clearPending()) {
                p.saveError =
                    "Copy saved and verified. Recovery state could not be updated; the saved photo"
                        + " has been kept. Free storage and retry.";
                busy = false;
                notifyUi();
                return;
              }
            } catch (CancellationException e) {
              p.saveError = "Save cancelled. This copy is still available to retry.";
              recoverPending();
              break;
            } catch (Exception e) {
              p.saveError = friendly(e);
              failed++;
              if (!recoverPending()) {
                busy = false;
                notifyUi();
                return;
              }
            }
            notifyUi();
          }
          finish(
              cancelled
                  ? "Save cancelled. Copies already saved are kept."
                  : "Saved "
                      + saved
                      + " new copies · "
                      + failed
                      + " failed. Successful copies are kept.");
        });
  }

  static String friendly(Throwable e) {
    if (e instanceof SecurityException || e instanceof FileNotFoundException)
      return "Photo or destination unavailable. Select it again and check access.";
    if (e instanceof OutOfMemoryError) return "Not enough memory. Try a smaller photo.";
    return e.getMessage() == null
        ? "This photo could not be processed. Try another image."
        : e.getMessage();
  }
}
