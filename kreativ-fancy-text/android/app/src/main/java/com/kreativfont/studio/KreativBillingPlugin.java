package com.kreativfont.studio;

import android.os.Handler;
import android.os.Looper;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Only native, signed Play purchases can grant a shipping Android entitlement. */
@CapacitorPlugin(name = "KreativBilling")
public class KreativBillingPlugin extends Plugin {
    private KreativBillingController controller;
    private String message = "Connecting to Google Play. The free editor is ready to use.";
    private boolean actionMessage;
    private final Handler main = new Handler(Looper.getMainLooper());
    @Override public void load() {
        controller = new KreativBillingController(getContext(), new KreativBillingController.Listener() {
            public void onBillingMessage(String text) { actionMessage = true; message = text; changed(); }
            public void onProductChanged() { if (!actionMessage) message = controller.purchaseStatus(); changed(); }
            public void onProStatusChanged(boolean enabled) { changed(); }
        });
        main.post(controller::connect);
    }
    private JSObject snapshot() {
        JSObject result = new JSObject();
        result.put("owned", controller != null && controller.isProEnabled());
        result.put("available", controller != null && controller.canPurchase());
        result.put("price", controller == null ? null : controller.price());
        result.put("native", true);
        result.put("pending", controller != null && controller.isPending());
        result.put("purchasing", controller != null && controller.isPurchasing());
        result.put("message", message);
        if (controller != null) result.put("diagnostics", controller.diagnostics());
        return result;
    }
    private void changed() { notifyListeners("billingStateChanged", snapshot()); }
    @PluginMethod public void getStatus(PluginCall call) {
        main.post(() -> { call.resolve(snapshot()); controller.refresh(); });
    }
    @PluginMethod public void purchase(PluginCall call) {
        main.post(() -> {
            if (controller.isProEnabled()) { call.resolve(snapshot()); return; }
            actionMessage = false;
            controller.launchPurchase(getActivity()); call.resolve(snapshot());
        });
    }
    @PluginMethod public void restore(PluginCall call) {
        main.post(() -> { actionMessage = true; message = "Checking purchases with Google Play…"; controller.restore(); call.resolve(snapshot()); });
    }
    @Override protected void handleOnResume() { if (controller != null) main.post(controller::refresh); }
    @Override protected void handleOnDestroy() { main.removeCallbacksAndMessages(null); if (controller != null) controller.destroy(); }
}
