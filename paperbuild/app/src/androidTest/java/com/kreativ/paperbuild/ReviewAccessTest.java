package com.kreativ.paperbuild;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

@RunWith(AndroidJUnit4.class)
public class ReviewAccessTest {
    private Context context;
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getPackageName().endsWith(".qa"));
        ReviewAccess.clear(context); setPurchased(false);
    }
    @After public void cleanup() { ReviewAccess.clear(context); setPurchased(false); }
    private void setPurchased(boolean owned) {
        context.getSharedPreferences("paperbuild_pro", Context.MODE_PRIVATE).edit().putBoolean("owned", owned).commit();
    }
    @Test public void invalidCodeNeverUnlocksAndLeavingKeepsPurchase() {
        assertFalse(ReviewAccess.activate(context, "PBR1.invalid"));
        assertFalse(ProBilling.isPro(context));
        setPurchased(true); ReviewAccess.clear(context);
        assertTrue(ProBilling.isPro(context));
    }
    @Test public void realSignedGrantSurvivesBillingRefreshAndReopen() {
        String code = InstrumentationRegistry.getArguments().getString("reviewCode");
        assumeNotNull(code); // Supply the private code at invocation, never commit it as a fixture.
        assertTrue(ReviewAccess.activate(context, code));
        assertTrue(ReviewAccess.active(context));
        assertFalse(context.getSharedPreferences("paperbuild_pro", Context.MODE_PRIVATE).getBoolean("owned", false));
        assertTrue(ProBilling.isPro(context));
        ProBilling.get(context).process(java.util.Collections.emptyList(), true);
        Context reopened = context.getApplicationContext();
        assertTrue(ProBilling.isPro(reopened));
        assertFalse(ReviewAccess.activate(context, "bad-code"));
        assertTrue(ProBilling.isPro(context));
        ReviewAccess.clear(context); assertFalse(ProBilling.isPro(context));
        assertTrue(ReviewAccess.activate(context, code)); setPurchased(true);
        ReviewAccess.clear(context); assertTrue(ProBilling.isPro(context));
    }
}
