package pl.marcin.investmentmonitor.analysis

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.LocationProfile

/**
 * Placeholder analyzer used when no local LLM is configured
 * (`investment-monitor.llm.enabled=false`, the default). Reports
 * explicitly that analysis was skipped rather than fabricating a score,
 * so reports/callers can distinguish "not analyzed" from "analyzed as low
 * priority". See [pl.marcin.investmentmonitor.llm.OllamaInvestmentAnalyzer]
 * for the real implementation.
 */
@Component
@ConditionalOnProperty(prefix = "investment-monitor.llm", name = ["enabled"], havingValue = "false", matchIfMissing = true)
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
