package com.launcher.projectvoid.helper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

/**
 * On-device notification summarization with tiered AI engine selection.
 *
 * Tier 1 — ML Kit GenAI Prompt API (genai-prompt:1.0.0-beta2)
 *   Full contextual summarization with custom prompt engineering.
 *   Supported on: Pixel 9+, Galaxy S24+, Xiaomi 14T Pro+, etc.
 *
 * Tier 2 — ML Kit GenAI Summarization API (genai-summarization:1.0.0-beta1)
 *   Basic AI summarization with LoRA adapter.
 *   Supported on: Pixel 8a and older AICore devices.
 *
 * Tier 3 — Structured fallback (no AI)
 *   Clean bullet-point formatting of raw notification text.
 *   Works on all devices regardless of hardware.
 *
 * The engine probes Tier 1 first, falls back to Tier 2, then Tier 3.
 * All inference is on-device via Android AICore — zero cloud calls.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiSummarizer(private val context: Context) {

    companion object {
        private const val TAG = "AiSummarizer"

        /** Deterministic prompt for Gemini Nano via Prompt API. */
        private const val SYSTEM_PROMPT = """ROLE: You are a concise notification summarizer.
STRICT RULES:
1. Output ONLY a bulleted list. Start each line with "• ".
2. One bullet per notification. Preserve ALL numbers, OTPs, dates, and amounts exactly.
3. For media attachments, mention the media type (image/video/audio) contextually without describing the media content itself.
4. Be maximally concise — no filler, no introductions, no conclusions.
5. If there is only one notification, output one bullet.
6. Summarize the core intent or action of each notification."""
    }

    /** Detected engine tier: 1 = Prompt, 2 = Summarization, 3 = Fallback, null = not yet probed. */
    @Volatile
    private var detectedTier: Int? = null

    // Tier 1: Prompt API client (lazy, nullable)
    private var promptClient: com.google.mlkit.genai.prompt.GenerativeModel? = null

    /**
     * Check if any on-device AI is available and determine the engine tier.
     * Returns true for Tier 1 or 2, false for Tier 3.
     */
    suspend fun isAvailable(): Boolean {
        detectedTier?.let { return it <= 2 }
        return withContext(Dispatchers.IO) {
            detectTier() <= 2
        }
    }

    /**
     * Returns the detected tier (1, 2, or 3). Probes if not yet determined.
     */
    suspend fun getTier(): Int {
        detectedTier?.let { return it }
        return withContext(Dispatchers.IO) { detectTier() }
    }

    /**
     * Probe device capabilities and select the best engine tier.
     * Must be called on a background thread.
     */
    private fun detectTier(): Int {
        // First, check if AICore is even installed
        val hasAiCore = try {
            context.packageManager.getPackageInfo("com.google.android.aicore", 0)
            true
        } catch (_: Exception) {
            false
        }

        if (!hasAiCore) {
            Log.d(TAG, "AICore not installed → Tier 3 (fallback)")
            detectedTier = 3
            return 3
        }

        // Try Tier 1: Prompt API
        try {
            val client = com.google.mlkit.genai.prompt.Generation.getClient()
            val status = kotlinx.coroutines.runBlocking {
                client.checkStatus()
            }
            val statusValue = status as? Int ?: -1
            // FeatureStatus.AVAILABLE = 0, DOWNLOADABLE = 1
            if (statusValue == 0 || statusValue == 1) {
                promptClient = client
                detectedTier = 1
                Log.d(TAG, "Prompt API available → Tier 1 (contextual)")
                return 1
            }
        } catch (e: Exception) {
            Log.d(TAG, "Prompt API probe failed: ${e.message}")
        }

        // Try Tier 2: Summarization API
        try {
            val summClass = Class.forName("com.google.mlkit.genai.summarization.Summarization")
            val getClient = summClass.methods.firstOrNull { it.name == "getClient" }
            if (getClient != null) {
                val client = if (getClient.parameterCount == 0) {
                    getClient.invoke(null)
                } else {
                    getClient.invoke(null, context)
                }
                if (client != null) {
                    val checkMethod = client.javaClass.methods.firstOrNull { it.name == "checkFeatureStatus" }
                    if (checkMethod != null) {
                        val statusTask = checkMethod.invoke(client)
                        val statusResult = awaitGmsTask<Any>(statusTask)
                        if (statusResult is Int && statusResult <= 1) {
                            detectedTier = 2
                            Log.d(TAG, "Summarization API available → Tier 2 (basic)")
                            return 2
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Summarization API probe failed: ${e.message}")
        }

        Log.d(TAG, "No AI engine available → Tier 3 (fallback)")
        detectedTier = 3
        return 3
    }

    /**
     * Summarize notifications from a single application.
     *
     * @param appName Display name of the originating application.
     * @param notificationTexts List of extracted notification strings (may include media metadata markers).
     * @return Bulleted summary string, or null if summarization failed entirely.
     */
    suspend fun summarize(appName: String, notificationTexts: List<String>): String? {
        if (notificationTexts.isEmpty()) return null

        val tier = getTier()
        return withContext(Dispatchers.IO) {
            when (tier) {
                1 -> summarizeWithPromptApi(appName, notificationTexts)
                2 -> summarizeWithSummarizationApi(appName, notificationTexts)
                else -> fallbackSummarize(notificationTexts)
            }
        }
    }

    // ── Tier 1: Prompt API ──────────────────────────────────────────────

    private suspend fun summarizeWithPromptApi(appName: String, texts: List<String>): String {
        try {
            val client = promptClient ?: com.google.mlkit.genai.prompt.Generation.getClient()
            promptClient = client

            // Build the notification payload with XML delimiters for prompt injection safety
            val payload = buildString {
                appendLine(SYSTEM_PROMPT)
                appendLine()
                appendLine("<notifications app=\"$appName\">")
                // Token budget: ~3000 words max. Truncate oldest if too long.
                val truncated = truncateForTokenBudget(texts)
                truncated.forEachIndexed { i, text ->
                    appendLine("[${i + 1}] $text")
                }
                appendLine("</notifications>")
                appendLine()
                appendLine("SUMMARY:")
            }

            // Build request with deterministic parameters
            val request = com.google.mlkit.genai.prompt.generateContentRequest(
                com.google.mlkit.genai.prompt.TextPart(payload)
            ) {
                temperature = 0.1f
                topK = 5
                maxOutputTokens = 256
            }

            val response = client.generateContent(request)
            val result = response.candidates.firstOrNull()?.text
            if (!result.isNullOrBlank()) {
                return result.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Prompt API inference failed: ${e.message}")
        }
        // Fallback if Prompt API fails at runtime
        return fallbackSummarize(texts)
    }

    // ── Tier 2: Summarization API ───────────────────────────────────────

    private suspend fun summarizeWithSummarizationApi(appName: String, texts: List<String>): String {
        try {
            val summClass = Class.forName("com.google.mlkit.genai.summarization.Summarization")
            val getClient = summClass.methods.firstOrNull { it.name == "getClient" }
                ?: return fallbackSummarize(texts)

            val client = if (getClient.parameterCount == 0) {
                getClient.invoke(null)
            } else {
                getClient.invoke(null, context)
            } ?: return fallbackSummarize(texts)

            val inputText = buildString {
                appendLine("Notifications from $appName:")
                texts.forEach { line ->
                    appendLine("- $line")
                }
            }

            val inferMethod = client.javaClass.methods.firstOrNull {
                it.name == "runInference" && it.parameterCount == 1
            }

            if (inferMethod != null) {
                val paramType = inferMethod.parameterTypes[0]
                val param = if (paramType == String::class.java) {
                    inputText
                } else {
                    try {
                        val reqClass = Class.forName("com.google.mlkit.genai.summarization.SummarizationRequest")
                        val builderMethod = reqClass.getMethod("builder", String::class.java)
                        val builder = builderMethod.invoke(null, inputText)
                        val buildMethod = builder.javaClass.getMethod("build")
                        buildMethod.invoke(builder)
                    } catch (_: Exception) {
                        inputText
                    }
                }

                val inferTask = inferMethod.invoke(client, param)
                val result = awaitGmsTask<Any>(inferTask)
                result?.toString()?.let { summary ->
                    if (summary.isNotBlank()) return summary
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Summarization API inference failed: ${e.message}")
        }
        return fallbackSummarize(texts)
    }

    // ── Tier 3: Structured Fallback ─────────────────────────────────────

    /**
     * Structured bullet-point formatting of raw notification text.
     * No AI involved — works on every device.
     */
    private fun fallbackSummarize(texts: List<String>): String {
        return texts.joinToString("\n") { "• $it" }
    }

    // ── Utilities ───────────────────────────────────────────────────────

    /**
     * Truncate notification list to fit within ~3000 word token budget.
     * Drops oldest (first) entries, retaining the most recent.
     */
    private fun truncateForTokenBudget(texts: List<String>, maxWords: Int = 2800): List<String> {
        var totalWords = 0
        val result = mutableListOf<String>()
        // Iterate from newest (last) to oldest (first)
        for (text in texts.reversed()) {
            val wordCount = text.split("\\s+".toRegex()).size
            if (totalWords + wordCount > maxWords && result.isNotEmpty()) break
            result.add(0, text)
            totalWords += wordCount
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> awaitGmsTask(task: Any): T? {
        return try {
            kotlinx.coroutines.runBlocking {
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
                            if (cont.isActive) cont.resume(args?.firstOrNull() as? T) {}
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
                        if (cont.isActive) cont.resume(null) {}
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
