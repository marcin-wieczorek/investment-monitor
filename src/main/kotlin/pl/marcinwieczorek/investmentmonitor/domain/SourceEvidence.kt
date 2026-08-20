package pl.marcinwieczorek.investmentmonitor.domain

import java.net.URI
import java.time.Instant

/** How a fact's value was obtained. Never [LLM] for facts a parser can already extract. */
enum class ExtractionMethod { PARSER, LLM, MANUAL }

/**
 * Which entity a piece of [SourceEvidence] belongs to. A sum type replacing the
 * previous nullable-XOR `investmentId`/`signalId` pair - the compiler now
 * enforces "exactly one of investment or signal" instead of a runtime `require`.
 */
sealed interface EvidenceOwner {
    data class ForInvestment(val investmentId: Long) : EvidenceOwner
    data class ForSignal(val signalId: Long) : EvidenceOwner
}

/**
 * Provenance record for a single fact: which source produced it, when it
 * was captured, and how it was extracted.
 *
 * Every important discovered fact should be traceable back to one of
 * these (see docs/ARCHITECTURE.md provenance section). [owner] identifies
 * which entity (investment or signal) the fact belongs to.
 */
data class SourceEvidence(
    val id: Long? = null,
    val owner: EvidenceOwner,
    val sourceId: String,
    val sourceCategory: SourceCategory,
    val capturedAt: Instant,
    val url: URI,
    val extractionMethod: ExtractionMethod,
    val fieldName: String,
    val fieldValue: String
)
