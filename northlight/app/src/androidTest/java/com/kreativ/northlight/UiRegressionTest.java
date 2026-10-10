package com.kreativ.northlight;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UiRegressionTest {
    private Context context;
    @Before public void cachedOfflineAccess() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Never seed production preferences", context.getPackageName().endsWith(".debug"));
        context.getSharedPreferences(BillingManager.PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("pro_entitled", true).putString("saved_bearings", "[]").commit();
        // This is cached-access UI testing, not purchase verification.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                java.lang.reflect.Field entitled = BillingManager.class.getDeclaredField("entitled");
                entitled.setAccessible(true); entitled.setBoolean(BillingManager.get(context), true);
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        });
    }
    @Test public void allScreensLaunchAndUseScrollableLayouts() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(a -> assertTrue(hasScroll(a.findViewById(android.R.id.content))));
        }
        try (ActivityScenario<AboutActivity> screen = ActivityScenario.launch(AboutActivity.class)) {
            onView(withText(R.string.about_read_privacy_policy)).perform(scrollTo()).check(matches(isDisplayed()));
        }
        try (ActivityScenario<ProActivity> screen = ActivityScenario.launch(ProActivity.class)) {
            try {
                onView(withText(R.string.open_strobe)).perform(scrollTo()).check(matches(isEnabled()));
            } catch (RuntimeException failure) {
                screen.onActivity(a -> captureLayout(a));
                throw failure;
            }
        }
        try (ActivityScenario<StrobeActivity> screen = ActivityScenario.launch(StrobeActivity.class)) {
            onView(withText(R.string.close)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    @Test public void nightLightStaysAwakeAndBrightnessIsAdjustable() {
        try (ActivityScenario<RedLightActivity> screen = ActivityScenario.launch(RedLightActivity.class)) {
            screen.onActivity(a -> {
                assertTrue((a.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0);
                assertTrue(a.getWindow().getAttributes().screenBrightness > 0f);
                assertTrue(hasScroll(a.findViewById(android.R.id.content)));
            });
            onView(withContentDescription(R.string.screen_brightness)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    @Test public void savedHeadingIsFrozenWhileNaming() throws Exception {
        try (ActivityScenario<ProActivity> screen = ActivityScenario.launch(ProActivity.class)) {
            screen.onActivity(a -> { stopLiveSensors(a); a.onHeading(45f); });
            onView(withText(R.string.save_bearing)).perform(scrollTo(), click());
            onView(isAssignableFrom(EditText.class)).perform(replaceText("Trail"), closeSoftKeyboard());
            screen.onActivity(a -> a.onHeading(175f));
            onView(withText(R.string.save)).perform(click());
            BearingData data = stored(); assertEquals(1, data.size()); assertEquals(45, data.degrees(0)); assertEquals("Trail", data.name(0));
        }
    }
    @Test public void savedBearingsCanBeFollowedRenamedAndDeleted() throws Exception {
        context.getSharedPreferences(BillingManager.PREFS, Context.MODE_PRIVATE).edit()
                .putString("saved_bearings", "[{\"name\":\"Trail\",\"degrees\":45}]").commit();
        try (ActivityScenario<ProActivity> screen = ActivityScenario.launch(ProActivity.class)) {
            screen.onActivity(a -> { stopLiveSensors(a); a.onHeading(35f); });
            onView(withText(context.getString(R.string.bearing_row, 45, "Trail"))).perform(scrollTo(), click());
            onView(withText(context.getResources().getStringArray(R.array.bearing_actions)[0])).perform(click());
            onView(withText(context.getString(R.string.guidance_target, "Trail", 45, context.getString(R.string.guidance_right, 10))))
                    .perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText(context.getString(R.string.bearing_row, 45, "Trail"))).perform(scrollTo(), click());
            onView(withText(context.getResources().getStringArray(R.array.bearing_actions)[1])).perform(click());
            onView(isAssignableFrom(EditText.class)).perform(replaceText("Home"), closeSoftKeyboard());
            onView(withText(R.string.save)).perform(click()); assertEquals("Home", stored().name(0));
            onView(withText(context.getString(R.string.bearing_row, 45, "Home"))).perform(scrollTo(), click());
            onView(withText(context.getResources().getStringArray(R.array.bearing_actions)[2])).perform(click());
            onView(withText(R.string.delete)).perform(click()); assertEquals(0, stored().size());
        }
    }
    @Test public void strobeCloseRemainsReachableInLandscape() {
        try (ActivityScenario<StrobeActivity> screen = ActivityScenario.launch(StrobeActivity.class)) {
            screen.onActivity(a -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            onView(withText(R.string.close)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    @Test public void largeTextStrobeControlsRemainReachable() {
        try (ActivityScenario<LargeTextStrobeActivity> screen = ActivityScenario.launch(LargeTextStrobeActivity.class)) {
            screen.onActivity(a -> assertEquals(2f, a.getResources().getConfiguration().fontScale, .01f));
            onView(withText(R.string.strobe_start_strobe)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText(R.string.close)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    private BearingData stored() throws Exception { return new BearingData(context.getSharedPreferences(BillingManager.PREFS, Context.MODE_PRIVATE).getString("saved_bearings", "[]")); }
    private void captureLayout(ProActivity activity) {
        logLayout(activity.getWindow().getDecorView());
    }
    private void logLayout(View view) {
        if (view instanceof ScrollView || view instanceof android.widget.Button) {
            android.graphics.Rect rect = new android.graphics.Rect(); view.getGlobalVisibleRect(rect);
            android.util.Log.i("NorthlightQa", view.getClass().getSimpleName() + " h=" + view.getHeight() + " y=" + view.getY()
                    + " scroll=" + view.getScrollY() + " padding=" + view.getPaddingTop() + "," + view.getPaddingBottom() + " visible=" + rect
                    + (view instanceof android.widget.Button ? " text=" + ((android.widget.Button) view).getText() : ""));
        }
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) logLayout(((ViewGroup) view).getChildAt(i));
    }
    private void stopLiveSensors(ProActivity activity) {
        try {
            java.lang.reflect.Field source = ProActivity.class.getDeclaredField("compass");
            source.setAccessible(true); ((CompassSource) source.get(activity)).stop();
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private boolean hasScroll(View view) {
        if (view instanceof ScrollView) return true;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) if (hasScroll(((ViewGroup) view).getChildAt(i))) return true;
        return false;
    }
}
