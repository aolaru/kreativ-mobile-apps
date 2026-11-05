# AndroidX Test's runner uses tracing from the target app's shared dependency.
# Keep it only in the isolated test build, where the test APK cannot bundle it.
-keep class androidx.tracing.Trace { *; }
# AndroidX Test also links against the target's Kotlin runtime before tests start.
-keep class kotlin.** { *; }
