package pl.marcin.investmentmonitor.persistence

import pl.marcin.investmentmonitor.domain.Investment
import java.time.Instant

interface InvestmentRepository {
    fun findAllBySource(source: String): Map<String, Investment>
    fun findAll(): List<Investment>
    fun upsert(investment: Investment, seenAt: Instant)

    /** Looks up the database-assigned id for an investment, e.g. to store a [pl.marcin.investmentmonitor.domain.Correlation]. */
    fun findIdByCanonicalKey(canonicalKey: String): Long?
}
