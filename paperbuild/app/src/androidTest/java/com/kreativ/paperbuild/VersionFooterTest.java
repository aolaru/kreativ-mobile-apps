package com.kreativ.paperbuild;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VersionFooterTest {
    private void assertFooter(MainActivity activity) {
        TextView footer = activity.findViewById(R.id.version_credit);
        assertNotNull(footer);
        String version;
        try {
            version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
        assertEquals("Version " + version + " · Made by Kreativ", footer.getText().toString());
        assertEquals(Gravity.CENTER, footer.getGravity());
        assertEquals(View.VISIBLE, footer.getVisibility());
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, footer.getLayoutParams().width);
        ViewGroup parent = (ViewGroup) footer.getParent();
        assertSame(footer, parent.getChildAt(parent.getChildCount() - 1));
        assertEquals("Privacy & About",
                ((TextView) parent.getChildAt(parent.getChildCount() - 2)).getText().toString());
        assertTrue("Retain safe space above system navigation",
                parent.getPaddingBottom() >= Ui.dp(activity, 64));
    }

    @Test public void versionCreditAppearsBelowPrivacyAndSurvivesRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(this::assertFooter);
            scenario.recreate();
            scenario.onActivity(this::assertFooter);
        }
    }
}
