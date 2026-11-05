package com.kreativ.paperbuild;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

/** Platform runner: no references to app-private or stripped AndroidX classes. */
public final class OptimizedReleaseRunner extends Instrumentation {
    static OptimizedReleaseRunner instance;
    static String reviewCode;
    @Override public void onCreate(Bundle args) {
        super.onCreate(args); instance = this; reviewCode = args.getString("reviewCode"); start();
    }
    @Override public void onStart() {
        long started = SystemClock.elapsedRealtime();
        String[] names = {"freeCropExportAndRecovery", "protectedExportAndPasswordFreePresets", "compactMergeExtractAndUnlimitedPages", "editPdfAndLogoPresets"};
        OptimizedReleaseTest test = new OptimizedReleaseTest(); int failures = 0;
        for (int i = 0; i < names.length; i++) {
            Bundle status = new Bundle(); status.putString("class", OptimizedReleaseTest.class.getName());
            status.putString("test", names[i]); status.putInt("numtests", names.length); status.putInt("current", i + 1); sendStatus(1, status);
            try {
                if (i == 0) test.freeCropExportAndRecovery();
                else if (i == 1) test.protectedExportAndPasswordFreePresets();
                else if (i == 2) test.compactMergeExtractAndUnlimitedPages();
                else test.editPdfAndLogoPresets();
                status.putString("stream", "."); sendStatus(0, status);
            } catch (Throwable failure) {
                failures++; String trace = Log.getStackTraceString(failure);
                // Never leak the bearer reviewer code even if an Android error echoes an input.
                if (reviewCode != null) trace = trace.replace(reviewCode, "[review code redacted]");
                status.putString("stack", trace); status.putString("stream", trace); sendStatus(-2, status);
            }
        }
        reviewCode = null;
        Bundle result = new Bundle(); result.putString("stream", "\nTime: " + (SystemClock.elapsedRealtime() - started) / 1000f
                + (failures == 0 ? "\nOK (4 tests)\n" : "\nFAILURES!!! Failures: " + failures + "\n"));
        finish(failures == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    }
}
