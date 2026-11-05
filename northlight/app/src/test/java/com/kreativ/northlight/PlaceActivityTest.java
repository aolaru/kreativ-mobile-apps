package com.kreativ.northlight;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.widget.TextView;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, application = FieldUiTest.OfflineApplication.class)
public class PlaceActivityTest {
    @Test public void gpsAndSpeechStartOffAndPauseClearsFix() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(BillingManager.PREFS, 0).edit().putBoolean("pro_entitled", true)
                .putString("saved_places", "[{\"name\":\"Car\",\"lat\":44.01,\"lon\":26,\"accuracy\":5}]").commit();
        try (ActivityController<PlaceActivity> controller = Robolectric.buildActivity(PlaceActivity.class).setup()) {
            PlaceActivity activity = controller.get();
            assertEquals(activity.getString(R.string.place_start_gps), ((TextView) activity.findViewById(R.id.place_gps)).getText());
            assertFalse(activity.findViewById(R.id.place_save).isEnabled());
            assertFalse(activity.findViewById(R.id.place_speak).isEnabled());
            PlaceLocationSource source = (PlaceLocationSource) field(activity, "location");
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(44); location.setLongitude(26); location.setAccuracy(5);
            setField(source, "fix", location); setField(source, "state", PlaceLocationSource.State.READY);
            setField(activity, "selected", 0);
            activity.onCompassStatus(R.string.compass_accuracy_ok); activity.onHeading(0);
            assertTrue(((TextView) activity.findViewById(R.id.place_target)).getText().toString().contains("Car"));
            assertTrue(activity.findViewById(R.id.place_speak).isEnabled());
            controller.pause();
            assertNull(source.fix());
            assertFalse(activity.findViewById(R.id.place_speak).isEnabled());
        }
    }
    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
