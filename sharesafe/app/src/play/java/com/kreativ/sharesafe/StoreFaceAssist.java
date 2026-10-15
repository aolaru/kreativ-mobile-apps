package com.kreativ.sharesafe;

import android.graphics.Bitmap;
import android.graphics.RectF;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.util.ArrayList;
import java.util.List;

final class StoreFaceAssist implements FaceAssist {
    @Override public List<RectF> find(Bitmap image) throws Exception {
        FaceDetector detector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(.05f).build());
        try {
            // Only called on EditorSession's worker. Await completion before releasing its bitmap.
            List<Face> faces = Tasks.await(detector.process(InputImage.fromBitmap(image, 0)));
            ArrayList<RectF> areas = new ArrayList<>();
            for (Face face : faces) {
                RectF area = FaceAssist.padded(new RectF(face.getBoundingBox()), image.getWidth(), image.getHeight());
                if (area != null) areas.add(area);
                if (areas.size() == 200) break;
            }
            return areas;
        } finally { detector.close(); }
    }
}
