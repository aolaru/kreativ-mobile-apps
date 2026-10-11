package com.kreativ.paperbuild;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Recycled rows with bounded thumbnail cache and decoding off the UI thread. */
final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {
    interface Actions { void move(int from, int to); void rotate(int index); void remove(int index); }
    private final List<PageItem> pages;
    private final Actions actions;
    private final DocumentProcessor processor;
    private final ExecutorService images = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
    };
    private boolean busy, closed;
    PageAdapter(List<PageItem> pages, DocumentProcessor processor, Actions actions) {
        this.pages = pages; this.processor = processor; this.actions = actions;
    }
    void setBusy(boolean busy) { if (this.busy != busy) { this.busy = busy; notifyDataSetChanged(); } }
    void close() { closed = true; images.shutdownNow(); cache.evictAll(); }
    @Override public int getItemCount() { return pages.size(); }

    @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(row, 12), dp(row, 10), dp(row, 12), dp(row, 10));
        row.setBackgroundResource(R.drawable.field_background);
        RecyclerView.LayoutParams layout = new RecyclerView.LayoutParams(-1, -2);
        layout.bottomMargin = dp(row, 8); row.setLayoutParams(layout);
        LinearLayout top = new LinearLayout(row.getContext()); top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView image = new ImageView(row.getContext()); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        top.addView(image, new LinearLayout.LayoutParams(dp(row, 58), dp(row, 76)));
        TextView name = Ui.text(row.getContext(), "", 14, Ui.INK, true);
        name.setMaxLines(2); name.setPadding(dp(row, 10), 0, 0, 0);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(top);
        LinearLayout controls = new LinearLayout(row.getContext());
        Button up = control(controls, "↑", "Move page up");
        Button down = control(controls, "↓", "Move page down");
        Button rotate = control(controls, "↻", "Rotate page clockwise");
        boolean compact = row.getResources().getConfiguration().screenWidthDp < 360
                || row.getResources().getConfiguration().fontScale > 1.15f;
        Button remove;
        if (compact) {
            row.addView(controls); Ui.gap(row, 8);
            LinearLayout removeRow = new LinearLayout(row.getContext());
            remove = control(removeRow, "Remove", "Remove page"); row.addView(removeRow);
        } else {
            remove = control(controls, "Remove", "Remove page"); row.addView(controls);
        }
        remove.setTextColor(Color.rgb(181, 61, 61));
        return new Holder(row, image, name, up, down, rotate, remove);
    }

    @Override public void onBindViewHolder(Holder holder, int position) {
        PageItem page = pages.get(position);
        holder.name.setText("Page " + (position + 1) + " · " + page.name);
        holder.image.setContentDescription("Preview of page " + (position + 1));
        holder.up.setEnabled(!busy && position > 0); holder.down.setEnabled(!busy && position < pages.size() - 1);
        holder.rotate.setEnabled(!busy); holder.remove.setEnabled(!busy);
        holder.up.setOnClickListener(v -> { int p = holder.getBindingAdapterPosition(); if (!busy && p > 0) actions.move(p, p - 1); });
        holder.down.setOnClickListener(v -> { int p = holder.getBindingAdapterPosition(); if (!busy && p >= 0 && p < pages.size() - 1) actions.move(p, p + 1); });
        holder.rotate.setOnClickListener(v -> { int p = holder.getBindingAdapterPosition(); if (!busy && p >= 0) actions.rotate(p); });
        holder.remove.setOnClickListener(v -> { int p = holder.getBindingAdapterPosition(); if (!busy && p >= 0) actions.remove(p); });
        String key = page.uri + ":" + page.rotation;
        holder.image.setTag(key);
        Bitmap cached = cache.get(key);
        holder.image.setImageBitmap(cached);
        if (cached == null) images.execute(() -> {
            Bitmap thumbnail = null;
            try { thumbnail = processor.loadImage(page, 240, 0); }
            catch (Exception | OutOfMemoryError ignored) { }
            Bitmap result = thumbnail;
            main.post(() -> {
                if (closed) { if (result != null) result.recycle(); return; }
                if (result != null) cache.put(key, result);
                if (key.equals(holder.image.getTag())) holder.image.setImageBitmap(result);
            });
        });
    }

    private Button control(LinearLayout parent, String text, String description) {
        Button button = new Button(parent.getContext()); button.setText(text); button.setAllCaps(false);
        button.setTextSize(13); button.setContentDescription(description); button.setPadding(0, 0, 0, 0);
        button.setTextColor(Ui.ACCENT); button.setStateListAnimator(null);
        button.setMinWidth(0); button.setMinHeight(dp(button, 48)); button.setBackgroundResource(R.drawable.button_compact);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, text.equals("Remove") ? 1.5f : 1f);
        params.setMarginEnd(dp(button, 4)); parent.addView(button, params); return button;
    }
    private static int dp(View view, int size) { return Math.round(size * view.getResources().getDisplayMetrics().density); }
    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image; final TextView name; final Button up, down, rotate, remove;
        Holder(View row, ImageView image, TextView name, Button up, Button down, Button rotate, Button remove) {
            super(row); this.image = image; this.name = name; this.up = up; this.down = down; this.rotate = rotate; this.remove = remove;
        }
    }
}
