# SMARTGHOST release rules (Bab 11).

# Keep WebView JS bridge surface if ever added.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep domain models used by DataStore / serialization-like access.
-keep class com.altomedia.smartghost.data.** { *; }

# Do not strip line numbers; helps safe debugging without exposing secrets.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
