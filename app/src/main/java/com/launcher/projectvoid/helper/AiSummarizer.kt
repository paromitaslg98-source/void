package com.launcher.projectvoid.helper

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.mlkit.genai.summarization.FeatureStatus
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

/**
 * On-device notification summarization with tiered AI engine selection.
 *
 * Tier 1 — ML Kit GenAI Prompt API (genai-prompt:1.0.0-beta2)
 * Tier 2 — ML Kit GenAI Summarization API (genai-summarization:1.0.0-beta1)
 * Tier 3 — Structured fallback (no AI)
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiSummarizer(private val context: Context) {

    companion object {
        private const val TAG = "AiSummarizer"
        private const val AICORE_PACKAGE = "com.google.android.aicore"

        /**
         * This list intentionally errs on the side of safety for known-bad device/model combos.
         * We can expand this as we collect diagnostics from the field.
         */
        private val MODEL_DENYLIST_SUBSTRINGS = setOf<String>()
        private val MANUFACTURER_DENYLIST = setOf<String>()

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

    /** Last structured diagnostic captured during capability detection. */
    @Volatile
    private var lastCapabilityDiagnostic: CapabilityDiagnostic? = null

    // Tier 1: Prompt API client (lazy, nullable)
    private var promptClient: com.google.mlkit.genai.prompt.GenerativeModel? = null

    // Tier 2: typed summarizer client (preferred at compile/runtime)
    private var summarizerClient: Summarizer? = null

    data class CapabilityMetadata(
        val sdkInt: Int,
        val device: String,
        val model: String,
        val manufacturer: String,
        val hasAiCore: Boolean,
        val promptStatus: Int? = null,
        val summarizationStatus: Int? = null
    )

    data class CapabilityDiagnostic(
        val tier: Int,
        val reason: String,
        val metadata: CapabilityMetadata
    )

    /**
     * Public accessor so callers can persist/emit diagnostics externally if needed.
     */
    fun getLastCapabilityDiagnostic(): CapabilityDiagnostic? = lastCapabilityDiagnostic

    /**
     * Check if any on-device AI is available and determine the engine tier.
     * Returns true for Tier 1 or 2, false for Tier 3.
     */
    suspend fun isAvailable(): Boolean {
        detectedTier?.let { return it <= 2 }
        return withContext(Dispatchers.IO) {
            detectTierAndPersistDiagnostic().tier <= 2
        }
    }

    /**
     * Returns the detected tier (1, 2, or 3). Probes if not yet determined.
     */
    suspend fun getTier(): Int {
        detectedTier?.let { return it }
        return withContext(Dispatchers.IO) { detectTierAndPersistDiagnostic().tier }
    }

    /**
     * Unified decision function for capabilities that includes:
     * - AICore package presence
     * - API feature status for prompt + summarization
     * - Device/API metadata and allow/deny checks
     */
    private suspend fun decideBestTier(): CapabilityDiagnostic {
        val baseMetadata = CapabilityMetadata(
            sdkInt = Build.VERSION.SDK_INT,
            device = Build.DEVICE.orEmpty(),
            model = Build.MODEL.orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            hasAiCore = hasAiCoreInstalled()
        )

        if (!baseMetadata.hasAiCore) {
            return CapabilityDiagnostic(
                tier = 3,
                reason = "aicore_missing",
                metadata = baseMetadata
            )
        }

        if (isDeviceDenied(baseMetadata)) {
            return CapabilityDiagnostic(
                tier = 3,
                reason = "device_denylist",
                metadata = baseMetadata
            )
        }

        // Tier 1 probe (Prompt API).
        val promptStatus = try {
            val client = com.google.mlkit.genai.prompt.Generation.getClient()
            val status = client.checkStatus().await()
            promptClient = client
            status as? Int
        } catch (e: Exception) {
            Log.d(TAG, "Prompt API probe failed: ${e.message}")
            null
        }

        if (promptStatus == 0 || promptStatus == 1) {
            return CapabilityDiagnostic(
                tier = 1,
                reason = "prompt_available_or_downloadable",
                metadata = baseMetadata.copy(promptStatus = promptStatus)
            )
        }

        // Tier 2 probe (typed API first, reflection fallback only if needed).
        val summarizationTypedStatus = tryTypedSummarizationStatus()
        if (summarizationTypedStatus != null &&
            (summarizationTypedStatus == FeatureStatus.AVAILABLE || summarizationTypedStatus == FeatureStatus.DOWNLOADABLE)
        ) {
            return CapabilityDiagnostic(
                tier = 2,
                reason = "summarization_typed_available_or_downloadable",
                metadata = baseMetadata.copy(
                    promptStatus = promptStatus,
                    summarizationStatus = summarizationTypedStatus
                )
            )
        }

        val summarizationReflectedStatus = tryReflectedSummarizationStatus()
        if (summarizationReflectedStatus == 0 || summarizationReflectedStatus == 1) {
            return CapabilityDiagnostic(
                tier = 2,
                reason = "summarization_reflection_available_or_downloadable",
                metadata = baseMetadata.copy(
                    promptStatus = promptStatus,
                    summarizationStatus = summarizationReflectedStatus
                )
            )
        }

        return CapabilityDiagnostic(
            tier = 3,
            reason = "no_ai_feature_available",
            metadata = baseMetadata.copy(
                promptStatus = promptStatus,
                summarizationStatus = summarizationTypedStatus ?: summarizationReflectedStatus
            )
        )
    }

    private suspend fun detectTierAndPersistDiagnostic(): CapabilityDiagnostic {
        val diagnostic = decideBestTier()
        detectedTier = diagnostic.tier
        lastCapabilityDiagnostic = diagnostic

        Log.d(
            TAG,
            "Capability decision -> tier=${diagnostic.tier}, reason=${diagnostic.reason}, metadata=${diagnostic.metadata}"
        )
        return diagnostic
    }

    private fun hasAiCoreInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(AICORE_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isDeviceDenied(metadata: CapabilityMetadata): Boolean {
        val manufacturerDenied = MANUFACTURER_DENYLIST.any {
            metadata.manufacturer.equals(it, ignoreCase = true)
        }
        val modelDenied = MODEL_DENYLIST_SUBSTRINGS.any {
            metadata.model.contains(it, ignoreCase = true)
        }

        // Keep this explicit so we can evolve policy without touching probe flow.
        return manufacturerDenied || modelDenied
    }

    private suspend fun tryTypedSummarizationStatus(): Int? {
        return try {
            val summarizer = summarizerClient ?: Summarization.getClient(context).also { summarizerClient = it }
            summarizer.checkFeatureStatus().await()
        } catch (e: Exception) {
            Log.d(TAG, "Typed summarization probe failed: ${e.message}")
            null
        }
    }

    private fun tryReflectedSummarizationStatus(): Int? {
        return try {
            val summClass = Class.forName("com.google.mlkit.genai.summarization.Summarization")
            val getClient = summClass.methods.firstOrNull { it.name == "getClient" } ?: return null
            val client = if (getClient.parameterCount == 0) getClient.invoke(null) else getClient.invoke(null, context)
                ?: return null
            val checkMethod = client.javaClass.methods.firstOrNull { it.name == "checkFeatureStatus" } ?: return null
            val statusTask = checkMethod.invoke(client)
            awaitGmsTask<Int>(statusTask)
        } catch (e: Exception) {
            Log.d(TAG, "Reflected summarization probe failed: ${e.message}")
            null
        }
    }

    /**
     * Summarize notifications from a single application.
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

            val payload = buildString {
                appendLine(SYSTEM_PROMPT)
                appendLine()
                appendLine("<notifications app=\"$appName\">")
                val truncated = truncateForTokenBudget(texts)
                truncated.forEachIndexed { i, text -> appendLine("[${i + 1}] $text") }
                appendLine("</notifications>")
                appendLine()
                appendLine("SUMMARY:")
            }

            val request = com.google.mlkit.genai.prompt.generateContentRequest(
                com.google.mlkit.genai.prompt.TextPart(payload)
            ) {
                temperature = 0.1f
                topK = 5
                maxOutputTokens = 256
            }

            val response = client.generateContent(request)
            val result = response.candidates.firstOrNull()?.text
            if (!result.isNullOrBlank()) return result.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Prompt API inference failed: ${e.message}")
        }
        return fallbackSummarize(texts)
    }

    // ── Tier 2: Summarization API ───────────────────────────────────────

    private suspend fun summarizeWithSummarizationApi(appName: String, texts: List<String>): String {
        val inputText = buildString {
            appendLine("Notifications from $appName:")
            texts.forEach { line -> appendLine("- $line") }
        }

        // Preferred path: direct typed API usage for compile-time safety.
        try {
            val summarizer = summarizerClient ?: Summarization.getClient(context).also { summarizerClient = it }
            val request = SummarizationRequest.builder(inputText).build()
            val result = summarizer.runInference(request).await()
            if (!result.isNullOrBlank()) return result
        } catch (e: Exception) {
            Log.e(TAG, "Typed summarization inference failed: ${e.message}")
        }

        // Guarded fallback path: reflection only if typed path fails at runtime.
        try {
            val summClass = Class.forName("com.google.mlkit.genai.summarization.Summarization")
            val getClient = summClass.methods.firstOrNull { it.name == "getClient" }
                ?: return fallbackSummarize(texts)

            val client = if (getClient.parameterCount == 0) {
                getClient.invoke(null)
            } else {
                getClient.invoke(null, context)
            } ?: return fallbackSummarize(texts)

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
                result?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reflection summarization inference failed: ${e.message}")
        }

        return fallbackSummarize(texts)
    }

    // ── Tier 3: Structured Fallback ─────────────────────────────────────

    private fun fallbackSummarize(texts: List<String>): String {
        return texts.joinToString("\n") { "• $it" }
    }

    // ── Utilities ───────────────────────────────────────────────────────

    private fun truncateForTokenBudget(texts: List<String>, maxWords: Int = 2800): List<String> {
        var totalWords = 0
        val result = mutableListOf<String>()
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
