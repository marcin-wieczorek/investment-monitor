package pl.marcin.investmentmonitor.reporting

import pl.marcin.investmentmonitor.analysis.InvestmentAnalysis
import pl.marcin.investmentmonitor.detection.ChangeType
import pl.marcin.investmentmonitor.detection.InvestmentChange
import pl.marcin.investmentmonitor.validation.ValidationResult
import java.time.Instant

/**
 * A detected change paired with its (optional) analysis. Analysis is only
 * ever produced for [ChangeType.NEW] investments and may be null when no
 * analyzer ran.
 */
data class AnalyzedChange(
    val change: InvestmentChange,
    val analysis: InvestmentAnalysis?
)

data class SourceReport(
    val sourceId: String,
    val fetchSucceeded: Boolean,
    val validation: ValidationResult,
    val changes: List<AnalyzedChange>
)

data class ScanReport(
    val startedAt: Instant,
    val finishedAt: Instant,
    val sources: List<SourceReport>
) {
    val newInvestmentCount: Int
        get() = sources.sumOf { source -> source.changes.count { it.change.type == ChangeType.NEW } }
}
