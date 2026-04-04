package com.launcher.projectvoid.helper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device notification summarization via Google ML Kit GenAI Summarization API.
 *
 * Uses Gemini Nano through the ML Kit GenAI Summarization API when available.
 * Falls back gracefully with null on unsupported devices.
 *
 * The API is accessed via reflection to handle version differences across
 * ML Kit GenAI releases. Requires com.google.mlkit:genai-summarization 
 * dependency and a device with AICore installed (Pixel 8+, Samsung Galaxy S24+, etc.)
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
                // Check if AICore is installed on device
                context.packageManager.getPackageInfo("com.google.android.aicore", 0)
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
     * Falls back to a simple concatenation if AI summarization fails.
     */
    suspend fun summarize(appName: String, notificationTexts: List<String>): String? {
        if (!isAvailable()) return null
        if (notificationTexts.isEmpty()) return null

        return withContext(Dispatchers.IO) {
            try {
                // Try ML Kit GenAI via reflection to handle API differences
                val summClass = Class.forName("com.google.mlkit.genai.summarization.Summarization")
                val getClient = summClass.methods.firstOrNull { it.name == "getClient" }
                    ?: return@withContext fallbackSummarize(notificationTexts)
                
                val client = if (getClient.parameterCount == 0) {
                    getClient.invoke(null)
                } else {
                    getClient.invoke(null, context)
                } ?: return@withContext fallbackSummarize(notificationTexts)

                // Check feature status
                val checkMethod = client.javaClass.methods.firstOrNull { it.name == "checkFeatureStatus" }
                if (checkMethod != null) {
                    val statusTask = checkMethod.invoke(client)
                    val statusResult = awaitGmsTask<Any>(statusTask)
                    // If status is non-zero (unavailable), fall back
                    if (statusResult is Int && statusResult != 0) {
                        return@withContext fallbackSummarize(notificationTexts)
                    }
                }

                // Build input text
                val inputText = buildString {
                    appendLine("Summarize these notifications from $appName in 1-2 concise sentences:")
                    notificationTexts.forEach { line ->
                        appendLine("- $line")
                    }
                }

                // Try to run inference
                val inferMethod = client.javaClass.methods.firstOrNull { 
                    it.name == "runInference" && it.parameterCount == 1 
                }
                
                if (inferMethod != null) {
                    // Check if parameter is String or needs a Request object
                    val paramType = inferMethod.parameterTypes[0]
                    val param = if (paramType == String::class.java) {
                        inputText
                    } else {
                        // Try creating a SummarizationRequest via builder
                        try {
                            val reqClass = Class.forName("com.google.mlkit.genai.summarization.SummarizationRequest")
                            val builderMethod = reqClass.getMethod("builder", String::class.java)
                            val builder = builderMethod.invoke(null, inputText)
                            val buildMethod = builder.javaClass.getMethod("build")
                            buildMethod.invoke(builder)
                        } catch (_: Exception) {
                            inputText // fallback to raw string
                        }
                    }
                    
                    val inferTask = inferMethod.invoke(client, param)
                    val result = awaitGmsTask<Any>(inferTask)
                    result?.toString() ?: fallbackSummarize(notificationTexts)
                } else {
                    fallbackSummarize(notificationTexts)
                }
            } catch (e: Exception) {
                // AI failed — provide clean fallback
                fallbackSummarize(notificationTexts)
            }
        }
    }

    /**
     * Simple concatenation fallback when AI is unavailable.
     */
    private fun fallbackSummarize(texts: List<String>): String {
        return texts.take(3).joinToString(". ") + if (texts.size > 3) " (+${texts.size - 3} more)" else ""
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> awaitGmsTask(task: Any): T? = withContext(Dispatchers.IO) {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            try {
                val successListenerClass = Class.forName("com.google.android.gms.tasks.OnSuccessListener")
                val failureListenerClass = Class.forName("com.google.android.gms.tasks.OnFailureListener")

                val addSuccess = task.javaClass.getMethod("addOnSuccessListener", successListenerClass)
                val addFailure = task.javaClass.getMethod("addOnFailureListener", failureListenerClass)

                val successProxy = java.lang.reflect.Proxy.newProxyInstance(
                    task.javaClass.classLoader,
                    arrayOf(successListenerClass)
                ) { _, _, args ->
                    if (cont.isActive) cont.resume(args?.firstOrNull() as? T)
                    null
                }

                val failureProxy = java.lang.reflect.Proxy.newProxyInstance(
                    task.javaClass.classLoader,
                    arrayOf(failureListenerClass)
                ) { _, _, args ->
                    if (cont.isActive) {
                        val ex = args?.firstOrNull() as? Exception ?: RuntimeException("Unknown GMS error")
                        cont.resumeWithException(ex)
                    }
                    null
                }

                addSuccess.invoke(task, successProxy)
                addFailure.invoke(task, failureProxy)
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }
}
