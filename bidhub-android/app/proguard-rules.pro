# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep API client classes
-keep class com.cc106.bidhub.api.** { *; }
-keep class com.cc106.bidhub.models.** { *; }

# Keep model classes
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

