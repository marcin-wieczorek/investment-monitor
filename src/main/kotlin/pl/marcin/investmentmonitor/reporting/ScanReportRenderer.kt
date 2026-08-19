package pl.marcin.investmentmonitor.reporting

import pl.marcin.investmentmonitor.detection.ChangeType

object ScanReportRenderer {

    fun render(report: ScanReport): String = buildString {
        appendLine("DAILY SCAN")
        appendLine(SEPARATOR)

        report.sources.forEach { source -> appendSource(source) }

        appendLine(SEPARATOR)
        appendLine("New investments: ${report.newInvestmentCount}")
    }

    private fun StringBuilder.appendSource(source: SourceReport) {
        val status = if (source.fetchSucceeded && source.validation.valid) "OK" else "FAIL"
        appendLine("${source.sourceId.padEnd(12)} $status  ${source.changes.size} investments")

        source.changes
            .filter { it.change.type != ChangeType.UNCHANGED }
            .forEach { appendChange(it) }

        if (!source.validation.valid) {
            appendLine("  validation failed: ${source.validation.reason}")
        }
    }

    private fun StringBuilder.appendChange(analyzed: AnalyzedChange) {
        val current = analyzed.change.current
        val location = current.location ?: "unknown location"
        appendLine("  [${analyzed.change.type}] ${current.name} ($location)")

        val analysis = analyzed.analysis ?: return
        appendLine("    priority: ${analysis.priority} - ${analysis.reason}")
    }

    private const val SEPARATOR = "----------------------------------------"
}
