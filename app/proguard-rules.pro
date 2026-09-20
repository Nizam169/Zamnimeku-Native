# Proguard rules for Zamnimeku Native
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-dontwarn okhttp3.**
-dontwarn org.jsoup.**
