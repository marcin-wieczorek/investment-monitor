package pl.marcin.investmentmonitor.analysis

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.LocationProfile

/**
 * Placeholder analyzer used until a local LLM (e.g. Ollama + Qwen) is wired
 * in. Reports explicitly that analysis was skipped rather than fabricating
 * a score, so reports/callers can distinguish "not analyzed" from
 * "analyzed as low priority".
 */
@Component
class NoOpInvestmentAnalyzer : InvestmentAnalyzer {

    override fun analyze(investment: Investment, locationProfile: LocationProfile?): InvestmentAnalysis =
        InvestmentAnalysis(
            investmentScore = null,
            locationScore = null,
            referenceProfileScore = null,
            priority = Priority.UNKNOWN,
            reason = "LLM analysis not configured."
        )
}
