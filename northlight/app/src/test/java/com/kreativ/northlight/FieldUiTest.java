package com.kreativ.northlight;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** JVM UI checks and native-rendered previews. No emulator, Play purchase or real torch. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, application = FieldUiTest.OfflineApplication.class, qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FieldUiTest {
    public static class OfflineApplication extends NorthlightApplication {
        @Override public void onCreate() { /* Do not connect Billing during layout tests. */ }
    }
    private void access(boolean owned, String bearings) {
        Context context = RuntimeEnvironment.getApplication();
        CameraCharacteristics camera = org.robolectric.shadows.ShadowCameraCharacteristics.newCameraCharacteristics();
        shadowOf(camera).set(CameraCharacteristics.FLASH_INFO_AVAILABLE, true);
        shadowOf(camera).set(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL, 1);
        shadowOf(camera).set(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL, 1);
        shadowOf(context.getSystemService(CameraManager.class)).addCamera("test-flash", camera);
        context.getSharedPreferences(BillingManager.PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("pro_entitled", owned).putString("saved_bearings", bearings).commit();
    }
    @Test public void freeHomeHasCompactToolsAndNoSavedData() throws Exception {
        access(false, "[{\"name\":\"Private direction\",\"degrees\":32}]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get(); a.onHeading(349); a.onCompassStatus(R.string.compass_accuracy_ok);
            assertEquals("349°", ((TextView)a.findViewById(R.id.field_heading)).getText().toString());
            assertEquals(View.GONE, a.findViewById(R.id.field_saved_bearings).getVisibility());
            assertEquals(a.getString(R.string.main_explore_northlight_pro), ((Button)a.findViewById(R.id.field_pro_tools)).getText());
            assertNotNull(a.findViewById(R.id.field_night_light)); assertNotNull(a.findViewById(R.id.field_sos));
            preview(a, "field-free", 786, 1704);
        }
    }
    @Test public void proHomeOpensSavedBearingDirectly() throws Exception {
        access(true, "[{\"name\":\"Trailhead direction\",\"degrees\":32}]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get(); a.onHeading(349); a.onCompassStatus(R.string.compass_accuracy_ok);
            LinearLayout bearings = a.findViewById(R.id.field_saved_bearings);
            assertEquals(View.VISIBLE, bearings.getVisibility()); assertEquals(1, bearings.getChildCount());
            assertEquals(a.getString(R.string.field_all_pro_tools), ((Button)a.findViewById(R.id.field_pro_tools)).getText());
            bearings.getChildAt(0).performClick();
            Intent target = shadowOf(a).getNextStartedActivity();
            assertEquals(ProActivity.class.getName(), target.getComponent().getClassName());
            assertEquals(0, target.getIntExtra(ProActivity.EXTRA_BEARING_INDEX, -1));
            preview(a, "field-pro", 786, 1704);
        }
    }
    @Test public void directBearingEntrySelectsGuidanceWithoutSalesTitle() {
        access(true, "[{\"name\":\"Trail\",\"degrees\":32}]");
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), ProActivity.class).putExtra(ProActivity.EXTRA_BEARING_INDEX, 0);
        try (ActivityController<ProActivity> controller = Robolectric.buildActivity(ProActivity.class, intent).setup()) {
            ProActivity a = controller.get(); a.onHeading(349);
            assertTrue(hasText(a.findViewById(android.R.id.content), a.getString(R.string.field_pro_owner_title)));
            assertTrue(hasText(a.findViewById(android.R.id.content), a.getString(R.string.guidance_target, "Trail", 32, a.getString(R.string.guidance_right, 43))));
        }
    }
    @Test public void invalidBearingEntryDoesNotCrash() {
        access(true, "[]");
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), ProActivity.class).putExtra(ProActivity.EXTRA_BEARING_INDEX, 99);
        try (ActivityController<ProActivity> controller = Robolectric.buildActivity(ProActivity.class, intent).setup()) {
            assertTrue(hasText(controller.get().findViewById(android.R.id.content), controller.get().getString(R.string.guidance_hint)));
        }
    }
    @Test @Config(qualifiers = "w320dp-h640dp-mdpi") public void smallPhoneStillFitsDialAndTools() throws Exception {
        access(false, "[]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get(); a.onHeading(85); a.onCompassStatus(R.string.compass_accuracy_ok);
            preview(a, "field-small", 320, 640);
            View dial = a.findViewById(R.id.field_compass);
            assertTrue(dial.getWidth() <= 320);
            LinearLayout tools = (LinearLayout)a.findViewById(R.id.field_lantern).getParent();
            assertEquals(LinearLayout.VERTICAL, tools.getOrientation());
            assertTrue(a.findViewById(R.id.field_lantern).getHeight() >= 48);
        }
    }
    @Test public void calibrationActionShowsHelp() {
        access(false, "[]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().findViewById(R.id.field_accuracy).performClick();
            assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().isShowing());
        }
    }
    @Test public void spokenCompassIsFreeAndOffByDefault() throws Exception {
        access(false, "[]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get();
            assertTrue(a.findViewById(R.id.speech_speak).isEnabled());
            assertFalse(((android.widget.Switch)a.findViewById(R.id.speech_auto)).isChecked());
            assertFalse(a.findViewById(R.id.speech_pause).isEnabled());
            assertEquals(View.ACCESSIBILITY_LIVE_REGION_NONE, a.findViewById(R.id.speech_result).getAccessibilityLiveRegion());
            a.onSpeechReading("Phone facing North, 0 degrees, magnetic.", true);
            assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, a.findViewById(R.id.speech_result).getAccessibilityLiveRegion());
            a.onSpeechReading("Phone facing East, 90 degrees, magnetic.", false);
            assertEquals(View.ACCESSIBILITY_LIVE_REGION_NONE, a.findViewById(R.id.speech_result).getAccessibilityLiveRegion());
            preview(a, "speech-home", 786, 1704);
            View speak = a.findViewById(R.id.speech_speak);
            ScrollView scroll = (ScrollView)((View)speak.getParent()).getParent();
            scroll.scrollTo(0, Math.max(0, speak.getTop() - 140));
            preview(a, "speech-controls", 786, 1704);
        }
    }
    @Test public void speechSettingsUseNativeChoiceDialogsAndSavePreferences() {
        access(false, "[]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get(); a.findViewById(R.id.speech_settings).performClick();
            android.app.AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            dialog.getListView().performItemClick(null, 0, 0);
            android.app.AlertDialog frequency = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            assertEquals(3, frequency.getListView().getCount());
            frequency.getListView().performItemClick(null, 2, 2);
            assertEquals(2, a.getSharedPreferences(CompassSpeech.PREFS, 0).getInt("interval", -1));
            assertFalse(((android.widget.Switch)a.findViewById(R.id.speech_auto)).isChecked());
        }
    }
    @Test public void screenReaderButtonUsesLiveRegionWithoutStartingTts() {
        access(false, "[]");
        android.view.accessibility.AccessibilityManager manager = RuntimeEnvironment.getApplication()
                .getSystemService(android.view.accessibility.AccessibilityManager.class);
        shadowOf(manager).setEnabled(true); shadowOf(manager).setTouchExplorationEnabled(true);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get(); a.onHeading(315); a.onCompassStatus(R.string.compass_accuracy_ok);
            assertFalse(a.findViewById(R.id.speech_auto).isEnabled());
            a.findViewById(R.id.speech_speak).performClick();
            TextView result = a.findViewById(R.id.speech_result);
            assertTrue(result.getText().toString().contains("315"));
            assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, result.getAccessibilityLiveRegion());
            a.findViewById(R.id.speech_speak).performClick();
            assertTrue(result.getText().toString().contains("315"));
            assertNull(org.robolectric.shadows.ShadowTextToSpeech.getLastTextToSpeechInstance());
        }
    }
    @Test @Config(qualifiers = "w320dp-h640dp-mdpi") public void speechControlsFitWithLargeText() throws Exception {
        RuntimeEnvironment.setFontScale(2f); access(false, "[]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get(); preview(a, "speech-small-initial", 320, 640);
            View speak = a.findViewById(R.id.speech_speak);
            ScrollView scroll = (ScrollView)((View)speak.getParent()).getParent();
            scroll.scrollTo(0, Math.max(0, speak.getTop() - 160));
            preview(a, "speech-large-text", 320, 640);
            for (int id : new int[]{R.id.speech_speak, R.id.speech_pause, R.id.speech_settings, R.id.speech_auto}) {
                TextView v = a.findViewById(id);
                assertTrue(v.getHeight() >= 48); assertTrue(v.getWidth() <= 320);
                for (int i=0; i<v.getLayout().getLineCount(); i++) assertEquals(0, v.getLayout().getEllipsisCount(i));
            }
            assertEquals(LinearLayout.VERTICAL, ((LinearLayout)a.findViewById(R.id.speech_pause).getParent()).getOrientation());
        }
    }
    @Test public void newLauncherSupportsAdaptiveAndThemedIcons() {
        android.graphics.drawable.Drawable icon = RuntimeEnvironment.getApplication().getDrawable(R.mipmap.ic_launcher);
        assertTrue(icon instanceof android.graphics.drawable.AdaptiveIconDrawable);
        assertNotNull(((android.graphics.drawable.AdaptiveIconDrawable)icon).getMonochrome());
    }
    @Test public void versionCreditAppearsDirectlyBelowPrivacyFooter() {
        access(false, "[]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get();
            TextView credit = a.findViewById(R.id.field_version_credit);
            assertEquals(a.getString(R.string.field_version_credit, BuildConfig.VERSION_NAME), credit.getText());
            LinearLayout parent = (LinearLayout)credit.getParent();
            int index = parent.indexOfChild(credit);
            assertTrue(index > 0);
            assertEquals(a.getString(R.string.main_no_account_no_ads_no_tracking), ((TextView)parent.getChildAt(index - 1)).getText());
        }
    }
    @Test public void unavailableCompassNeverKeepsAnOldNumericHeading() {
        access(false, "[]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get(); a.onHeading(349); a.onCompassStatus(R.string.compass_unavailable);
            assertEquals(a.getString(R.string.main), ((TextView)a.findViewById(R.id.field_heading)).getText());
            assertEquals(a.getString(R.string.field_no_sensor), ((TextView)a.findViewById(R.id.field_direction)).getText());
        }
    }
    @Test public void largeTextStacksLightControls() throws Exception {
        RuntimeEnvironment.setFontScale(2f);
        access(true, "[]");
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity a = controller.get(); a.onHeading(349); a.onCompassStatus(R.string.compass_accuracy_ok);
            preview(a, "field-large-text", 786, 1704);
            LinearLayout tools = (LinearLayout)a.findViewById(R.id.field_lantern).getParent();
            assertEquals(LinearLayout.VERTICAL, tools.getOrientation());
            for (int id : new int[]{R.id.field_lantern, R.id.field_night_light, R.id.field_sos}) {
                assertTrue(a.findViewById(id).getWidth() <= tools.getWidth());
            }
        }
    }
    private boolean hasText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView)view).getText())) return true;
        if (view instanceof ViewGroup) for (int i=0; i<((ViewGroup)view).getChildCount(); i++)
            if (hasText(((ViewGroup)view).getChildAt(i), text)) return true;
        return false;
    }
    private void preview(MainActivity activity, String name, int width, int height) throws Exception {
        View content = activity.findViewById(android.R.id.content);
        content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        content.layout(0, 0, width, height);
        TextView heading = activity.findViewById(R.id.field_heading);
        assertNotNull(heading.getLayout());
        assertTrue("Heading must fit inside the dial", heading.getLayout().getLineWidth(0) <= heading.getWidth() + 1);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); canvas.drawColor(activity.getColor(R.color.night)); content.draw(canvas);
        File directory = new File("build/field-previews"); assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(directory, name + ".png"))) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); }
        bitmap.recycle();
    }
}
