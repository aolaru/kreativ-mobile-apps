package com.kreativ.northlight;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, application = FieldUiTest.OfflineApplication.class)
public class PlaceLocationSourceTest {
    @Test public void stopsAndRejectsLateOrInaccurateFixes() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
        LocationManager manager = context.getSystemService(LocationManager.class);
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        PlaceLocationSource source = new PlaceLocationSource(context, () -> { });
        source.start(); assertTrue(source.active());
        int generation = (Integer) field(source, "generation");
        long started = (Long) field(source, "started");
        Location value = new Location(LocationManager.GPS_PROVIDER);
        value.setLatitude(44); value.setLongitude(26); value.setAccuracy(5);
        value.setElapsedRealtimeNanos(started - 1);
        accept(source, value, generation); assertNull(source.fix());
        SystemClock.sleep(1);
        value.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        accept(source, value, generation); assertNotNull(source.fix());
        SystemClock.sleep(1);
        value.setAccuracy(51); value.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        accept(source, value, generation); assertNull(source.fix()); assertEquals(PlaceLocationSource.State.LOW, source.state());
        source.stop(); assertFalse(source.active());
        SystemClock.sleep(1);
        value.setAccuracy(5); value.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        accept(source, value, generation); assertNull(source.fix()); assertEquals(PlaceLocationSource.State.OFF, source.state());
    }
    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void accept(PlaceLocationSource source, Location value, int generation) throws Exception {
        Method method = PlaceLocationSource.class.getDeclaredMethod("accept", Location.class, int.class);
        method.setAccessible(true); method.invoke(source, value, generation);
    }
}
