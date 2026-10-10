package com.kreativ.northlight;

import android.app.Application;
import android.content.Context;
import androidx.test.runner.AndroidJUnitRunner;

/** UI tests intentionally do not contact Play or simulate a real payment. */
public class UiTestRunner extends AndroidJUnitRunner {
    @Override public Application newApplication(ClassLoader loader, String name, Context context)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(loader, UiTestApplication.class.getName(), context);
    }
}
