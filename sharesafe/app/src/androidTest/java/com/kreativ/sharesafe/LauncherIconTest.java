package com.kreativ.sharesafe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LauncherIconTest {
    private static Bitmap render(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, 108, 108);
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }

    private static void assertShield(Bitmap bitmap) {
        assertEquals("White shield outline", Color.WHITE, bitmap.getPixel(54, 30));
        assertEquals("White plus", Color.WHITE, bitmap.getPixel(54, 54));
        assertEquals("Shield interior stays transparent", 0, Color.alpha(bitmap.getPixel(54, 69)));
        for (int y = 0; y < 108; y++) for (int x = 0; x < 108; x++) {
            if (Color.alpha(bitmap.getPixel(x, y)) > 32) {
                assertTrue("Shield must fit the adaptive safe zone", Math.hypot(x + 0.5 - 54, y + 0.5 - 54) <= 33);
            }
        }
    }

    @Test public void launcherUsesOriginalGreenAndMaskSafeShieldIncludingThemedIcon() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Drawable drawable = context.getPackageManager().getApplicationIcon(context.getPackageName());
        assertTrue("Manifest must use an adaptive launcher icon", drawable instanceof AdaptiveIconDrawable);
        AdaptiveIconDrawable icon = (AdaptiveIconDrawable) drawable;
        Bitmap background = render(icon.getBackground());
        Bitmap foreground = render(icon.getForeground());
        try {
            assertEquals(Color.rgb(52, 116, 99), background.getPixel(0, 0));
            assertEquals(Color.rgb(52, 116, 99), background.getPixel(54, 54));
            assertShield(foreground);
        } finally {
            background.recycle(); foreground.recycle();
        }
        if (Build.VERSION.SDK_INT >= 33) {
            assertNotNull("Android 13+ themed icon layer", icon.getMonochrome());
            Bitmap monochrome = render(icon.getMonochrome());
            try { assertShield(monochrome); } finally { monochrome.recycle(); }
        }
    }
}
