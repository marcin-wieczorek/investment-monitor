package pl.marcin.investmentmonitor.analysis

import pl.marcin.investmentmonitor.domain.LocationProfile

/**
 * Shared conversions from a [ScoringResult] to the fields [InvestmentAnalyzer]
 * implementations report, so "no LLM configured" ([DefaultInvestmentAnalyzer])
 * and "LLM's deterministic fallback" ([pl.marcin.investmentmonitor.llm.OllamaInvestmentAnalyzer])
 * always describe an identical deterministic score identically.
 */
object DeterministicAnalysisSupport {

    fun locationScore(locationProfile: LocationProfile?): Double? = locationProfile?.let {
        (it.growthScore + it.infrastructureScore + it.transportScore + it.familyScore) / 40.0
    }

    fun priorityFrom(scoring: ScoringResult): Priority = when {
        scoring.overallScore >= 0.66 -> Priority.HIGH
        scoring.overallScore >= 0.4 -> Priority.MEDIUM
        else -> Priority.LOW
    }

    fun describeScore(scoring: ScoringResult): String = buildString {
        append("Deterministic score ${"%.2f".format(scoring.overallScore)}.")
        if (scoring.largePlotBonus) append(" Plot is unusually large for the reference profile.")
    }
}
