# AndroidX Test references these compile-time Error Prone annotations, but does
# not package them. They are not needed at runtime by the instrumentation APK.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.MustBeClosed
