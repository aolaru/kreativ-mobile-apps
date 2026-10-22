package com.kreativ.photofeather;

import android.content.Context;
import android.content.res.Configuration;

/** Test-only configuration override; never changes device settings. */
public final class DarkActivity extends MainActivity {
  @Override
  protected void attachBaseContext(Context base) {
    Configuration c = new Configuration(base.getResources().getConfiguration());
    c.uiMode = (c.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
    super.attachBaseContext(base.createConfigurationContext(c));
  }
}
