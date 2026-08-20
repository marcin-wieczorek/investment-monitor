package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.Investment

/**
 * A source that lists investments from a third-party aggregator portal
 * (e.g. RynekPierwotny) rather than a developer's own site.
 *
 * Aggregators are a completeness/cross-check layer, never the primary
 * discovery mechanism or the source of truth for an investment's identity
 * when a first-party [InvestmentSource] already exists for it (see
 * docs/ARCHITECTURE.md source precedence section).
 */
interface AggregatorSource {
    val id: String
    fun fetch(): List<Investment>
}
