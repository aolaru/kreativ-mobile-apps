package com.kreativ.northlight;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

final class Ui {
    private Ui() { }
    /** Keep insets outside ScrollView: Android's scroll-to-rectangle math counts its padded edges as visible. */
    static View screen(Activity activity, View content) {
        FrameLayout safeArea = new FrameLayout(activity);
        if (content.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
            safeArea.setBackgroundColor(((android.graphics.drawable.ColorDrawable) content.getBackground()).getColor());
        }
        safeArea.addView(content, new FrameLayout.LayoutParams(-1, -1));
        insets(activity, safeArea);
        return safeArea;
    }
    /** Preserve design padding and add actual bars, cutout and keyboard insets. */
    static void insets(Activity activity, View view) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView()).setAppearanceLightNavigationBars(false);
        int left = view.getPaddingLeft(), top = view.getPaddingTop(), right = view.getPaddingRight(), bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom);
            return windowInsets;
        });
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { ViewCompat.requestApplyInsets(v); }
            @Override public void onViewDetachedFromWindow(View v) { }
        });
    }
}
