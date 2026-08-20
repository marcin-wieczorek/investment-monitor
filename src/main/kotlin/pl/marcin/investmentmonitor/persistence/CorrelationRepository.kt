package pl.marcin.investmentmonitor.persistence

import pl.marcin.investmentmonitor.domain.Correlation

/**
 * How much earlier a discovery signal was detected than the investment it
 * was correlated with - the core "early detection" KPI (see
 * AGENTS.md section 28). Null when either side's first-seen date is
 * somehow unavailable, never fabricated.
 */
data class CorrelationLeadTime(
    val investmentName: String,
    val signalTitle: String,
    val leadTimeDays: Long?
)

interface CorrelationRepository {
    fun save(correlation: Correlation)
    fun findByInvestment(investmentId: Long): List<Correlation>
    fun exists(investmentId: Long, signalId: Long): Boolean

    /** Positive [CorrelationLeadTime.leadTimeDays] means the signal was detected before the investment. */
    fun findAllWithLeadTime(): List<CorrelationLeadTime>
}
