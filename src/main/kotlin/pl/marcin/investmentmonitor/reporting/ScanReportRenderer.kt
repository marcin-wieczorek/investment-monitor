package pl.marcin.investmentmonitor.reporting

import pl.marcin.investmentmonitor.detection.ChangeType

object ScanReportRenderer {

    fun render(report: ScanReport): String = buildString {
        appendLine("SCAN REPORT")
        appendLine(SEPARATOR)

        report.sources.forEach { source -> appendSource(source) }

        appendLine(SEPARATOR)
        appendLine("New investments: ${report.newInvestmentCount}")
    }

    private fun StringBuilder.appendSource(source: SourceReport) {
        val status = if (source.fetchSucceeded && source.validation.valid) "OK" else "FAIL"
        val presentCount = source.changes.count { it.change.type != ChangeType.REMOVED }
        appendLine("${source.sourceId.padEnd(12)} $status  $presentCount investments")

        source.changes
            .filter { it.change.type != ChangeType.UNCHANGED }
            .forEach { appendChange(it) }

        if (!source.validation.valid) {
            appendLine("  validation failed: ${source.validation.reason}")
        }
    }

    private fun StringBuilder.appendChange(analyzed: AnalyzedChange) {
        val change = analyzed.change
        // current is null only for REMOVED; fall back to previous so we can
        // still report the name/location of what disappeared.
        val investment = change.current ?: change.previous ?: return
        val location = investment.location ?: "unknown location"
        appendLine("  [${change.type}] ${investment.name} ($location)")

        val analysis = analyzed.analysis ?: return
        appendLine("    priority: ${analysis.priority} - ${analysis.reason}")
    }

    private const val SEPARATOR = "----------------------------------------"
}
