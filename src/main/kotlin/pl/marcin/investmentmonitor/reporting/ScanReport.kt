package pl.marcin.investmentmonitor.reporting

import pl.marcin.investmentmonitor.analysis.InvestmentAnalysis
import pl.marcin.investmentmonitor.correlation.CorrelationCandidate
import pl.marcin.investmentmonitor.detection.ChangeType
import pl.marcin.investmentmonitor.detection.InvestmentChange
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.InvestmentSignal
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

/** Result of scanning a single [pl.marcin.investmentmonitor.source.DiscoverySource]. */
data class DiscoverySourceReport(
    val sourceId: String,
    val municipality: String,
    val fetchSucceeded: Boolean,
    val totalSignals: Int,
    val newSignals: List<InvestmentSignal>
)

/**
 * The outcome of one complete scan across all three source categories -
 * developer, discovery and aggregator (see docs/ARCHITECTURE.md).
 *
 * The "nothing new" case is a normal, fully-populated report (all lists
 * simply empty) rather than a special/absent value, so
 * [pl.marcin.investmentmonitor.reporting.ScanReportRenderer] can always
 * produce deterministic output (see docs/ARCHITECTURE.md daily report
 * section).
 */
data class ScanReport(
    val startedAt: Instant,
    val finishedAt: Instant,
    val developerReports: List<SourceReport>,
    val aggregatorReports: List<SourceReport>,
    val discoveryReports: List<DiscoverySourceReport>,
    val correlations: List<CorrelationCandidate>,
    /** Aggregator-sourced investments newly seen this run with no matching developer-sourced investment. */
    val aggregatorOnlyDiscoveries: List<Investment>
) {
    val newInvestmentCount: Int
        get() = developerReports.sumOf { source -> source.changes.count { it.change.type == ChangeType.NEW } }

    val changedInvestmentCount: Int
        get() = developerReports.sumOf { source -> source.changes.count { it.change.type == ChangeType.CHANGED } }

    val newDiscoverySignalCount: Int
        get() = discoveryReports.sumOf { it.newSignals.size }

    val sourcesChecked: Int
        get() = developerReports.size + aggregatorReports.size + discoveryReports.size

    val sourcesFailed: Int
        get() = developerReports.count { !it.fetchSucceeded || !it.validation.valid } +
            aggregatorReports.count { !it.fetchSucceeded || !it.validation.valid } +
            discoveryReports.count { !it.fetchSucceeded }
}
