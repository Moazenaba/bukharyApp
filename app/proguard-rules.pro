# Add project specific ProGuard rules here.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Moshi & Models
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.example.data.model.** { *; }
-keep class com.example.data.remote.** { *; }
-keep class com.example.data.local.entities.** { *; }


# Tink (used by androidx.security:security-crypto) references compile-time-only Error Prone
# annotations that are not on the runtime classpath; R8 fails the release build without this.
-dontwarn com.google.errorprone.annotations.**
