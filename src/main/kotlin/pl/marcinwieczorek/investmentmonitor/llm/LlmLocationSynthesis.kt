package pl.marcinwieczorek.investmentmonitor.llm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Structured JSON shape expected back from the local LLM for a single
 * location's synthesis (see [LocationSynthesisPromptBuilder] and
 * docs/LLM.md). Every field is nullable/defaulted so a partial or
 * malformed response degrades gracefully rather than failing the whole
 * synthesis.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LlmLocationSynthesis(
    val developmentTrend: String? = null,
    val summary: String? = null,
    val estimatedNewInvestmentsTimeline: String? = null,
    val keyDevelopers: List<String> = emptyList(),
    val opportunities: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val recommendedAction: String? = null,
    val reason: String? = null
)
