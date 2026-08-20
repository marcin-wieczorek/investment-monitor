package pl.marcinwieczorek.investmentmonitor.persistence

import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import java.time.Instant

interface InvestmentRepository {
    fun findAllBySource(source: SourceId): Map<String, Investment>
    fun findAll(): List<Investment>
    fun upsert(investment: Investment, seenAt: Instant)

    /** Looks up the database-assigned id for an investment, e.g. to store a [pl.marcinwieczorek.investmentmonitor.domain.Correlation]. */
    fun findIdByCanonicalKey(canonicalKey: String): Long?

    /**
     * Persists whether an aggregator investment currently has no matching
     * developer source covering its location - recomputed for every
     * aggregator investment on every scan (see
     * `MonitoringService.updateAggregatorOnlyDiscoveryFlags`), so the
     * frontend can filter on it directly instead of re-deriving the same
     * location-coverage logic.
     */
    fun updateAggregatorOnlyDiscoveryFlag(canonicalKey: String, isAggregatorOnly: Boolean)
}
