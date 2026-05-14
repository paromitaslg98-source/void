package com.knownassurajit.app.launcher.voidlauncher.helper

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import com.google.mlkit.genai.summarization.SummarizerOptions.InputType
import com.google.mlkit.genai.summarization.SummarizerOptions.OutputType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.mlkit.genai.common.FeatureStatus
import com.google.common.util.concurrent.ListenableFuture

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

        internal fun mapPromptStatusToTier(status: Int): Int? = when (status) {
            FeatureStatus.AVAILABLE -> 1
            else -> null
        }

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
            val futuresClient = com.google.mlkit.genai.prompt.java.GenerativeModelFutures.from(client)
            val status = futuresClient.checkStatus().awaitTask()
            promptClient = client
            status
        } catch (e: Exception) {
            Log.d(TAG, "Prompt API probe failed: ${e.message}")
            null
        }

        if (promptStatus == FeatureStatus.AVAILABLE || promptStatus == FeatureStatus.DOWNLOADABLE) {
            return CapabilityDiagnostic(
                tier = 1,
                reason = "prompt_available_or_downloadable",
                metadata = baseMetadata.copy(promptStatus = promptStatus)
            )
        }

        // Tier 2 probe (typed API only).
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

        return CapabilityDiagnostic(
            tier = 3,
            reason = "no_ai_feature_available",
            metadata = baseMetadata.copy(
                promptStatus = promptStatus,
                summarizationStatus = summarizationTypedStatus
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
            val options = SummarizerOptions.builder(context)
                .setInputType(InputType.CONVERSATION)
                .setOutputType(OutputType.ONE_BULLET)
                .build()
            val summarizer = summarizerClient ?: Summarization.getClient(options).also { summarizerClient = it }
            summarizer.checkFeatureStatus().awaitTask()
        } catch (e: Exception) {
            Log.d(TAG, "Typed summarization probe failed: ${e.message}")
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
                appendLine("<notifications app=\"${escapeXmlAttribute(appName)}\">")
                // Token budget: ~3000 words max. Truncate oldest if too long.
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

        // Typed API usage for compile-time safety.
        try {
            val options = SummarizerOptions.builder(context)
                .setInputType(InputType.CONVERSATION)
                .setOutputType(OutputType.ONE_BULLET)
                .build()
            val summarizer = summarizerClient ?: Summarization.getClient(options).also { summarizerClient = it }
            val request = SummarizationRequest.builder(inputText).build()
            val result = summarizer.runInference(request).awaitTask()
            val summary = result.summary
            if (!summary.isNullOrBlank()) return summary
        } catch (e: Exception) {
            Log.e(TAG, "Typed summarization inference failed: ${e.message}")
        }

        return fallbackSummarize(texts)
    }

    // ── Tier 3: Structured Fallback ─────────────────────────────────────
    // Produces minimum-token output: extracts title + truncated body rather than
    // dumping full notification text, keeping each bullet under ~80 characters.

    private fun fallbackSummarize(texts: List<String>): String {
        return texts
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { text ->
                val colonIdx = text.indexOf(':').takeIf { it in 1..50 }
                when {
                    colonIdx != null -> {
                        val title = text.substring(0, colonIdx).trim()
                        val body = text.substring(colonIdx + 1).trim()
                        if (body.isBlank()) title else "$title: ${body.take(70)}"
                    }
                    text.length > 80 -> text.take(80) + "…"
                    else -> text
                }
            }
            .joinToString("\n") { "• $it" }
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

    private fun escapeXmlAttribute(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private suspend fun <T> ListenableFuture<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addListener({
            try {
                cont.resume(get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }, { it.run() })
    }
}
