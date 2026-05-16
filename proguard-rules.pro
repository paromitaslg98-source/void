# VOID Launcher ProGuard Rules
# =============================

# Suppress warnings for Google Error Prone annotations (transitive from Tink/AndroidX Security).
# Compile-time-only annotations, stripped at runtime — safe to ignore.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# ── Integrated-flavor: ML Kit GenAI ───────────────────────────────────────────
# ML Kit on-device feature delivery loads classes via reflection at runtime.
# These keep rules are no-ops for the disintegrated build because the classes
# are not on the classpath; R8 just ignores keeps for absent classes.
-keep class com.google.mlkit.genai.** { *; }
-dontwarn com.google.mlkit.genai.**
-keep class com.google.android.gms.internal.mlkit_genai_summarization.** { *; }
-keep class com.google.android.gms.internal.mlkit_genai_prompt.** { *; }

# Guava ListenableFuture used by the ML Kit GenAI futures adapter.
-keep interface com.google.common.util.concurrent.ListenableFuture { *; }
-keep class com.google.common.util.concurrent.** { *; }

# ── Kotlinx Serialization (@Serializable Compose navigation routes) ───────────
# Type-safe Navigation-Compose routes are @Serializable data objects/classes.
# R8 must keep the generated $serializer companion fields or NavType lookup fails at runtime.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class <1>.<2> {
    <1>.<2>$Companion Companion;
}
-keep,includedescriptorclasses class com.knownassurajit.app.launcher.voidlauncher.**$$serializer { *; }
-keepclassmembers class com.knownassurajit.app.launcher.voidlauncher.** {
    *** Companion;
}
-keepclasseswithmembers class com.knownassurajit.app.launcher.voidlauncher.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Kotlin coroutines internals (reflection-based dispatcher lookup) ──────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
