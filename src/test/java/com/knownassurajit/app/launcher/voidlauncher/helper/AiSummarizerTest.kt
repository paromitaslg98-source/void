package com.knownassurajit.app.launcher.voidlauncher.helper

import com.google.mlkit.genai.common.FeatureStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiSummarizerTest {

    @Test
    @Test
    fun mapPromptStatusToTier_availableStatus_selectTierOne() {
        // Only AVAILABLE represents a usable Prompt API path.
        assertEquals(1, AiSummarizer.mapPromptStatusToTier(FeatureStatus.AVAILABLE))
    }

    @Test
    fun mapPromptStatusToTier_pendingStatuses_fallsBack() {
        // DOWNLOADABLE and DOWNLOADING are not yet ready for inference.
        assertNull(AiSummarizer.mapPromptStatusToTier(FeatureStatus.DOWNLOADABLE))
        assertNull(AiSummarizer.mapPromptStatusToTier(FeatureStatus.DOWNLOADING))
    }
    @Test
    fun mapPromptStatusToTier_unavailableStatus_fallsBack() {
        // Explicitly unavailable should not claim AI availability.
        assertNull(AiSummarizer.mapPromptStatusToTier(FeatureStatus.UNAVAILABLE))
    }

    @Test
    fun mapPromptStatusToTier_unknownStatus_fallsBackDefensively() {
        // If ML Kit adds new constants later, this defensive behavior prevents accidental Tier 1 selection.
        assertNull(AiSummarizer.mapPromptStatusToTier(Int.MAX_VALUE))
    }
}
