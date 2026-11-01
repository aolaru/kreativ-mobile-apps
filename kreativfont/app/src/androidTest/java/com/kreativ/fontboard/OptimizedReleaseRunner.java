package com.kreativ.fontboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;

/** Test APK only. Uses Android's platform runner rather than shared/stripped AndroidX code. */
public class OptimizedReleaseRunner extends Instrumentation {
    static OptimizedReleaseRunner instance;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        instance = this;
        start();
    }

    @Override public void onStart() {
        long start = android.os.SystemClock.elapsedRealtime();
        StringBuilder failures = new StringBuilder();
        for (int index = 0; index < 2; index++) {
            String name = index == 0 ? "catalogueSearchFavoritesAndRecreationSurviveOptimization"
                    : "originalArtworkZoomAndBrowserLinksSurviveOptimization";
            Bundle status = new Bundle();
            status.putString("class", OptimizedReleaseTest.class.getName());
            status.putString("test", name);
            status.putInt("numtests", 2);
            status.putInt("current", index + 1);
            sendStatus(1, status);
            try {
                OptimizedReleaseTest test = new OptimizedReleaseTest();
                if (index == 0) test.catalogueSearchFavoritesAndRecreationSurviveOptimization();
                else test.originalArtworkZoomAndBrowserLinksSurviveOptimization();
                status.putString("stream", ".");
                sendStatus(0, status);
            } catch (Throwable failed) {
                String trace = Log.getStackTraceString(failed);
                failures.append(name).append('\n').append(trace).append('\n');
                status.putString("stack", trace);
                status.putString("stream", trace);
                sendStatus(-2, status);
            }
        }
        Bundle result = new Bundle();
        result.putString("stream", "\nTime: " + (android.os.SystemClock.elapsedRealtime() - start) / 1000f
                + (failures.length() == 0 ? "\nOK (2 tests)\n" : "\nFAILURES!!!\n" + failures));
        finish(failures.length() == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    }
}
