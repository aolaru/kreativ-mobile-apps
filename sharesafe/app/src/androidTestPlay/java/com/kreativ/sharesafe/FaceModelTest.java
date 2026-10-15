package com.kreativ.sharesafe;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FaceModelTest {
    @Test public void realBundledDetectorFindsFacesInPublicGroupPhoto() throws Exception {
        Bitmap photo;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("face-assist-nasa.jpg")) {
            photo = BitmapFactory.decodeStream(input);
        }
        assertNotNull(photo);
        try {
            int original = photo.getPixel(0, 0);
            List<RectF> faces = new StoreFaceAssist().find(photo);
            assertFalse("Bundled model must detect at least one face in the public fixture", faces.isEmpty());
            for (RectF area : faces) assertTrue(MaskGeometry.valid(area));
            assertFalse(photo.isRecycled()); assertEquals(original, photo.getPixel(0, 0));
        } finally { photo.recycle(); }
    }
}
