# Capacitor's consumer rules keep its JavaScript bridge and plugin callbacks.
# Custom plugin methods are reflected by Capacitor and must retain their names.
# Keep runtime annotation types and defaults as well as annotated plugin classes.
# Full-mode R8 otherwise may remove metadata that the Capacitor 8 bridge reflects.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault
-keep @interface com.getcapacitor.annotation.** { *; }
-keep @interface com.getcapacitor.NativePlugin { *; }
-keep @interface com.getcapacitor.PluginMethod { *; }
-keep @com.getcapacitor.annotation.CapacitorPlugin class * { *; }
-keepclassmembers class * {
    @com.getcapacitor.PluginMethod <methods>;
    @com.getcapacitor.annotation.ActivityCallback <methods>;
    @com.getcapacitor.annotation.PermissionCallback <methods>;
}
# Room 2.6 (used by WorkManager) instantiates generated database implementations
# by reflection. AGP 9 no longer implicitly keeps a kept class's constructor.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
