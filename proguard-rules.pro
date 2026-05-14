# IMPSTR ProGuard Rules
# =====================

# Suppress warnings for Google Error Prone annotations (transitive dependency from Tink/AndroidX Security).
# These are compile-time-only annotations stripped at runtime — safe to ignore.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ML Kit GenAI — on-device feature delivery loads classes via reflection at runtime
-keep class com.google.mlkit.genai.** { *; }
-dontwarn com.google.mlkit.genai.**
-keep class com.google.android.gms.internal.mlkit_genai_summarization.** { *; }
-keep class com.google.android.gms.internal.mlkit_genai_prompt.** { *; }

# Guava ListenableFuture used by ML Kit GenAI futures adapter
-keep interface com.google.common.util.concurrent.ListenableFuture { *; }
-keep class com.google.common.util.concurrent.** { *; }

# Kotlin coroutines internal machinery (uses reflection for dispatcher lookup)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
