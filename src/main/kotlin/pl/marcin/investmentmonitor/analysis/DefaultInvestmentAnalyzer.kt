package pl.marcin.investmentmonitor.analysis

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.LocationProfile

/**
 * Default analyzer used when no local LLM is configured
 * (`investment-monitor.llm.enabled=false`, the default).
 *
 * Unlike a no-op placeholder, this always computes a fully deterministic
 * [InvestmentAnalysis] via [DeterministicScorer] against
 * [ReferenceProfiles.DEFAULT] - "no LLM configured" must never mean "no
 * scoring happens" (see docs/ARCHITECTURE.md deterministic scoring
 * section). [pl.marcin.investmentmonitor.llm.OllamaInvestmentAnalyzer]
 * uses the exact same deterministic score (via [DeterministicAnalysisSupport])
 * and only adds qualitative priority/reason on top when available.
 */
@Component
@ConditionalOnProperty(prefix = "investment-monitor.llm", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class DefaultInvestmentAnalyzer(private val scorer: DeterministicScorer) : InvestmentAnalyzer {

    override fun analyze(investment: Investment, locationProfile: LocationProfile?): InvestmentAnalysis {
        val referenceProfile = ReferenceProfiles.DEFAULT
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
