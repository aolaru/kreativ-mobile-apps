package com.kreativ.northlight;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/** Quick Settings entry point for a lantern that has already been permission-approved in Northlight. */
public class LanternTileService extends TileService {
    private CameraManager cameraManager;
    private String cameraId;
    private boolean torchOn;
    private boolean available;
    private boolean known;
    private boolean registered;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CameraManager.TorchCallback callback = new CameraManager.TorchCallback() {
        @Override public void onTorchModeChanged(String id, boolean enabled) {
            if (!id.equals(cameraId)) return;
            torchOn = enabled; available = true; known = true; updateTile();
        }
        @Override public void onTorchModeUnavailable(String id) {
            if (!id.equals(cameraId)) return;
            available = false; known = true; torchOn = false; updateTile();
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        cameraManager = getSystemService(CameraManager.class);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                if (Boolean.TRUE.equals(cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                    cameraId = id;
                    break;
                }
            }
        } catch (Exception ignored) { }
    }

    @Override public void onStartListening() {
        if (!registered) { known = false; cameraManager.registerTorchCallback(callback, handler); registered = true; }
        updateTile();
    }
    @Override public void onStopListening() {
        if (registered) { cameraManager.unregisterTorchCallback(callback); registered = false; }
    }
    @Override public void onDestroy() { onStopListening(); super.onDestroy(); }

    @Override public void onClick() {
        if (cameraId == null || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Intent openApp = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 34) {
                PendingIntent pending = PendingIntent.getActivity(this, 0, openApp, PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pending);
            } else {
                startActivity(openApp);
            }
            return;
        }
        if (!known || !available) return;
        try {
            cameraManager.setTorchMode(cameraId, !torchOn);
        } catch (Exception ignored) {
            available = false;
        }
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean permission = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        tile.setState(cameraId != null && permission && (!known || !available) ? Tile.STATE_UNAVAILABLE : torchOn ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(torchOn ? R.string.tile_on : R.string.tile_lantern));
        tile.updateTile();
    }
}
