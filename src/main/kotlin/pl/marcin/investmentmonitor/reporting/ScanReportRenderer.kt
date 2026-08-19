package pl.marcin.investmentmonitor.reporting

import pl.marcin.investmentmonitor.detection.ChangeType
import pl.marcin.investmentmonitor.domain.Investment

/**
 * Renders a deterministic plain-text daily report (see
 * docs/ARCHITECTURE.md daily report section). The "nothing new" case is
 * never an empty/absent output - it always prints the same structure with
 * a final `STATUS: NO NEW INVESTMENTS` line.
 */
object ScanReportRenderer {

    /** Plot areas at or above this size are flagged as unusually large, independent of any analyzer. */
    private const val LARGE_PLOT_THRESHOLD_M2 = 500.0

    fun render(report: ScanReport): String = buildString {
        appendLine("SCAN REPORT")
        appendLine(SEPARATOR)
        appendLine("Sources checked: ${report.sourcesChecked}")
        appendLine("Sources failed: ${report.sourcesFailed}")
        appendLine()

        appendDeveloperSources(report)
        appendNewInvestments(report)
        appendChangedInvestments(report)
        appendNewDiscoverySignals(report)
        appendCorrelatedSignals(report)
        appendAggregatorOnlyDiscoveries(report)
        appendFailures(report)

        appendLine(SEPARATOR)
        appendLine("New investments: ${report.newInvestmentCount}")
        appendLine("Changed investments: ${report.changedInvestmentCount}")
        appendLine("New discovery signals: ${report.newDiscoverySignalCount}")
        appendLine()

        val nothingNew = report.newInvestmentCount == 0 &&
            report.changedInvestmentCount == 0 &&
            report.newDiscoverySignalCount == 0
        appendLine(if (nothingNew) "STATUS: NO NEW INVESTMENTS" else "STATUS: NEW ACTIVITY DETECTED")
    }

    private fun StringBuilder.appendDeveloperSources(report: ScanReport) {
        (report.developerReports + report.aggregatorReports).forEach { source ->
            val status = if (source.fetchSucceeded && source.validation.valid) "OK" else "FAIL"
            val presentCount = source.changes.count { it.change.type != ChangeType.REMOVED }
            appendLine("${source.sourceId.padEnd(16)} $status  $presentCount investments")
        }
        if (report.developerReports.isNotEmpty() || report.aggregatorReports.isNotEmpty()) appendLine()
    }

    private fun StringBuilder.appendNewInvestments(report: ScanReport) {
        val newChanges = report.developerReports.flatMap { it.changes }.filter { it.change.type == ChangeType.NEW }
        appendLine("NEW INVESTMENTS")
        appendLine(SEPARATOR)
        if (newChanges.isEmpty()) {
            appendLine("(none)")
        } else {
            newChanges.forEach { appendInvestmentChange(it) }
        }
        appendLine()
    }

    private fun StringBuilder.appendChangedInvestments(report: ScanReport) {
        val changed = report.developerReports.flatMap { it.changes }.filter { it.change.type == ChangeType.CHANGED }
        appendLine("CHANGED INVESTMENTS")
        appendLine(SEPARATOR)
        if (changed.isEmpty()) {
            appendLine("(none)")
        } else {
            changed.forEach { appendInvestmentChange(it) }
        }
        appendLine()
    }

    private fun StringBuilder.appendInvestmentChange(analyzed: AnalyzedChange) {
        val investment = analyzed.change.current ?: analyzed.change.previous ?: return
        val location = investment.location ?: "unknown location"
        val largePlotMarker = if (isLargePlot(investment)) " \u2605 LARGE PLOT" else ""
        appendLine("  [${analyzed.change.type}] ${investment.name} ($location)$largePlotMarker")

        val analysis = analyzed.analysis ?: return
        appendLine("    priority: ${analysis.priority} - ${analysis.reason}")
    }

    private fun isLargePlot(investment: Investment): Boolean {
        val plot = investment.plotArea ?: return false
        val value = plot.max ?: plot.min ?: return false
        return value >= LARGE_PLOT_THRESHOLD_M2
    }

    private fun StringBuilder.appendNewDiscoverySignals(report: ScanReport) {
        appendLine("NEW DISCOVERY SIGNALS")
        appendLine(SEPARATOR)
        val newSignals = report.discoveryReports.flatMap { it.newSignals }
        if (newSignals.isEmpty()) {
            appendLine("(none)")
        } else {
            newSignals.forEach { signal ->
                val location = signal.location ?: signal.municipality
                appendLine("  [${signal.signalType}] ${signal.title} ($location)")
                if (signal.reference != null) appendLine("    reference: ${signal.reference}")
            }
        }
        appendLine()
    }

    private fun StringBuilder.appendCorrelatedSignals(report: ScanReport) {
        appendLine("CORRELATED SIGNALS")
        appendLine(SEPARATOR)
        if (report.correlations.isEmpty()) {
            appendLine("(none)")
        } else {
            report.correlations.forEach { candidate ->
                appendLine(
                    "  ${candidate.signal.title} <-> ${candidate.investment.name} " +
                        "(${candidate.confidence}: ${candidate.reason})"
                )
            }
        }
        appendLine()
    }

    private fun StringBuilder.appendAggregatorOnlyDiscoveries(report: ScanReport) {
        appendLine("AGGREGATOR-ONLY DISCOVERIES")
        appendLine(SEPARATOR)
        if (report.aggregatorOnlyDiscoveries.isEmpty()) {
            appendLine("(none)")
        } else {
            report.aggregatorOnlyDiscoveries.forEach { investment ->
                appendLine("  ${investment.name} (${investment.location ?: "unknown location"}) - source: ${investment.source}")
            }
        }
        appendLine()
    }

    private fun StringBuilder.appendFailures(report: ScanReport) {
        val failedDeveloper = report.developerReports.filter { !it.fetchSucceeded || !it.validation.valid }
        val failedAggregator = report.aggregatorReports.filter { !it.fetchSucceeded || !it.validation.valid }
        val failedDiscovery = report.discoveryReports.filter { !it.fetchSucceeded }

        appendLine("PARSER/SOURCE FAILURES")
        appendLine(SEPARATOR)
        if (failedDeveloper.isEmpty() && failedAggregator.isEmpty() && failedDiscovery.isEmpty()) {
            appendLine("(none)")
        } else {
            failedDeveloper.forEach { appendLine("  ${it.sourceId}: ${it.validation.reason ?: "fetch failed"}") }
            failedAggregator.forEach { appendLine("  ${it.sourceId}: ${it.validation.reason ?: "fetch failed"}") }
            failedDiscovery.forEach { appendLine("  ${it.sourceId}: fetch failed") }
        }
        appendLine()
    }

    private const val SEPARATOR = "----------------------------------------"
}
