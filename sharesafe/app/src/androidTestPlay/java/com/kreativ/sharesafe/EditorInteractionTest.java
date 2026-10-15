package com.kreativ.sharesafe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EditorInteractionTest {
    private static Object field(Object object, String name) {
        try { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object); }
        catch (Exception error) { throw new AssertionError(error); }
    }
    private static EditorSession session(RedactActivity activity) { return (EditorSession) field(activity, "session"); }
    private static View control(View root, String text) {
        if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) return root;
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View child = control(((ViewGroup) root).getChildAt(i), text); if (child != null) return child;
        }
        return null;
    }
    private static void ready(ActivityScenario<RedactActivity> scenario) throws Exception {
        AtomicBoolean ready = new AtomicBoolean(); long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> ready.set(session(activity).ready && !session(activity).previewPending));
            if (ready.get()) return; Thread.sleep(20);
        }
        fail("Editor preview timed out");
    }
    private static void drag(RedactActivity activity, float x, float y, float endX, float endY) {
        View editor = (View) field(activity, "editor"); EditorSession session = session(activity);
        float scale = Math.min(editor.getWidth() / (float) session.preview.getWidth(), editor.getHeight() / (float) session.preview.getHeight());
        float width = session.preview.getWidth() * scale, height = session.preview.getHeight() * scale;
        long start = SystemClock.uptimeMillis();
        for (int i = 0; i < 3; i++) {
            float px = (editor.getWidth() - width) / 2 + (i == 0 ? x : endX) * width;
            float py = (editor.getHeight() - height) / 2 + (i == 0 ? y : endY) * height;
            MotionEvent event = MotionEvent.obtain(start, start + i * 20,
                    i == 0 ? MotionEvent.ACTION_DOWN : i == 1 ? MotionEvent.ACTION_MOVE : MotionEvent.ACTION_UP, px, py, 0);
            editor.dispatchTouchEvent(event); event.recycle();
        }
    }
    private static void inputs(View root, ArrayList<EditText> fields) {
        if (root instanceof EditText) fields.add((EditText) root);
        else if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) inputs(((ViewGroup) root).getChildAt(i), fields);
    }

    @Test public void dragMoveCornerResizeAndAccessibleEditingUseSameMaskCoordinates() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File input = ImageAndExportTest.fixture(context, "editor-interaction.jpg", 200, 100);
        try (ActivityScenario<RedactActivity> scenario = ActivityScenario.launch(new Intent(context, RedactActivity.class).setData(Uri.fromFile(input)))) {
            ready(scenario); scenario.onActivity(activity -> session(activity).add(new RectF(.2f, .2f, .5f, .6f))); ready(scenario);
            scenario.onActivity(activity -> {
                control(activity.findViewById(android.R.id.content), "Select areas").performClick(); drag(activity, .35f, .4f, .5f, .5f);
                assertEquals(.35f, session(activity).masks.get(0).area.left, .01f);
            });
            ready(scenario);
            scenario.onActivity(activity -> {
                RectF area = session(activity).masks.get(0).area;
                drag(activity, area.right, area.bottom, area.right + .1f, area.bottom + .1f);
                assertEquals(.75f, session(activity).masks.get(0).area.right, .01f);
                control(activity.findViewById(android.R.id.content), "Choose area…").performClick();
                AlertDialog choose = (AlertDialog) field(activity, "chooseDialog"); choose.getListView().performItemClick(null, 0, 0);
                control(activity.findViewById(android.R.id.content), "Edit selected…").performClick();
            });
            // Dialog OnShow callbacks are queued on Android's main loop. A real user
            // cannot click before that turn, so let it settle before pressing Save.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                AlertDialog dialog = (AlertDialog) field(activity, "areaDialog"); ArrayList<EditText> fields = new ArrayList<>();
                assertTrue(dialog.isShowing());
                inputs(dialog.getWindow().getDecorView(), fields); assertEquals(4, fields.size());
                String[] bounds = {"10", "10", "50", "50"}; for (int i = 0; i < 4; i++) fields.get(i).setText(bounds[i]);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); assertEquals(.1f, session(activity).masks.get(0).area.left, .001f);
                control(activity.findViewById(android.R.id.content), "Delete selected").performClick(); assertTrue(session(activity).masks.isEmpty());
                control(activity.findViewById(android.R.id.content), "Undo").performClick(); assertEquals(1, session(activity).masks.size());
                control(activity.findViewById(android.R.id.content), "Redo").performClick(); assertTrue(session(activity).masks.isEmpty());
            });
        } finally { input.delete(); }
    }
}
