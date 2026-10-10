package com.kreativ.northlight;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.android.billingclient.api.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** WorkManager persists receipt confirmation across process death and network loss. No payment details. */
public class AcknowledgeWorker extends Worker {
    public AcknowledgeWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }
    static void enqueue(Context context, String token) {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(AcknowledgeWorker.class)
                .setInputData(new Data.Builder().putString("token", token).build())
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(workName(token), ExistingWorkPolicy.KEEP, work);
    }
    static String workName(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder("northlight-ack-");
            for (byte b : digest) name.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            return name.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    @NonNull @Override public Result doWork() {
        String token = getInputData().getString("token");
        if (token == null || token.isEmpty()) return Result.failure();
        Handler main = new Handler(Looper.getMainLooper());
        AtomicReference<BillingClient> connection = new AtomicReference<>();
        AtomicInteger code = new AtomicInteger(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE);
        CountDownLatch connected = new CountDownLatch(1), acknowledged = new CountDownLatch(1);
        try {
            main.post(() -> {
                BillingClient client = BillingClient.newBuilder(getApplicationContext()).setListener((result, purchases) -> { })
                        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                        .enableAutoServiceReconnection().build();
                connection.set(client);
                client.startConnection(new BillingClientStateListener() {
                    @Override public void onBillingSetupFinished(BillingResult result) { code.set(result.getResponseCode()); connected.countDown(); }
                    @Override public void onBillingServiceDisconnected() { }
                });
            });
            if (!connected.await(20, TimeUnit.SECONDS) || code.get() != BillingClient.BillingResponseCode.OK || isStopped()) return Result.retry();
            code.set(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE);
            main.post(() -> connection.get().acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(), result -> {
                code.set(result.getResponseCode()); acknowledged.countDown();
            }));
            if (!acknowledged.await(20, TimeUnit.SECONDS)) return Result.retry();
            if (code.get() == BillingClient.BillingResponseCode.OK) return Result.success();
            // Revoked/expired ownership cannot be acknowledged; foreground inventory reconciles access.
            if (code.get() == BillingClient.BillingResponseCode.ITEM_NOT_OWNED || code.get() == BillingClient.BillingResponseCode.DEVELOPER_ERROR) return Result.failure();
            return Result.retry();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); return Result.retry();
        } finally {
            main.post(() -> { BillingClient client = connection.get(); if (client != null) client.endConnection(); });
        }
    }
}
