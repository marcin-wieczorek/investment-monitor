package pl.marcin.investmentmonitor.domain

import java.net.URI
import java.time.Instant

/** How a fact's value was obtained. Never [LLM] for facts a parser can already extract. */
enum class ExtractionMethod { PARSER, LLM, MANUAL }

/**
 * Provenance record for a single fact: which source produced it, when it
 * was captured, and how it was extracted.
 *
 * Every important discovered fact should be traceable back to one of
 * these (see docs/ARCHITECTURE.md provenance section). Exactly one of
 * [investmentId]/[signalId] is set, matching which entity the fact
 * belongs to.
 */
data class SourceEvidence(
    val id: Long? = null,
    val investmentId: Long?,
    val signalId: Long?,
    val sourceId: String,
    val sourceCategory: SourceCategory,
    val capturedAt: Instant,
    val url: URI,
    val extractionMethod: ExtractionMethod,
    val fieldName: String,
    val fieldValue: String
) {
    init {
        require((investmentId == null) != (signalId == null)) {
            "Evidence must reference exactly one of investmentId or signalId"
        }
    }
}
