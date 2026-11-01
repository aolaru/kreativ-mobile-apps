# The separately shrunk test runner links to Kotlin classes loaded from the tested APK.
# Keep Kotlin only in the isolated releasecheck build so the runner can link to that copy.
-keep class kotlin.** { *; }
# AndroidX Test also calls tracing code loaded from the tested APK.
-keep class androidx.tracing.Trace { *; }
-keep class androidx.lifecycle.** { *; }
-keep,allowoptimization class androidx.core.content.FileProvider { *; }
# The release smoke tests directly call these app and engine APIs across the APK boundary.
# Retain their signatures in QA while allowing their method bodies to be optimised.
-keep,allowoptimization class com.kreativ.photofeather.FeatherApp { *; }
-keep,allowoptimization class com.kreativ.photofeather.MainActivity { *; }
-keep,allowoptimization class com.kreativ.photofeather.Session { *; }
-keep,allowoptimization class com.kreativ.photofeather.PhotoItem { *; }
-keep,allowoptimization class com.kreativ.photofeather.ExportManager { *; }
-keep,allowoptimization class com.kreativ.imageengine.ImageEngine { *; }
-keep,allowoptimization class com.kreativ.imageengine.ImageEngine$** { *; }
-keep,allowoptimization class com.kreativ.imageengine.CompressionOptions { *; }
