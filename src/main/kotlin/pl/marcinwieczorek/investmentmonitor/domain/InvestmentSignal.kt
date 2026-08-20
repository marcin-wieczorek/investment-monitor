package pl.marcinwieczorek.investmentmonitor.domain

import java.net.URI
import java.time.Instant
import java.util.Locale

/**
 * The kind of official/public signal that a [DiscoverySource] observed.
 *
 * A signal is evidence that *something* is being planned or permitted, not
 * proof that a specific marketable investment exists yet - see
 * docs/DISCOVERY.md for the reasoning and [InvestmentSignal] for the shape
 * of the evidence itself.
 */
enum class SignalType {
    BUILDING_PERMIT,
    ZONING_DECISION,
    WZ_DECISION,
    MPZP_CHANGE,
    PLANNING_APPLICATION,
    LAND_DEVELOPMENT_SIGNAL,
    ENVIRONMENTAL_DECISION,
    MUNICIPAL_INVESTMENT_SIGNAL,
    DEVELOPER_LAND_ACQUISITION_SIGNAL,
    OTHER
}

/**
 * A single piece of evidence observed from an official/public
 * [SourceCategory.DISCOVERY] source - e.g. a municipal zoning-conditions
 * decision ("warunki zabudowy") referencing a residential development.
 *
 * This is deliberately *not* an [Investment]: a signal does not claim to
 * know a marketable project name, price or unit count. Its identity
 * ([canonicalKey]) follows the same `source:normalized-url` scheme as
 * [Investment] so it can be diffed deterministically by the same kind of
 * change detection, independent of any LLM interpretation.
 */
data class InvestmentSignal(
    val source: String,
    val municipality: String,
    val location: String?,
    val signalType: SignalType,
    val title: String,
    val reference: String?,
    val detectedAt: Instant,
    val url: URI,
    val rawFacts: Map<String, String> = emptyMap()
) {
    val canonicalKey: String
        get() {
            val normalizedUrl = url.normalize()
                .toString()
                .removeSuffix("/")
                .lowercase(Locale.ROOT)
            return "$source:$normalizedUrl"
        }
}
