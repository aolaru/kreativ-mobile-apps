package com.kreativ.photofeather;

import android.app.Application;
import java.util.concurrent.*;

public final class FeatherApp extends Application {
  Session session;

  @Override
  public void onCreate() {
    super.onCreate();
    session = new Session(this);
  }
}
