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

/** Quick Settings entry point for a lantern that has already been permission-approved in Northlight. */
public class LanternTileService extends TileService {
    private CameraManager cameraManager;
    private String cameraId;
    private boolean torchOn;

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

    @Override public void onStartListening() { updateTile(); }

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
        try {
            torchOn = !torchOn;
            cameraManager.setTorchMode(cameraId, torchOn);
        } catch (Exception ignored) {
            torchOn = false;
        }
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(torchOn ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(torchOn ? "Northlight on" : "Northlight lantern");
        tile.updateTile();
    }
}
