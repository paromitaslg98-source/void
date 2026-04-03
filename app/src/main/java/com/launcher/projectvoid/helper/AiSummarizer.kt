package com.launcher.projectvoid.helper

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device notification summarization via Google AI Core / ML Kit GenAI.
 *
 * Uses Gemini Nano through the ML Kit GenAI Summarization API when available.
 * Falls back gracefully with null on unsupported devices.
 *
 * Requires: com.google.mlkit:genai-summarization dependency and a device
 * with AICore installed (Pixel 8+, Samsung Galaxy S24+, etc.)
 */
class AiSummarizer(private val context: Context) {

    @Volatile
    private var available: Boolean? = null

    /**
     * Check if AICore / ML Kit GenAI is available on this device.
     */
    suspend fun isAvailable(): Boolean {
        available?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                context.packageManager.getPackageInfo("com.google.android.aicore", 0)
                // Also try loading the ML Kit class
                Class.forName("com.google.mlkit.genai.summarization.Summarization")
                available = true
                true
            } catch (e: Exception) {
                available = false
                false
            }
        }
    }

    /**
     * Summarize a list of notification texts from one app.
     * Returns null if AI is unavailable or inference fails.
     */
    suspend fun summarize(appName: String, notificationTexts: List<String>): String? {
        if (!isAvailable()) return null
        if (notificationTexts.isEmpty()) return null

        return withContext(Dispatchers.IO) {
            try {
                val inputText = buildString {
                    appendLine("Summarize these notifications from $appName in 1-2 concise sentences:")
                    notificationTexts.forEach { line ->
                        appendLine("- $line")
                    }
                }

                // ML Kit GenAI Summarization — reflection-based call to avoid hard
                // compile-time coupling. The actual dependency must be in the classpath.
                val summClass = Class.forName("com.google.mlkit.genai.summarization.Summarization")
                val getClient = summClass.getMethod("getClient")
                val client = getClient.invoke(null)

                // Check feature status first
                val checkMethod = client.javaClass.getMethod("checkFeatureStatus")
                val statusTask = checkMethod.invoke(client)

                val statusResult = awaitTask<Int>(statusTask)
                if (statusResult != 0) return@withContext null // 0 = DOWNLOADABLE/AVAILABLE

                // Run inference
                val inferMethod = client.javaClass.getMethod("runInference", String::class.java)
                val inferTask = inferMethod.invoke(client, inputText)

                awaitTask<String>(inferTask)
            } catch (e: Exception) {
                available = false
                null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> awaitTask(task: Any): T = withContext(Dispatchers.IO) {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            try {
                val addSuccess = task.javaClass.getMethod(
                    "addOnSuccessListener",
                    Class.forName("com.google.android.gms.tasks.OnSuccessListener")
                )
                val addFailure = task.javaClass.getMethod(
                    "addOnFailureListener",
                    Class.forName("com.google.android.gms.tasks.OnFailureListener")
                )

                val successProxy = java.lang.reflect.Proxy.newProxyInstance(
                    task.javaClass.classLoader,
                    arrayOf(Class.forName("com.google.android.gms.tasks.OnSuccessListener"))
                ) { _, _, args ->
                    cont.resume(args[0] as T) { }
                    null
                }

                val failureProxy = java.lang.reflect.Proxy.newProxyInstance(
                    task.javaClass.classLoader,
                    arrayOf(Class.forName("com.google.android.gms.tasks.OnFailureListener"))
                ) { _, _, args ->
                    cont.resumeWith(Result.failure(args[0] as Exception))
                    null
                }

                addSuccess.invoke(task, successProxy)
                addFailure.invoke(task, failureProxy)
            } catch (e: Exception) {
                cont.resumeWith(Result.failure(e))
            }
        }
    }
}
