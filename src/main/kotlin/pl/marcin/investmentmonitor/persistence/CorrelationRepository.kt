package pl.marcin.investmentmonitor.persistence

import pl.marcin.investmentmonitor.domain.Correlation

interface CorrelationRepository {
    fun save(correlation: Correlation)
    fun findByInvestment(investmentId: Long): List<Correlation>
    fun exists(investmentId: Long, signalId: Long): Boolean
}
