package pl.marcinwieczorek.investmentmonitor.domain

import java.net.URI

/** Priority tier used to sequence adapter implementation work - not a measure of investment quality. */
enum class DeveloperTier { A, B, DISCOVERED }

/**
 * Lifecycle status of a developer within [pl.marcinwieczorek.investmentmonitor.registry.DeveloperRegistry].
 *
 * A developer never disappears from the registry just because it currently
 * has no active investment - see AGENTS.md developer coverage section.
 */
enum class DeveloperStatus {
    /** Has a working [pl.marcinwieczorek.investmentmonitor.source.InvestmentSource] adapter. */
    MONITORED,

    /** Found via aggregator-only discovery, not yet reviewed. */
    DISCOVERED,

    /** Known priority developer, real URL verified, adapter not implemented yet. */
    CANDIDATE,

    /** Known developer, verified URL, but no investments currently for sale. */
    NO_CURRENT_INVESTMENTS,

    /** Was monitored previously, now dormant (site gone, company inactive, ...). */
    INACTIVE,

    /** Verified to be technically unreachable/unscrapable (JS SPA, anti-bot, ...). */
    BLOCKED
}

/**
 * A residential developer as a first-class domain concept, independent of
 * whether an [pl.marcinwieczorek.investmentmonitor.source.InvestmentSource] adapter
 * currently exists for it.
 *
 * Do not invent [website]/[investmentListUrls] values - leave them `null`
 * until a real URL has been manually verified (see AGENTS.md "no fake
 * implementations").
 */
data class Developer(
    val id: String,
    val name: String,
    val website: URI?,
    val investmentListUrls: List<URI>,
    val tier: DeveloperTier,
    val status: DeveloperStatus,
    val geographicScope: Set<String>,
    val adapterSourceId: String?,
    /**
     * True when this developer is (or was) [DeveloperStatus.BLOCKED] due to
     * client-side rendering (JS SPA/React/AJAX-hydrated listings) rather
     * than a WAF or a nonexistent site (see ADR-007) - i.e. a headless
     * browser fetcher could plausibly unblock it. Purely descriptive;
     * doesn't imply an adapter exists.
     */
    val requiresBrowser: Boolean = false
) {
    init {
        require(id.isNotBlank()) { "Developer id must not be blank" }
        require(name.isNotBlank()) { "Developer name must not be blank" }
        if (status == DeveloperStatus.MONITORED) {
            requireNotNull(adapterSourceId) { "MONITORED developer '$id' must reference an adapterSourceId" }
        }
    }
}
