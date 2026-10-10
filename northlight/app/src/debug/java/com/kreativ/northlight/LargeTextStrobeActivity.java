package com.kreativ.northlight;

import android.content.Context;
import android.content.res.Configuration;

/** Debug-only accessibility fixture. Does not change system settings or enter the release APK. */
public class LargeTextStrobeActivity extends StrobeActivity {
    @Override protected void attachBaseContext(Context base) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.fontScale = 2f;
        super.attachBaseContext(base.createConfigurationContext(config));
    }
}
