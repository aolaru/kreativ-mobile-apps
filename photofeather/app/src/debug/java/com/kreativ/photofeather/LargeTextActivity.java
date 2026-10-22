package com.kreativ.photofeather;

import android.content.Context;
import android.content.res.Configuration;

/** Test-only configuration override; never changes device settings. */
public final class LargeTextActivity extends MainActivity {
  @Override
  protected void attachBaseContext(Context base) {
    Configuration c = new Configuration(base.getResources().getConfiguration());
    c.fontScale = 2f;
    super.attachBaseContext(base.createConfigurationContext(c));
  }
}
