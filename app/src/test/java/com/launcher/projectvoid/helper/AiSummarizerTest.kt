package com.launcher.projectvoid.helper

import com.google.mlkit.genai.common.FeatureStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiSummarizerTest {

    @Test
    fun mapPromptStatusToTier_availableStatuses_selectTierOne() {
        // These statuses all represent a usable Prompt API path, so we should keep Tier 1 selected.
        assertEquals(1, AiSummarizer.mapPromptStatusToTier(FeatureStatus.AVAILABLE))
        assertEquals(1, AiSummarizer.mapPromptStatusToTier(FeatureStatus.DOWNLOADABLE))
        assertEquals(1, AiSummarizer.mapPromptStatusToTier(FeatureStatus.DOWNLOADING))
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
