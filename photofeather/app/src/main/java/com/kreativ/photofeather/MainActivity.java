package com.kreativ.photofeather;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import com.kreativ.imageengine.CompressionOptions;
import java.io.File;
import java.util.*;

public class MainActivity extends Activity {
  Session session;
  LinearLayout root, dock;
  boolean viewingResults, pageChosen;
  final Set<String> expandedResults = new HashSet<>();
  ArrayList<String> folderItemIds;
  EditText edgeInput, qualityInput;
  TextView progressText;
  Button cancelButton;
  final java.util.concurrent.ExecutorService thumbnails =
      java.util.concurrent.Executors.newSingleThreadExecutor();
  int generation, thumbnailCount;
  String edgeDraft = "2560", qualityDraft = "85";
  boolean draftsLoaded, showAll;
  boolean wasBusy;
  ScrollView scroll;
  int contentScrollY, selectionStart, selectionEnd;
  String focusedField = "";
  long seenPreviewGeneration;
  TextView resultHeading;

  static final int PICK = 41, FOLDER = 42;

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    session = ((FeatherApp) getApplication()).session;
    seenPreviewGeneration = session.previewGeneration;
    if (saved != null) {
      edgeDraft = saved.getString("edge", "2560");
      qualityDraft = saved.getString("quality", "85");
      draftsLoaded = true;
      showAll = saved.getBoolean("all");
      viewingResults = saved.getBoolean("results");
      pageChosen = saved.getBoolean("pageChosen");
      folderItemIds = saved.getStringArrayList("folderIds");
      ArrayList<String> details = saved.getStringArrayList("details");
      if (details != null) expandedResults.addAll(details);
      contentScrollY = saved.getInt("scrollY");
      focusedField = saved.getString("focusedField", "");
      selectionStart = saved.getInt("selectionStart");
      selectionEnd = saved.getInt("selectionEnd");
      seenPreviewGeneration = saved.getLong("seenPreviewGeneration", session.previewGeneration);
    }
    render();
    if (saved == null) acceptShare(getIntent());
  }

  @Override
  protected void onStart() {
    super.onStart();
    session.listener = this::update;
    update();
  }

  @Override
  protected void onStop() {
    session.listener = null;
    super.onStop();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    acceptShare(intent);
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    capture();
    capturePosition();
    out.putInt("scrollY", contentScrollY);
    out.putString("focusedField", focusedField);
    out.putInt("selectionStart", selectionStart);
    out.putInt("selectionEnd", selectionEnd);
    out.putLong("seenPreviewGeneration", seenPreviewGeneration);
    out.putString("edge", edgeDraft);
    out.putString("quality", qualityDraft);
    out.putBoolean("all", showAll);
    out.putBoolean("results", viewingResults);
    out.putBoolean("pageChosen", pageChosen);
    out.putStringArrayList("folderIds", folderItemIds);
    out.putStringArrayList("details", new ArrayList<>(expandedResults));
    super.onSaveInstanceState(out);
  }

  void capture() {
    if (edgeInput != null) edgeDraft = edgeInput.getText().toString();
    if (qualityInput != null) qualityDraft = qualityInput.getText().toString();
  }

  void update() {
    if (session.busy && wasBusy && progressText != null) {
      progressText.setText(session.message);
      return;
    }
    render();
  }

  void capturePosition() {
    if (scroll == null || wasBusy || scroll.getHeight() == 0) return;
    contentScrollY = scroll.getScrollY();
    View focus = getCurrentFocus();
    if (focus != null && (focus == edgeInput || focus == qualityInput)) {
      focusedField = focus == edgeInput ? "edge" : "quality";
      EditText input = (EditText) focus;
      selectionStart = input.getSelectionStart();
      selectionEnd = input.getSelectionEnd();
    } else focusedField = "";
  }

  void restorePosition(ScrollView view, int renderGeneration, boolean showResults) {
    view.getViewTreeObserver()
        .addOnPreDrawListener(
            new android.view.ViewTreeObserver.OnPreDrawListener() {
              public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                if (renderGeneration != generation) return true;
                EditText input =
                    focusedField.equals("edge")
                        ? edgeInput
                        : focusedField.equals("quality") ? qualityInput : null;
                if (!session.busy && input != null && !showResults) {
                  input.requestFocus();
                  int length = input.length();
                  input.setSelection(
                      Math.max(0, Math.min(selectionStart, length)),
                      Math.max(0, Math.min(selectionEnd, length)));
                }
                if (!session.busy) {
                  if (showResults && resultHeading != null) {
                    int y = 0;
                    View child = resultHeading;
                    while (child != view && child.getParent() instanceof View) {
                      y += child.getTop();
                      child = (View) child.getParent();
                    }
                    view.scrollTo(0, y);
                    seenPreviewGeneration = session.previewGeneration;
                    focusedField = "";
                  } else view.scrollTo(0, contentScrollY);
                }
                return true;
              }
            });
  }

  void showPage(boolean results) {
    capture();
    viewingResults = results;
    pageChosen = true;
    contentScrollY = 0;
    focusedField = "";
    if (scroll != null) scroll.scrollTo(0, 0);
    render();
  }

  boolean hasResults() {
    for (PhotoItem p : session.items) if (!p.output.isEmpty() || !p.error.isEmpty()) return true;
    return false;
  }

  void render() {
    capturePosition();
    boolean showResults = !session.busy && session.previewGeneration > seenPreviewGeneration;
    if (!session.busy && !pageChosen) {
      viewingResults = hasResults();
      pageChosen = !session.items.isEmpty();
    }
    if (showResults) viewingResults = true;
    ++generation;
    thumbnailCount = 0;
    capture();
    if (!draftsLoaded && !session.busy) {
      edgeDraft = "" + session.edge;
      qualityDraft = "" + session.quality;
      draftsLoaded = true;
    }
    edgeInput = null;
    qualityInput = null;
    progressText = null;
    wasBusy = session.busy;
    root = Ui.pageColumn(this);
    resultHeading = null;
    dock = !session.busy && !session.items.isEmpty() ? Ui.column(this) : null;
    scroll = Ui.page(this, root, dock);
    restorePosition(scroll, generation, showResults);
    LinearLayout brand = Ui.row(this);
    ImageView logo = new ImageView(this);
    logo.setImageResource(R.drawable.feather);
    logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    brand.addView(logo, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
    LinearLayout title = Ui.column(this);
    TextView name = Ui.text(this, "PhotoFeather", 21, R.color.ink, true);
    Ui.heading(name);
    name.setMaxLines(2);
    title.addView(name);
    title.addView(Ui.text(this, "MADE BY KREATIV", 10, R.color.muted, true));
    brand.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    Button about = Ui.button(this, "About", false);
    about.setContentDescription("About PhotoFeather and privacy");
    about.setOnClickListener(v -> about());
    if (getResources().getConfiguration().fontScale > 1.3f) {
      root.addView(brand);
      Ui.gap(root, 12);
      root.addView(about, new LinearLayout.LayoutParams(-1, -2));
    } else {
      brand.addView(about);
      root.addView(brand);
    }
    Ui.gap(root, 24);
    if (session.busy) {
      LinearLayout card = Ui.card(this);
      card.addView(Ui.text(this, "Working on your device", 23, R.color.ink, true));
      Ui.gap(card, 12);
      ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
      bar.setIndeterminate(true);
      card.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 12)));
      Ui.gap(card, 16);
      progressText = Ui.text(this, session.message, 16, R.color.muted, false);
      progressText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
      card.addView(progressText);
      Ui.gap(card, 16);
      cancelButton = Ui.addButton(card, "Cancel task", false, session::cancel);
      root.addView(card);
      return;
    }
    if (session.items.isEmpty()) {
      contentScrollY = 0;
      focusedField = "";
      home();
      return;
    }
    LinearLayout tabs = Ui.row(this);
    Button settingsTab = Ui.button(this, "Settings", !viewingResults);
    Button resultsTab = Ui.button(this, "Results", viewingResults);
    settingsTab.setSelected(!viewingResults);
    resultsTab.setSelected(viewingResults);
    androidx.core.view.ViewCompat.setStateDescription(
        settingsTab, !viewingResults ? "Selected" : null);
    androidx.core.view.ViewCompat.setStateDescription(
        resultsTab, viewingResults ? "Selected" : null);
    resultsTab.setEnabled(hasResults());
    settingsTab.setOnClickListener(v -> showPage(false));
    resultsTab.setOnClickListener(v -> showPage(true));
    tabs.addView(settingsTab, new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout.LayoutParams tabGap = new LinearLayout.LayoutParams(0, -2, 1);
    tabGap.setMarginStart(Ui.dp(this, 8));
    tabs.addView(resultsTab, tabGap);
    root.addView(tabs);
    Ui.gap(root, 20);
    if (!viewingResults) {
      LinearLayout selection = Ui.card(this);
      selection.addView(
          Ui.text(this, "Your photos  ·  " + session.items.size(), 20, R.color.ink, true));
      Ui.gap(selection, 8);
      int shown = 0;
      for (PhotoItem p : session.items) {
        if (!showAll && shown >= 3) break;
        TextView line =
            Ui.text(
                this,
                p.name
                    + "\n"
                    + (p.source.isEmpty()
                        ? p.error
                        : Ui.size(p.before) + "  ·  " + p.ow + " × " + p.oh + " px"),
                14,
                p.source.isEmpty() ? R.color.warning : R.color.muted,
                false);
        selection.addView(line);
        Button remove = Ui.addButton(selection, "Remove photo", false, () -> confirmRemove(p));
        remove.setContentDescription("Remove " + p.name + " from this session");
        Ui.gap(selection, 12);
        shown++;
      }
      if (session.items.size() > 3)
        Ui.addButton(
            selection,
            showAll ? "Show fewer" : "Show all " + session.items.size() + " photos",
            false,
            () -> {
              showAll = !showAll;
              render();
            });
      Ui.gap(selection, 8);
      Ui.addButton(selection, "Add photos", false, this::pick);
      root.addView(selection);
      Ui.gap(root, 16);
      settings();
      dock.addView(Ui.text(this, "New JPEG copies", 12, R.color.muted, false));
      Ui.gap(dock, 6);
      Ui.addButton(dock, "Preview copies", true, this::prepare);
    } else {
      TextView heading = Ui.text(this, "Your preview", 26, R.color.ink, true);
      Ui.heading(heading);
      resultHeading = heading;
      root.addView(heading);
      Ui.gap(root, 8);
      root.addView(Ui.text(this, session.message, 14, R.color.muted, false));
      Ui.gap(root, 12);
      if (!session.retryIds().isEmpty()) {
        Ui.addButton(root, "Retry failed", false, () -> prepare(session.retryIds()));
        Ui.gap(root, 12);
      }
      for (PhotoItem p : session.items) result(p);
      exportActions();
    }
    Ui.gap(root, 20);
    Ui.addButton(
        root,
        "Clear session",
        false,
        () ->
            new AlertDialog.Builder(this)
                .setTitle("Clear this session?")
                .setMessage(
                    "Removes the app’s imported working copies. Your originals and photos already"
                        + " saved to your gallery or chosen folder stay untouched. Shared temporary"
                        + " outputs are kept for up to 24 hours, then cleaned on a later app"
                        + " launch.")
                .setNegativeButton("Keep session", null)
                .setPositiveButton("Clear session", (d, w) -> session.clear())
                .show());
    footer();
  }

  void home() {
    LinearLayout hero = Ui.column(this);
    hero.setGravity(Gravity.CENTER);
    hero.setPadding(Ui.dp(this, 28), Ui.dp(this, 24), Ui.dp(this, 28), Ui.dp(this, 24));
    hero.setBackground(Ui.shape(this, Ui.color(this, R.color.mint), 32, false));
    ImageView feather = new ImageView(this);
    feather.setImageResource(R.drawable.feather);
    feather.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    hero.addView(feather, new LinearLayout.LayoutParams(Ui.dp(this, 100), Ui.dp(this, 100)));
    Ui.gap(hero, 12);
    hero.addView(Ui.text(this, "A little lighter.\nA lot easier.", 32, R.color.ink, true));
    root.addView(hero);
    Ui.gap(root, 24);
    root.addView(Ui.text(this, "Lighter photos. Ready to share.", 22, R.color.ink, true));
    Ui.gap(root, 8);
    root.addView(
        Ui.text(
            this,
            "Make photos fit upload forms, emails and messages. Choose a size, preview the result,"
                + " then save a new copy.",
            16,
            R.color.muted,
            false));
    Ui.gap(root, 24);
    Ui.addButton(root, "Choose photos", true, this::pick);
    Ui.gap(root, 12);
    TextView formats =
        Ui.text(this, "JPEG, PNG & static WebP  ·  One photo or a batch", 12, R.color.muted, false);
    formats.setGravity(Gravity.CENTER);
    root.addView(formats);
    Ui.gap(root, 24);
    LinearLayout privacy = Ui.card(this);
    privacy.addView(Ui.text(this, "Your photos stay with you", 17, R.color.ink, true));
    Ui.gap(privacy, 6);
    privacy.addView(
        Ui.text(
            this,
            "On-device processing. No uploads, ads or accounts. Originals stay untouched.",
            14,
            R.color.muted,
            false));
    root.addView(privacy);
    footer();
  }

  void settings() {
    LinearLayout card = Ui.card(this);
    TextView h = Ui.text(this, "Output settings", 21, R.color.ink, true);
    Ui.heading(h);
    card.addView(h);
    Ui.gap(card, 16);
    CheckBox resize = new CheckBox(this);
    resize.setText(R.string.resize_only);
    resize.setTextColor(Ui.color(this, R.color.ink));
    resize.setTextSize(15);
    resize.setMinHeight(Ui.dp(this, 48));
    resize.setChecked(session.resizeOnly);
    resize.setOnCheckedChangeListener(
        (b, c) -> {
          capture();
          session.resizeOnly = c;
          session.persistLater();
          render();
        });
    card.addView(resize);
    Ui.gap(card, 12);
    card.addView(Ui.text(this, "Maximum file size", 15, R.color.ink, true));
    Ui.gap(card, 8);
    LinearLayout row = Ui.row(this);
    long[] targets = {200_000, 500_000, 1_000_000};
    String[] labels = {"200 KB", "500 KB", "1 MB"};
    for (int i = 0; i < 3; i++) {
      final long target = targets[i];
      Button b = Ui.button(this, labels[i], !session.resizeOnly && session.target == target);
      b.setContentDescription(labels[i] + " maximum file size");
      b.setSelected(!session.resizeOnly && session.target == target);
      androidx.core.view.ViewCompat.setStateDescription(b, b.isSelected() ? "Selected" : null);
      b.setEnabled(!session.resizeOnly);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
      if (i > 0) lp.setMarginStart(Ui.dp(this, 6));
      row.addView(b, lp);
      b.setOnClickListener(
          v -> {
            capture();
            session.target = target;
            session.persistLater();
            render();
          });
    }
    card.addView(row);
    Ui.gap(card, 8);
    Button custom =
        Ui.addButton(
            card,
            "Custom target"
                + (session.target != 200_000
                        && session.target != 500_000
                        && session.target != 1_000_000
                    ? " · " + Ui.size(session.target)
                    : ""),
            false,
            this::customTarget);
    custom.setEnabled(!session.resizeOnly);
    Ui.gap(card, 8);
    card.addView(
        Ui.text(
            this,
            "KB measures file size. Pixels measure photo dimensions.",
            12,
            R.color.muted,
            false));
    Ui.gap(card, 20);
    card.addView(Ui.text(this, "Longest edge · pixels", 15, R.color.ink, true));
    Ui.gap(card, 8);
    edgeInput = Ui.number(this, "Longest edge in pixels, 64 to 8192", edgeDraft);
    card.addView(edgeInput, new LinearLayout.LayoutParams(-1, -2));
    Ui.gap(card, 8);
    card.addView(
        Ui.text(
            this,
            "Keeps the photo’s shape; never enlarges it. A size target may reduce dimensions.",
            13,
            R.color.muted,
            false));
    Ui.gap(card, 12);
    Ui.addButton(
        card,
        session.advanced ? "Hide advanced quality" : "Advanced JPEG quality",
        false,
        () -> {
          capture();
          session.advanced = !session.advanced;
          session.persistLater();
          render();
        });
    if (session.advanced) {
      Ui.gap(card, 12);
      card.addView(Ui.text(this, "Maximum JPEG quality · 10–100", 15, R.color.ink, true));
      Ui.gap(card, 8);
      qualityInput = Ui.number(this, "Maximum JPEG quality, 10 to 100", qualityDraft);
      card.addView(qualityInput);
      Ui.gap(card, 8);
      card.addView(
          Ui.text(
              this,
              "Default 85. A target may reduce quality to 40, or your lower maximum. JPEG is lossy;"
                  + " visual quality is not guaranteed.",
              13,
              R.color.muted,
              false));
    }
    Ui.gap(card, 16);
    card.addView(Ui.text(this, "JPEG copies · source metadata removed", 14, R.color.ink, true));
    Ui.gap(card, 6);
    card.addView(
        Ui.text(
            this,
            "Location and camera metadata are removed and checked. Visible faces and text remain.",
            13,
            R.color.muted,
            false));
    Ui.gap(card, 8);
    Ui.addButton(
        card,
        "How compression works",
        false,
        () ->
            new AlertDialog.Builder(this)
                .setTitle("Sizes, quality and privacy")
                .setMessage(
                    "1 KB = 1,000 bytes; 1 MB = 1,000,000 bytes. Pixel dimensions describe"
                        + " resolution. Large photos use an eight-megapixel memory limit. Reaching"
                        + " a target may reduce pixels and JPEG quality. JPEG is lossy; visual"
                        + " quality is not guaranteed. Source metadata is removed, but this does"
                        + " not anonymise a photo.")
                .setPositiveButton("Done", null)
                .show());
    root.addView(card);
  }

  void customTarget() {
    EditText input = Ui.number(this, "Custom file size in KB", "" + (session.target / 1000));
    LinearLayout content = Ui.column(this);
    content.setPadding(Ui.dp(this, 24), Ui.dp(this, 12), Ui.dp(this, 24), 0);
    content.addView(
        Ui.text(
            this,
            "Whole KB, from 1 to 50,000. Smaller targets can require stronger quality and"
                + " resolution reductions.",
            14,
            R.color.muted,
            false));
    content.addView(input);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Maximum file size")
            .setView(content)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Use target", null)
            .create();
    dialog.setOnShowListener(
        x ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        session.target =
                            CompressionOptions.parseKilobytes(input.getText().toString());
                        capture();
                        session.persistLater();
                        dialog.dismiss();
                        render();
                      } catch (IllegalArgumentException e) {
                        input.setError(e.getMessage());
                      }
                    }));
    dialog.show();
  }

  void prepare() {
    prepare(null);
  }

  void prepare(Set<String> ids) {
    capture();
    final int edge, q;
    try {
      edge = Integer.parseInt(edgeDraft);
      q = session.advanced ? Integer.parseInt(qualityDraft) : 85;
      new CompressionOptions(
          session.resizeOnly ? 0 : session.target, edge, q, session.background, true);
    } catch (IllegalArgumentException e) {
      new AlertDialog.Builder(this)
          .setTitle("Check your settings")
          .setMessage(
              e instanceof NumberFormatException
                  ? "Enter whole numbers for pixel dimensions and quality."
                  : e.getMessage())
          .setPositiveButton("OK", null)
          .show();
      return;
    }
    boolean alpha = false;
    for (PhotoItem p : session.items) if (ids == null || ids.contains(p.id)) alpha |= p.alpha;
    if (alpha) {
      String[] choices = {"White", "Warm paper", "Black"};
      int[] colors = {Color.WHITE, 0xFFF3ECDD, Color.BLACK};
      final int[] selected = {
        session.background == Color.BLACK ? 2 : session.background == 0xFFF3ECDD ? 1 : 0
      };
      new AlertDialog.Builder(this)
          .setTitle("JPEG needs a background")
          .setMessage(null)
          .setSingleChoiceItems(choices, selected[0], (d, w) -> selected[0] = w)
          .setNegativeButton("Cancel", null)
          .setPositiveButton(
              "Convert to JPEG",
              (d, w) ->
                  session.process(
                      new CompressionOptions(
                          session.resizeOnly ? 0 : session.target,
                          edge,
                          q,
                          colors[selected[0]],
                          true),
                      ids))
          .setView(transparencyNote())
          .show();
    } else
      session.process(
          new CompressionOptions(
              session.resizeOnly ? 0 : session.target, edge, q, session.background, false),
          ids);
  }

  TextView transparencyNote() {
    TextView t =
        Ui.text(
            this,
            "PNG and WebP may contain transparency. JPEG replaces transparent areas with the"
                + " background you choose. Review the result before export.",
            15,
            R.color.muted,
            false);
    t.setPadding(Ui.dp(this, 24), Ui.dp(this, 12), Ui.dp(this, 24), Ui.dp(this, 12));
    return t;
  }

  void confirmRemove(PhotoItem p) {
    new AlertDialog.Builder(this)
        .setTitle("Remove this photo?")
        .setMessage(
            "Removes "
                + p.name
                + " from this session. Your original and any saved copies stay untouched.")
        .setNegativeButton("Keep photo", null)
        .setPositiveButton("Remove photo", (d, w) -> session.remove(p.id))
        .show();
  }

  void result(PhotoItem p) {
    LinearLayout card = Ui.card(this);
    TextView name = Ui.text(this, p.name, 17, R.color.ink, true);
    Ui.heading(name);
    card.addView(name);
    Ui.gap(card, 8);
    boolean targetFailed = p.status.equals("Target not reached");
    TextView status =
        Ui.text(
            this,
            p.status,
            targetFailed ? 24 : 16,
            p.exportable() ? R.color.accent : R.color.warning,
            true);
    Ui.heading(status);
    card.addView(status);
    if (targetFailed) {
      Ui.gap(card, 8);
      card.addView(
          Ui.text(
              this,
              "Requested: "
                  + Ui.size(p.requestTarget >= 0 ? p.requestTarget : session.preparedTarget)
                  + " maximum\nFinal attempt: "
                  + Ui.size(p.after)
                  + " · "
                  + p.after
                  + " bytes",
              16,
              R.color.ink,
              true));
      Ui.gap(card, 8);
      card.addView(Ui.text(this, "Not available to save or share.", 14, R.color.warning, true));
      Ui.gap(card, 12);
      Ui.addButton(
          card,
          "Change target",
          true,
          () -> {
            showPage(false);
            customTarget();
          });
    }
    if (!p.error.isEmpty()) {
      Ui.gap(card, 10);
      card.addView(Ui.text(this, p.error, 14, R.color.warning, false));
    }
    if (p.hasOutput()) {
      Ui.gap(card, 12);
      if (thumbnailCount++ < 3) {
        ImageView thumbnail = new ImageView(this);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setContentDescription("Prepared JPEG preview for " + p.name);
        thumbnail.setBackground(Ui.shape(this, Ui.color(this, R.color.mint), 12, false));
        thumbnail.setClipToOutline(true);
        card.addView(thumbnail, new LinearLayout.LayoutParams(-1, Ui.dp(this, 140)));
        final int taskGeneration = generation;
        final String file = p.output;
        thumbnails.execute(
            () -> {
              try {
                android.graphics.Bitmap bitmap =
                    com.kreativ.imageengine.ImageEngine.preview(new File(file), 1, 512);
                runOnUiThread(
                    () -> {
                      if (!isDestroyed() && taskGeneration == generation)
                        thumbnail.setImageBitmap(bitmap);
                      else bitmap.recycle();
                    });
              } catch (Exception | OutOfMemoryError ignored) {
              }
            });
        Ui.gap(card, 12);
      }
      if (!targetFailed) {
        long saving = p.before - p.after;
        String percent =
            p.before > 0
                ? String.format(Locale.US, "%.1f%%", Math.abs(saving) * 100.0 / p.before)
                : "0%";
        card.addView(
            Ui.text(
                this,
                saving == 0
                    ? "Same file size"
                    : saving > 0 ? percent + " smaller" : percent + " larger",
                26,
                R.color.ink,
                true));
        Ui.gap(card, 8);
      }
      card.addView(
          Ui.text(
              this,
              "Original · "
                  + Ui.size(p.before)
                  + " · "
                  + p.ow
                  + " × "
                  + p.oh
                  + " px\n"
                  + (targetFailed ? "Attempt" : "Output")
                  + " · "
                  + Ui.size(p.after)
                  + " · "
                  + p.w
                  + " × "
                  + p.h
                  + " px",
              15,
              R.color.ink,
              false));
      Ui.gap(card, 12);
      Ui.addButton(
          card,
          "Compare before & after",
          false,
          () -> startActivity(new Intent(this, PreviewActivity.class).putExtra("id", p.id)));
      Ui.gap(card, 8);
      Button details =
          Ui.addButton(
              card,
              expandedResults.contains(p.id) ? "Hide details" : "Show details",
              false,
              () -> {});
      LinearLayout detail = Ui.column(this);
      Ui.gap(detail, 8);
      detail.addView(
          Ui.text(
              this,
              "Measured: "
                  + p.before
                  + " → "
                  + p.after
                  + " bytes\nJPEG quality "
                  + p.quality
                  + "\nPreview settings: "
                  + (p.requestTarget == 0 ? "resize only" : "≤ " + Ui.size(p.requestTarget))
                  + " · longest edge "
                  + p.requestEdge
                  + " px · max quality "
                  + p.requestQuality,
              12,
              R.color.muted,
              false));
      if (p.w < p.ow || p.h < p.oh || p.quality < 85) {
        Ui.gap(detail, 8);
        detail.addView(
            Ui.text(
                this,
                "Dimensions or JPEG quality were reduced. Check fine details in the comparison.",
                13,
                R.color.muted,
                false));
      }
      detail.setVisibility(expandedResults.contains(p.id) ? View.VISIBLE : View.GONE);
      card.addView(detail);
      androidx.core.view.ViewCompat.setStateDescription(
          details, detail.getVisibility() == View.VISIBLE ? "Expanded" : "Collapsed");
      details.setOnClickListener(
          v -> {
            boolean expand = detail.getVisibility() != View.VISIBLE;
            detail.setVisibility(expand ? View.VISIBLE : View.GONE);
            if (expand) expandedResults.add(p.id);
            else expandedResults.remove(p.id);
            details.setText(expand ? "Hide details" : "Show details");
            androidx.core.view.ViewCompat.setStateDescription(
                details, expand ? "Expanded" : "Collapsed");
          });
    }
    if (p.status.equals("Larger copy") || p.status.equals("Same size")) {
      CheckBox allow = new CheckBox(this);
      allow.setText(R.string.allow_larger);
      allow.setTextColor(Ui.color(this, R.color.ink));
      allow.setTextSize(15);
      allow.setMinHeight(Ui.dp(this, 48));
      allow.setChecked(p.allowLarger);
      allow.setOnCheckedChangeListener(
          (b, c) -> {
            p.allowLarger = c;
            session.persistLater();
            render();
          });
      card.addView(allow);
    }
    if (p.exportable()) {
      Ui.gap(card, 8);
      CheckBox selected = new CheckBox(this);
      selected.setText("Include in export");
      selected.setContentDescription("Include " + p.name + " in export");
      selected.setTextColor(Ui.color(this, R.color.ink));
      selected.setTextSize(15);
      selected.setMinHeight(Ui.dp(this, 48));
      selected.setChecked(p.selectedForExport);
      selected.setOnCheckedChangeListener(
          (b, c) -> {
            p.selectedForExport = c;
            session.persistLater();
            exportActions();
          });
      card.addView(selected);
    }
    if (!p.savedUri.isEmpty()) {
      Ui.gap(card, 8);
      card.addView(Ui.text(this, "Saved and verified", 14, R.color.accent, true));
    }
    if (!p.saveError.isEmpty()) {
      Ui.gap(card, 8);
      card.addView(Ui.text(this, p.saveError, 14, R.color.warning, false));
    }
    if (p.needsRetry()) {
      Ui.gap(card, 8);
      Button retry =
          Ui.addButton(card, "Retry photo", false, () -> prepare(Collections.singleton(p.id)));
      retry.setContentDescription("Retry " + p.name + " using the current settings");
    } else if (p.source.isEmpty()) {
      Ui.gap(card, 8);
      Ui.addButton(card, "Select photo again", false, this::pick);
    }
    Ui.gap(card, 8);
    Button remove = Ui.addButton(card, "Remove photo", false, () -> confirmRemove(p));
    remove.setContentDescription("Remove " + p.name + " from this session");
    root.addView(card);
    Ui.gap(root, 16);
  }

  void exportActions() {
    if (dock == null) return;
    dock.removeAllViews();
    int count = session.selectedIds().size();
    boolean unsaved = false;
    for (PhotoItem p : session.items)
      if (p.exportable() && p.selectedForExport && p.savedUri.isEmpty()) unsaved = true;
    TextView notice =
        Ui.text(
            this,
            compactActions()
                ? count + " selected · JPEG"
                : count
                    + " selected · source metadata removed\n"
                    + "Originals untouched; visible details remain.",
            12,
            R.color.muted,
            false);
    notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    dock.addView(notice);
    Ui.gap(dock, 6);
    if (session.eligible() == 0) {
      Ui.addButton(dock, "Review settings", true, () -> showPage(false));
      return;
    }
    if (compactActions()) {
      Button export = Ui.addButton(dock, "Export selected", true, this::chooseExport);
      export.setEnabled(count > 0);
      return;
    }
    LinearLayout actions = Ui.row(this);
    Button save = Ui.button(this, "Save selected", true);
    save.setEnabled(count > 0 && unsaved);
    save.setOnClickListener(v -> reviewExport(this::savePreparedCopies));
    Button share = Ui.button(this, "Share selected", false);
    share.setEnabled(count > 0);
    share.setOnClickListener(v -> share());
    actions.addView(save, new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
    lp.setMarginStart(Ui.dp(this, 8));
    actions.addView(share, lp);
    dock.addView(actions);
  }

  boolean compactActions() {
    android.content.res.Configuration c = getResources().getConfiguration();
    return c.fontScale > 1.3f || c.screenHeightDp < 420;
  }

  void chooseExport() {
    int count = session.selectedIds().size();
    if (count == 0) return;
    boolean unsaved = false;
    for (PhotoItem p : session.items)
      if (p.exportable() && p.selectedForExport && p.savedUri.isEmpty()) unsaved = true;
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Export copies")
            .setNegativeButton("Cancel", null)
            .create();
    LinearLayout options = Ui.column(this);
    options.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 12));
    options.addView(
        Ui.text(
            this,
            count
                + " JPEG copies. Originals stay untouched. Embedded source metadata is removed and"
                + " verified. Visible faces, text and other details remain.",
            14,
            R.color.muted,
            false));
    Ui.gap(options, 16);
    Button save =
        Ui.addButton(
            options,
            "Save",
            true,
            () -> {
              dialog.dismiss();
              reviewExport(this::savePreparedCopies);
            });
    save.setContentDescription("Save selected copies");
    save.setEnabled(unsaved);
    Ui.gap(options, 8);
    Button share =
        Ui.addButton(
            options,
            "Share",
            false,
            () -> {
              dialog.dismiss();
              share();
            });
    share.setContentDescription("Share selected copies");
    ScrollView choices = new ScrollView(this);
    choices.addView(options);
    dialog.setView(choices);
    dialog.show();
  }

  void pick() {
    Intent intent;
    if (Build.VERSION.SDK_INT >= 33) {
      intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
      intent.setType("image/*");
      intent.putExtra(
          MediaStore.EXTRA_PICK_IMAGES_MAX, Math.min(100, MediaStore.getPickImagesMaxLimit()));
    } else {
      intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.setType("image/*");
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    }
    try {
      startActivityForResult(intent, PICK);
    } catch (ActivityNotFoundException e) {
      new AlertDialog.Builder(this)
          .setMessage("No system photo picker is available on this device.")
          .setPositiveButton("OK", null)
          .show();
    }
  }

  @Override
  protected void onActivityResult(int code, int result, Intent data) {
    super.onActivityResult(code, result, data);
    if (result != RESULT_OK || data == null) return;
    if (code == FOLDER) {
      if (data.getData() != null) {
        try {
          getContentResolver()
              .takePersistableUriPermission(
                  data.getData(),
                  Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
        if (folderItemIds != null) {
          Session owner = session;
          Uri tree = data.getData();
          Set<String> chosen = new HashSet<>(folderItemIds);
          owner.whenReady(() -> owner.save(tree, chosen));
        }
        folderItemIds = null;
      }
      return;
    }
    if (code == PICK) {
      List<Uri> uris = new ArrayList<>();
      if (data.getClipData() != null)
        for (int i = 0; i < data.getClipData().getItemCount(); i++)
          uris.add(data.getClipData().getItemAt(i).getUri());
      else if (data.getData() != null) uris.add(data.getData());
      viewingResults = false;
      pageChosen = true;
      contentScrollY = 0;
      Session owner = session;
      owner.whenReady(() -> owner.add(uris));
    }
  }

  void acceptShare(Intent intent) {
    try {
      acceptShareChecked(intent);
    } catch (RuntimeException e) {
      new AlertDialog.Builder(this)
          .setTitle("Unreadable share")
          .setMessage(
              "This app did not provide readable photo files. Use Choose photos to select JPEG, PNG"
                  + " or static WebP images.")
          .setPositiveButton("OK", null)
          .show();
    }
  }

  void acceptShareChecked(Intent intent) {
    if (!Intent.ACTION_SEND.equals(intent.getAction())
        && !Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) return;
    ArrayList<Uri> uris = new ArrayList<>();
    if (Intent.ACTION_SEND.equals(intent.getAction())) {
      Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
      if (uri != null) uris.add(uri);
    } else {
      ArrayList<Uri> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
      if (list != null) uris.addAll(list);
    }
    if (uris.isEmpty() && intent.getClipData() != null)
      for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
        Uri uri = intent.getClipData().getItemAt(i).getUri();
        if (uri != null) uris.add(uri);
      }
    if (uris.isEmpty()) return;
    Runnable add =
        new Runnable() {
          public void run() {
            if (session.busy) {
              session.main.postDelayed(this, 200);
              return;
            }
            session.add(uris);
          }
        };
    session.main.post(add);
  }

  void savePreparedCopies() {
    Set<String> ids = session.selectedIds();
    if (ids.isEmpty()) return;
    if (Build.VERSION.SDK_INT >= 29) session.save(null, ids);
    else {
      folderItemIds = new ArrayList<>(ids);
      Intent folder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
      folder.putExtra(
          android.provider.DocumentsContract.EXTRA_INITIAL_URI,
          android.provider.DocumentsContract.buildDocumentUri(
              "com.android.externalstorage.documents", "primary:Pictures"));
      folder.addFlags(
          Intent.FLAG_GRANT_READ_URI_PERMISSION
              | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
              | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
      try {
        startActivityForResult(folder, FOLDER);
      } catch (ActivityNotFoundException e) {
        folderItemIds = null;
        new AlertDialog.Builder(this)
            .setMessage("No folder picker is available on this device.")
            .setPositiveButton("OK", null)
            .show();
      }
    }
  }

  boolean previewMatchesSettings() {
    capture();
    try {
      long wanted = session.resizeOnly ? 0 : session.target;
      int edge = Integer.parseInt(edgeDraft),
          quality = session.advanced ? Integer.parseInt(qualityDraft) : 85;
      for (PhotoItem p : session.items)
        if (p.exportable() && p.selectedForExport) {
          long target = p.requestTarget >= 0 ? p.requestTarget : session.preparedTarget;
          int limit = p.requestTarget >= 0 ? p.requestEdge : session.preparedEdge,
              maxQuality = p.requestTarget >= 0 ? p.requestQuality : session.preparedQuality;
          if (target != wanted || limit != edge || maxQuality != quality) return false;
        }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  void reviewExport(Runnable action) {
    if (previewMatchesSettings()) {
      action.run();
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("Review the shown previews")
        .setMessage(
            "Some previews use earlier settings, shown on each result. Preview copies again to"
                + " apply your current settings, or explicitly export the measured copies shown"
                + " here as they are.")
        .setNegativeButton("Keep reviewing", null)
        .setPositiveButton("Export shown copies", (d, w) -> action.run())
        .show();
  }

  void share() {
    reviewExport(this::sharePreparedCopies);
  }

  Intent buildShareIntent() {
    ArrayList<Uri> uris = new ArrayList<>();
    for (PhotoItem p : session.items)
      if (p.exportable() && p.selectedForExport)
        uris.add(
            FileProvider.getUriForFile(
                this, BuildConfig.APPLICATION_ID + ".files", new File(p.output)));
    if (uris.isEmpty()) return null;
    Intent share = new Intent(uris.size() == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
    share.setType("image/jpeg");
    if (uris.size() == 1) share.putExtra(Intent.EXTRA_STREAM, uris.get(0));
    else share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
    ClipData clip = ClipData.newUri(getContentResolver(), "PhotoFeather copies", uris.get(0));
    for (int i = 1; i < uris.size(); i++) clip.addItem(new ClipData.Item(uris.get(i)));
    share.setClipData(clip);
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    return share;
  }

  void sharePreparedCopies() {
    try {
      Intent share = buildShareIntent();
      if (share != null) startActivity(Intent.createChooser(share, "Share JPEG copies"));
    } catch (Exception e) {
      new AlertDialog.Builder(this)
          .setMessage("These copies could not be shared. Preview again and retry.")
          .setPositiveButton("OK", null)
          .show();
    }
  }

  @Override
  protected void onDestroy() {
    thumbnails.shutdownNow();
    super.onDestroy();
  }

  void footer() {
    Ui.gap(root, 24);
    TextView t =
        Ui.text(
            this,
            "Version " + BuildConfig.VERSION_NAME + " · Made by Kreativ",
            12,
            R.color.muted,
            false);
    t.setGravity(Gravity.CENTER);
    root.addView(t);
  }

  void about() {
    new AlertDialog.Builder(this)
        .setTitle("PhotoFeather")
        .setMessage(
            "Lighter photos. Ready to share.\n\nMade by Kreativ · Version "
                + BuildConfig.VERSION_NAME
                + "\n\n"
                + "All processing happens on-device. No internet permission, uploads, advertising"
                + " SDKs or analytics SDKs. Android’s picker may offer cloud-provider photos; your"
                + " chosen provider controls retrieval.\n\n"
                + "Inputs: JPEG, PNG and static WebP. Output: lossy JPEG. Transparency needs an"
                + " opaque background. Large photos may be reduced to fit the eight-megapixel"
                + " memory limit.\n\n"
                + "Metadata is removed by creating fresh pixels and stripping all JPEG APP and"
                + " comment segments. Each copy is checked and reopened. This does not anonymise a"
                + " photo or hide visible private details.\n\n"
                + "Working copies are kept privately to recover your session. Clear session removes"
                + " imported copies. Unused export files older than 24 hours are cleaned on a later"
                + " launch.\n\n"
                + "The name is provisional. No billing or paid features are included.")
        .setPositiveButton("Done", null)
        .show();
  }
}
