package com.kreativ.northlight;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/** Billing lives across screens; refresh ownership whenever the app enters the foreground. */
public class NorthlightApplication extends Application {
    private BillingManager billing;
    synchronized BillingManager billing() {
        if (billing == null) billing = new BillingManager(this);
        return billing;
    }
    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int started;
            @Override public void onActivityStarted(Activity activity) {
                if (started++ == 0) BillingManager.get(NorthlightApplication.this).refresh(false);
            }
            @Override public void onActivityStopped(Activity activity) {
                if (--started == 0) BillingManager.get(NorthlightApplication.this).pauseRetries();
            }
            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityResumed(Activity a) { }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        });
    }
}
