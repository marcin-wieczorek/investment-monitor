package pl.marcinwieczorek.investmentmonitor.llm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Structured JSON shape expected back from the local LLM (see
 * docs/LLM.md). Every field is nullable/defaulted: a partial or malformed
 * response degrades gracefully rather than failing analysis outright.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LlmInvestmentInterpretation(
    val attractiveness: String? = null,
    val strongestPositives: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val locationPromising: Boolean? = null,
    val plotUnusuallyAttractive: Boolean? = null,
    val worthManualReview: Boolean? = null,
    val missingInformation: List<String> = emptyList(),
    val reason: String? = null
)
