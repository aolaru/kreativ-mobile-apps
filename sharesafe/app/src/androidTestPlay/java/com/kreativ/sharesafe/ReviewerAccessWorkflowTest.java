package com.kreativ.sharesafe;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

@RunWith(AndroidJUnit4.class)
public class ReviewerAccessWorkflowTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private static View find(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View found = find(((ViewGroup) view).getChildAt(i), text); if (found != null) return found;
        }
        return null;
    }
    @Test public void reviewCodeUnlocksEditorAcrossRecreationWithoutChangingPurchaseRecords() throws Exception {
        String code = InstrumentationRegistry.getArguments().getString("reviewerCode");
        assumeNotNull(code); // Supply the private code locally; never commit it to the tests.
        ReviewerAccess.disable(context);
        String receipt = context.getSharedPreferences("sharesafe_billing", Context.MODE_PRIVATE).getString("verified_data", null);
        assertFalse(ReviewerAccess.activate(context, "invalid"));
        assertFalse(ReviewerAccess.enabled(context));
        try {
            try (ActivityScenario<AboutActivity> about = ActivityScenario.launch(AboutActivity.class)) {
                about.onActivity(activity -> {
                    assertNotNull(find(activity.findViewById(android.R.id.content), "Reviewer access"));
                    assertTrue(ReviewerAccess.activate(activity, code));
                });
                about.recreate();
                about.onActivity(activity -> assertNotNull(find(activity.findViewById(android.R.id.content), "Disable reviewer access")));
            }
            assertTrue(ReviewerAccess.enabled(context));
            File input = ImageAndExportTest.fixture(context, "reviewer-test.jpg", 160, 80);
            Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
            AtomicBoolean editorOpened = new AtomicBoolean();
            Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
                @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                    if (intent.getComponent() != null && RedactActivity.class.getName().equals(intent.getComponent().getClassName())) {
                        editorOpened.set(true); return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
                    }
                    return null;
                }
            };
            instrumentation.addMonitor(monitor);
            try (ActivityScenario<MainActivity> main = ActivityScenario.launch(MainActivity.class)) {
                main.onActivity(activity -> {
                    activity.onActivityResult(44, Activity.RESULT_OK, new Intent().setData(Uri.fromFile(input)));
                    View edit = find(activity.findViewById(android.R.id.content), "Advanced edit • Pro");
                    assertNotNull(edit); assertTrue(edit.isEnabled()); edit.performClick();
                });
                assertTrue(editorOpened.get());
            } finally { instrumentation.removeMonitor(monitor); input.delete(); }
            try (ActivityScenario<AboutActivity> about = ActivityScenario.launch(AboutActivity.class)) {
                about.onActivity(activity -> find(activity.findViewById(android.R.id.content), "Disable reviewer access").performClick());
            }
            assertFalse(ReviewerAccess.enabled(context));
            assertEquals(receipt, context.getSharedPreferences("sharesafe_billing", Context.MODE_PRIVATE).getString("verified_data", null));
        } finally { ReviewerAccess.disable(context); }
    }
}
