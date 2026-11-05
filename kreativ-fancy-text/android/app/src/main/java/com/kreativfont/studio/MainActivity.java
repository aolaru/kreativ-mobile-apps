package com.kreativfont.studio;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(ExportFilePlugin.class);
        registerPlugin(KreativBillingPlugin.class);
        super.onCreate(savedInstanceState);
        applyFontScale();
    }

    @Override
    public void onResume() {
        super.onResume();
        applyFontScale();
    }

    private void applyFontScale() {
        if (getBridge() == null) return;
        // WebView labels must follow Android's user-selected font size, too.
        int zoom = Math.round(getResources().getConfiguration().fontScale * 100);
        if (getBridge().getWebView().getSettings().getTextZoom() != zoom) {
            getBridge().getWebView().getSettings().setTextZoom(zoom);
        }
    }
}
