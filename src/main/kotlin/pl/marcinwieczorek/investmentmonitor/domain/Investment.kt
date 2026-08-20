package pl.marcinwieczorek.investmentmonitor.domain

import java.net.URI
import java.util.Locale

data class Investment(
    val source: String,
    val developer: String,
    val name: String,
    val url: URI,
    val location: String?,
    val propertyType: PropertyType?,
    val units: Int?,
    val houseArea: AreaRange?,
    val plotArea: AreaRange?,
    val price: PriceRange?,
    val status: InvestmentStatus?,
    val imageUrl: String?
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

data class AreaRange(val min: Double?, val max: Double?)
data class PriceRange(val min: Int?, val max: Int?)

enum class PropertyType { TERRACED, SEMI_DETACHED, DETACHED, APARTMENT, UNKNOWN }

/**
 * Sale/construction status as published by a source. Beyond the original
 * three (before/during/after sale) plus [UNKNOWN], several developer sites
 * publish more specific readiness states verified against real HTML (see
 * AgrobexParser, DeveliaParser, LineaParser, JakonInwestParser) - modeled
 * explicitly here rather than force-fit into [FOR_SALE], since e.g. "gotowe
 * do odbioru" (ready for handover) and "ostatnie wolne mieszkania" (last
 * units) carry meaningfully different information than a plain "for sale"
 * label. Not consumed by [pl.marcinwieczorek.investmentmonitor.analysis.DeterministicScorer]
 * (display-only), so widening this enum is safe.
 */
enum class InvestmentStatus {
    PRE_SALE,
    FOR_SALE,
    /** "Ostatnie wolne mieszkania/lokale" - urgency signal, still for sale. */
    LAST_UNITS,
    /** "Gotowe do odbioru" - construction finished, units ready for handover. */
    READY_FOR_HANDOVER,
    /** "W trakcie realizacji" - actively under construction. */
    UNDER_CONSTRUCTION,
    SOLD_OUT,
    UNKNOWN
}
