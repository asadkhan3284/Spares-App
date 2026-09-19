# MSAL
-keep class com.microsoft.identity.client.** { *; }
-keep class com.microsoft.identity.common.** { *; }
-dontwarn com.microsoft.identity.**

# Retrofit / OkHttp / Moshi
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class com.sparesapp.register.graph.model.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# fastexcel
-dontwarn org.dhatim.fastexcel.**

# ML Kit barcode scanning
-keep class com.google.mlkit.vision.barcode.** { *; }
