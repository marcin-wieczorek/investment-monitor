package pl.marcin.investmentmonitor.persistence

import pl.marcin.investmentmonitor.domain.InvestmentDuplicate

interface InvestmentDuplicateRepository {
    /** No-op if this pair (in either order) is already persisted. */
    fun save(duplicate: InvestmentDuplicate)
    fun findByInvestment(investmentId: Long): List<InvestmentDuplicate>
    fun exists(investmentIdA: Long, investmentIdB: Long): Boolean
}
