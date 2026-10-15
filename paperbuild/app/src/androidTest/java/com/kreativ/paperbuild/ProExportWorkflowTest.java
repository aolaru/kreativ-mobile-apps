package com.kreativ.paperbuild;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
public class ProExportWorkflowTest {
    @Test public void proOwnerCanConfigureExportAndSaveAndLoadPasswordFreePreset() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getPackageName().endsWith(".qa"));
        String presetName = "UI preset " + UUID.randomUUID();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Only an isolated QA fixture; no Google Play purchase or production entitlement is changed.
            scenario.onActivity(activity -> context.getSharedPreferences("paperbuild_pro", 0).edit().putBoolean("owned", true).commit());
            scenario.onActivity(activity -> ((android.widget.ScrollView) ((android.view.ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0)).fullScroll(android.view.View.FOCUS_DOWN));
            onView(withId(R.id.export_options)).check(matches(isDisplayed())).perform(click());
            onView(withContentDescription("Watermark text")).perform(replaceText("CONFIDENTIAL"), closeSoftKeyboard());
            onView(withText("Add page numbers · 1 / total")).perform(scrollTo(), click());
            onView(withText("Require a password to open")).perform(scrollTo(), click());
            onView(withContentDescription("Password · 8–64 characters")).perform(scrollTo(), replaceText("qa-only-password"), closeSoftKeyboard());
            onView(withContentDescription("Confirm password")).perform(scrollTo(), replaceText("qa-only-password"), closeSoftKeyboard());
            // QA is not a Play product. Its asynchronous empty ownership refresh must not revoke the mock owner fixture.
            scenario.onActivity(activity -> context.getSharedPreferences("paperbuild_pro", 0).edit().putBoolean("owned", true).commit());
            onView(withText("Apply")).perform(click());
            scenario.onActivity(activity -> {
                ExportOptions options = DocumentSession.get(context).options;
                assertEquals("CONFIDENTIAL", options.watermark); assertTrue(options.pageNumbers); assertTrue(options.protect); options.validate();
            });
            onView(withId(R.id.saved_presets)).check(matches(isDisplayed())).perform(click()); onView(withText("Save current settings…")).perform(click());
            onView(withContentDescription("Preset name")).perform(replaceText(presetName), closeSoftKeyboard()); onView(withText("Save")).perform(click());
            scenario.onActivity(activity -> { DocumentSession.get(context).options = ExportOptions.NONE; DocumentSession.get(context).changed(); });
            onView(withId(R.id.saved_presets)).perform(scrollTo(), click()); onView(withText(presetName)).perform(click()); onView(withText("Use preset")).perform(click());
            scenario.onActivity(activity -> {
                ExportOptions options = DocumentSession.get(context).options;
                assertEquals("CONFIDENTIAL", options.watermark); assertTrue(options.pageNumbers); assertTrue(options.protect); assertEquals("", options.password);
                assertFalse(context.getSharedPreferences("paperbuild_presets", 0).getAll().toString().contains("qa-only-password"));
            });
        } finally {
            new PresetStore(context).remove(presetName);
            context.getSharedPreferences("paperbuild_pro", 0).edit().putBoolean("owned", false).commit();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> { DocumentSession.get(context).options = ExportOptions.NONE; DocumentSession.get(context).save(); });
        }
    }
}
