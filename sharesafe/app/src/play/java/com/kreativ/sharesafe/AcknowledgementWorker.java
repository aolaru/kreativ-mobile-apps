package com.kreativ.sharesafe;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.QueryPurchasesParams;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Durable acknowledgement retries survive Activity destruction and process/device restarts. */
public final class AcknowledgementWorker extends Worker {
    private volatile BillingClient client;
    public AcknowledgementWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    static void enqueue(Context context, String token) {
        String identity;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            identity = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) { throw new IllegalStateException(e); }
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AcknowledgementWorker.class)
                .setInputData(new Data.Builder().putString("token", token).build())
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build();
        WorkManager.getInstance(context).enqueueUniqueWork("sharesafe-ack-" + identity, ExistingWorkPolicy.KEEP, request);
    }

    @NonNull @Override public Result doWork() {
        String token = getInputData().getString("token");
        if (token == null) return Result.failure();
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        client = BillingClient.newBuilder(getApplicationContext()).setListener((result, purchases) -> { })
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection().build();
        try {
            client.startConnection(new BillingClientStateListener() {
                public void onBillingSetupFinished(BillingResult setup) {
                    if (setup.getResponseCode() != BillingClient.BillingResponseCode.OK || isStopped()) { done.countDown(); return; }
                    client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (result, purchases) -> {
                        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK || isStopped()) { done.countDown(); return; }
                        for (com.android.billingclient.api.Purchase purchase : purchases) {
                            if (!token.equals(purchase.getPurchaseToken())) continue;
                            if (!PlayPurchaseValidator.valid(getApplicationContext(), purchase)) { done.countDown(); return; }
                            if (purchase.isAcknowledged()) { success.set(true); done.countDown(); return; }
                            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(), ack -> {
                                success.set(ack.getResponseCode() == BillingClient.BillingResponseCode.OK); done.countDown();
                            }); return;
                        }
                        // Token is no longer owned (refund/revocation): nothing left to acknowledge.
                        success.set(true); done.countDown();
                    });
                }
                public void onBillingServiceDisconnected() { done.countDown(); }
            });
            return done.await(40, TimeUnit.SECONDS) && success.get() ? Result.success() : Result.retry();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return Result.retry();
        } catch (RuntimeException e) { return Result.retry(); }
        finally { client.endConnection(); }
    }
    @Override public void onStopped() { if (client != null) client.endConnection(); }
}
