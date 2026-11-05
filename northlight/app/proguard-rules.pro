# WorkManager persists the worker's class name across application upgrades.
-keep class com.kreativ.northlight.AcknowledgeWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
