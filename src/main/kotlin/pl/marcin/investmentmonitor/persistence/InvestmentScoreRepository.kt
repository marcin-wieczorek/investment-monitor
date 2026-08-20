package pl.marcin.investmentmonitor.persistence

import pl.marcin.investmentmonitor.analysis.ScoringResult
import java.time.Instant

/**
 * Persists the [ScoringResult] computed for a newly detected investment.
 *
 * Keyed by canonical key (not investment id), matching the same reasoning
 * as [LlmAnalysisRepository]: scoring happens before a newly-detected
 * investment is committed/assigned a database id (see MonitoringService).
 */
interface InvestmentScoreRepository {
    fun save(investmentCanonicalKey: String, scoring: ScoringResult, scoredAt: Instant)
    fun find(investmentCanonicalKey: String): ScoringResult?
}
