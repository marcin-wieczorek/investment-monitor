package pl.marcinwieczorek.investmentmonitor.domain

/**
 * Typed identifier for a source (`"chronos"`, `"swarzedz-wz"`, `"rynek-pierwotny"`, ...).
 *
 * Wraps the raw string used throughout the codebase as [pl.marcinwieczorek.investmentmonitor.source.InvestmentSource.id]
 * / [pl.marcinwieczorek.investmentmonitor.source.DiscoverySource.id] / [pl.marcinwieczorek.investmentmonitor.source.AggregatorSource.id]
 * so that [Investment.source] / [InvestmentSignal.source] cannot be confused with any other
 * `String` field (developer name, location, ...) at compile time. Zero runtime cost
 * (`@JvmInline value class`).
 */
@JvmInline
value class SourceId(val value: String) {
    init {
        require(value.isNotBlank()) { "SourceId must not be blank" }
    }

    override fun toString(): String = value
}

/**
 * Canonical identity shared by [Investment] and [InvestmentSignal]: `source:normalized-url`,
 * lowercased with the trailing slash stripped. See ADR-002 - never change this scheme.
 */
fun canonicalKeyOf(source: SourceId, url: java.net.URI): String {
    val normalizedUrl = url.normalize()
        .toString()
        .removeSuffix("/")
        .lowercase(java.util.Locale.ROOT)
    return "${source.value}:$normalizedUrl"
}
