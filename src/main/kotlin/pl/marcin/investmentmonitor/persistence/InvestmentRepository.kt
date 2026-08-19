package pl.marcin.investmentmonitor.persistence

import pl.marcin.investmentmonitor.domain.Investment
import java.time.Instant

interface InvestmentRepository {
    fun findAllBySource(source: String): Map<String, Investment>
    fun upsert(investment: Investment, seenAt: Instant)
}
