package pl.marcin.investmentmonitor.domain

import java.net.URI
import java.time.Instant

/** Review lifecycle for a developer discovered outside [pl.marcin.investmentmonitor.registry.DeveloperRegistry]. */
enum class DeveloperCandidateStatus {
    /** Just discovered, not yet looked at. */
    NEW,

    /** Looked at once, needs a human decision. */
    REVIEW_REQUIRED,

    /** Confirmed as a real, relevant developer - ready to become a registry entry. */
    ACCEPTED,

    /** Not a real developer, or out of scope. */
    REJECTED,

    /** Promoted to [pl.marcin.investmentmonitor.registry.DeveloperRegistry] with a working adapter. */
    IMPLEMENTED,

    /** Verified unreachable/unscrapable. */
    BLOCKED
}

/**
 * A developer discovered indirectly - e.g. named in an aggregator listing
 * whose developer is not yet in [pl.marcin.investmentmonitor.registry.DeveloperRegistry].
 *
 * This is the feedback loop described in AGENTS.md section 6/33: the system
 * never silently trusts or auto-scrapes a discovered URL, it only records
 * the candidate for controlled human review.
 */
data class DeveloperCandidate(
    val id: Long? = null,
    val developerName: String,
    val discoveredUrl: URI,
    val municipality: String?,
    val discoveredFromSource: String,
    val discoveredAt: Instant,
    val status: DeveloperCandidateStatus = DeveloperCandidateStatus.NEW,
    val evidence: String? = null
) {
    init {
        require(developerName.isNotBlank()) { "developerName must not be blank" }
        require(discoveredFromSource.isNotBlank()) { "discoveredFromSource must not be blank" }
    }
}
