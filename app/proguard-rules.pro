# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.reportit.data.model.** { *; }
-keep class kotlinx.serialization.** { *; }
-keep class io.github.jan.supabase.** { *; }