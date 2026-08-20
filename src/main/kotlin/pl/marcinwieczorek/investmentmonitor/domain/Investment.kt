package pl.marcinwieczorek.investmentmonitor.domain

import java.net.URI

data class Investment(
    val source: SourceId,
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
    val imageUrl: URI?
) {
    init {
        require(name.isNotBlank()) { "Investment name must not be blank" }
        require(url.toString().isNotBlank()) { "Investment url must not be blank" }
    }

    val canonicalKey: String
        get() = canonicalKeyOf(source, url)
}

data class AreaRange(val min: Double?, val max: Double?) {
    init {
        require(min != null || max != null) { "AreaRange must have at least one non-null bound" }
    }
}

data class PriceRange(val min: Int?, val max: Int?) {
    init {
        require(min != null || max != null) { "PriceRange must have at least one non-null bound" }
    }
}

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
