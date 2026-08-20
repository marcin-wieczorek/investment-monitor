package pl.marcinwieczorek.investmentmonitor.llm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LlmHotspotEntry(
    val location: String? = null,
    val activityLevel: String? = null,
    val trend: String? = null,
    val reason: String? = null,
    val relevanceToProfile: String? = null
)

/**
 * Structured JSON shape expected back from the local LLM for the
 * region-wide hotspot comparison (see [HotspotSynthesisPromptBuilder] and
 * docs/LLM.md). Every field is nullable/defaulted so a partial or
 * malformed response degrades gracefully rather than failing the whole
 * synthesis.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LlmHotspotSynthesis(
    val hotspots: List<LlmHotspotEntry> = emptyList(),
    val emergingAreas: List<String> = emptyList(),
    val summary: String? = null,
    val recommendation: String? = null
)
