package com.kreativ.fontboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VersionFooterTest {
    private View find(View root, Class<?> type, String text) {
        if (type.isInstance(root) && root instanceof TextView && ((TextView) root).getText().toString().startsWith(text)) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View result = find(group.getChildAt(i), type, text);
                if (result != null) return result;
            }
        }
        return null;
    }

    private void assertFooter(MainActivity activity) {
        LinearLayout footer = activity.getWindow().getDecorView().findViewWithTag("version_credit");
        assertNotNull(footer);
        String version;
        try { version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName; }
        catch (Exception failed) { throw new AssertionError(failed); }
        assertEquals(2, footer.getChildCount());
        TextView versionLabel = (TextView) footer.getChildAt(0);
        Button maker = (Button) footer.getChildAt(1);
        assertEquals("Version " + version + " ·", versionLabel.getText().toString());
        assertFalse("The version must remain plain text", versionLabel.isClickable());
        assertEquals("Made by Kreativ", maker.getText().toString());
        assertTrue(maker.isClickable());
        assertTrue("Credit link needs a 48dp touch target",
                maker.getMinimumHeight() >= Math.round(48 * activity.getResources().getDisplayMetrics().density));
        assertEquals(Gravity.CENTER, footer.getGravity());
        assertEquals(View.VISIBLE, footer.getVisibility());
        ViewGroup parent = (ViewGroup) footer.getParent();
        assertSame("Credit should be the final footer item", footer, parent.getChildAt(parent.getChildCount() - 1));
        assertEquals("Privacy link should remain directly above the credit", activity.getString(R.string.privacy_policy),
                ((TextView) parent.getChildAt(parent.getChildCount() - 2)).getText().toString());
    }

    @Test public void makerCreditOpensAppsPageInBrowser() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        AtomicReference<Intent> opened = new AtomicReference<>();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                opened.set(intent);
                return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
            }
        };
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            instrumentation.addMonitor(monitor);
            try {
                scenario.onActivity(activity -> {
                    Button maker = activity.getWindow().getDecorView().findViewWithTag("maker_credit");
                    assertNotNull(maker);
                    maker.performClick();
                });
                assertNotNull("Credit should launch an external browser intent", opened.get());
                assertEquals(Intent.ACTION_VIEW, opened.get().getAction());
                assertEquals("https://madebykreativ.com/apps", opened.get().getDataString());
                assertNull("Respect the user's preferred browser", opened.get().getComponent());
            } finally {
                instrumentation.removeMonitor(monitor);
            }
        }
    }

    @Test public void dynamicVersionCreditAppearsOnFontsSearchAndFavorites() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertFooter(activity);
                View root = activity.getWindow().getDecorView();
                EditText search = (EditText) find(root, EditText.class, "");
                assertNotNull(search);
                search.setText("serif");
                assertFooter(activity);
                Button favorites = (Button) find(root, Button.class, "Favorites (");
                assertNotNull(favorites);
                favorites.performClick();
                assertFooter(activity);
            });
        }
    }
}
