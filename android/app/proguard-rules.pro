# Linkpoint Android ProGuard Rules

# Keep all viewer core classes
-keep class com.linkpoint.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep compose classes
-keep class androidx.compose.** { *; }

# Keep reflection for Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.linkpoint.**$$serializer { *; }
-keepclassmembers class com.linkpoint.** {
    *** Companion;
}

# Virtual world specific optimizations
-keep class com.linkpoint.protocol.** { *; }
-keep class com.linkpoint.graphics.** { *; }
-keep class com.linkpoint.audio.** { *; }
-keep class com.linkpoint.assets.** { *; }