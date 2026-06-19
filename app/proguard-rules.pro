# Gson — keep data classes used for serialization
-keepclassmembers class com.fabbixmb.app.data.remote.dto.** { <fields>; }
-keepclassmembers class com.fabbixmb.app.presentation.settings.ServerExportData { <fields>; }
-keepclassmembers class com.fabbixmb.app.presentation.settings.ServerExportEntry { <fields>; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep,allowobfuscation interface retrofit2.Call
-dontwarn retrofit2.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod