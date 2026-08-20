package pl.marcinwieczorek.investmentmonitor.analysis

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository

/**
 * Default analyzer used when no local LLM is configured
 * (`investment-monitor.llm.enabled=false`, the default).
 *
 * Unlike a no-op placeholder, this always computes a fully deterministic
 * [InvestmentAnalysis] via [DeterministicScorer] against the
 * user-configurable reference profile (see [UserPreferencesRepository],
 * falls back to [ReferenceProfiles.DEFAULT] when nothing has been
 * configured yet) - "no LLM configured" must never mean "no scoring
 * happens" (see docs/ARCHITECTURE.md deterministic scoring section).
 * [pl.marcinwieczorek.investmentmonitor.llm.OllamaInvestmentAnalyzer] uses the
 * exact same deterministic score (via [DeterministicAnalysisSupport]) and
 * only adds qualitative priority/reason on top when available.
 */
@Component
@ConditionalOnProperty(prefix = "investment-monitor.llm", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class DefaultInvestmentAnalyzer(
    private val scorer: DeterministicScorer,
    private val userPreferencesRepository: UserPreferencesRepository
) : InvestmentAnalyzer {

    override fun analyze(investment: Investment, locationProfile: LocationProfile?): InvestmentAnalysis {
        val referenceProfile = userPreferencesRepository.effectiveScoringProfile()
        val scoring = scorer.score(investment, locationProfile, referenceProfile)

        return InvestmentAnalysis(
            investmentScore = scoring.overallScore,
            locationScore = DeterministicAnalysisSupport.locationScore(locationProfile),
            referenceProfileScore = scoring.overallScore,
            priority = DeterministicAnalysisSupport.priorityFrom(scoring),
            reason = DeterministicAnalysisSupport.describeScore(scoring)
        )
    }
}
