package com.kreativ.northlight;

/** Test-only: disables automatic Play refresh so cached/offline UI can be tested deterministically. */
public class UiTestApplication extends NorthlightApplication {
    @Override public void onCreate() { }
}
