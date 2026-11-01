# AndroidX Test references these non-runtime Error Prone annotations in class metadata.
# They are not Android runtime classes. Scope these exact R8-generated exclusions to
# the instrumentation APK; do not suppress missing classes in the production app.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.MustBeClosed
