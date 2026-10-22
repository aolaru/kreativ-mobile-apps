package com.kreativ.photofeather;

import android.content.Context;
import android.content.res.Configuration;

public final class LargePreviewActivity extends PreviewActivity {
  @Override
  protected void attachBaseContext(Context base) {
    Configuration c = new Configuration(base.getResources().getConfiguration());
    c.fontScale = 2f;
    super.attachBaseContext(base.createConfigurationContext(c));
  }
}
