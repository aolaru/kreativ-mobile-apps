package com.kreativ.sharesafe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;

/** Debug source set only. A synthetic image for manual editor testing, not a Pro unlock. */
public class DemoActivity extends Activity {
    private boolean launched;
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (saved != null && saved.getBoolean("launched")) { launched = true; return; }
        android.widget.TextView message = new android.widget.TextView(this); message.setText("Preparing demo image…"); setContentView(message);
        new Thread(this::createDemo, "ShareSafeDemo").start();
    }
    private void createDemo() {
        Bitmap bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap); canvas.drawColor(Color.WHITE);
            Paint paint = new Paint(); paint.setAntiAlias(true); paint.setColor(Color.rgb(25, 44, 41)); paint.setTextSize(60);
            canvas.drawText("ShareSafe • TEST IMAGE", 70, 120, paint);
            paint.setTextSize(44); canvas.drawText("Name: Alex Example", 70, 260, paint);
            canvas.drawText("Address: 123 Sample Street", 70, 360, paint);
            canvas.drawText("ID: DEMO-123456", 70, 460, paint);
            paint.setColor(Color.rgb(52, 116, 99)); canvas.drawCircle(1000, 360, 115, paint);
            paint.setColor(Color.DKGRAY); paint.setTextSize(32);
            canvas.drawText("Fake details only. Try Cover, rotate, undo and zoom.", 70, 650, paint);
            File folder = PrivateCache.newFolder(this, "redactions"); File file = new File(folder, "demo.jpg");
            try (FileOutputStream stream = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream); }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                launched = true;
                startActivityForResult(new Intent(this, RedactActivity.class).setData(Uri.fromFile(file)), 1);
            });
        } catch (Exception e) {
            runOnUiThread(() -> { if (!isDestroyed()) { android.widget.Toast.makeText(this, "Couldn’t create demo image", android.widget.Toast.LENGTH_SHORT).show(); finish(); } });
        } finally { bitmap.recycle(); }
    }
    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data); setResult(result, data); finish();
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putBoolean("launched", launched); super.onSaveInstanceState(out); }
}
