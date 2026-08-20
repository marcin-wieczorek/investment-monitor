package pl.marcinwieczorek.investmentmonitor.reporting

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.detection.ChangeType
import pl.marcinwieczorek.investmentmonitor.detection.InvestmentChange
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationLeadTime
import pl.marcinwieczorek.investmentmonitor.testsupport.testInvestment
import pl.marcinwieczorek.investmentmonitor.validation.ValidationResult
import java.time.Instant

class ScanReportRendererTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun emptyReport(): ScanReport = ScanReport(
        startedAt = now,
        finishedAt = now,
        developerReports = listOf(SourceReport("chronos", true, ValidationResult(true), emptyList())),
        aggregatorReports = emptyList(),
        discoveryReports = listOf(
            pl.marcinwieczorek.investmentmonitor.reporting.DiscoverySourceReport("swarzedz-wz", "Swarzędz", true, 10, emptyList())
        ),
        correlations = emptyList(),
        aggregatorOnlyDiscoveries = emptyList()
    )

    @Test
    fun `produces a deterministic report even with nothing new`() {
        val output = ScanReportRenderer.render(emptyReport())

        output shouldContain "SCAN REPORT"
        output shouldContain "STATUS: NO NEW INVESTMENTS"
        output shouldContain "New investments: 0"
        output shouldContain "New discovery signals: 0"
    }

    @Test
    fun `never produces an empty string`() {
        ScanReportRenderer.render(emptyReport()).isBlank() shouldBe false
    }

    @Test
    fun `flags a large plot explicitly`() {
        val investment = testInvestment(name = "BigPlot", plotArea = AreaRange(800.0, 800.0))
        val change = InvestmentChange(ChangeType.NEW, investment, previous = null)
        val report = emptyReport().copy(
            developerReports = listOf(SourceReport("chronos", true, ValidationResult(true), listOf(AnalyzedChange(change, null))))
        )

        val output = ScanReportRenderer.render(report)
        output shouldContain "LARGE PLOT"
        output shouldContain "STATUS: NEW ACTIVITY DETECTED"
    }

    @Test
    fun `does not flag a modestly sized plot`() {
        val investment = testInvestment(name = "SmallPlot", plotArea = AreaRange(200.0, 200.0))
        val change = InvestmentChange(ChangeType.NEW, investment, previous = null)
        val report = emptyReport().copy(
            developerReports = listOf(SourceReport("chronos", true, ValidationResult(true), listOf(AnalyzedChange(change, null))))
        )

        ScanReportRenderer.render(report) shouldContain "SmallPlot"
        (ScanReportRenderer.render(report).contains("LARGE PLOT")) shouldBe false
    }

    @Test
    fun `renders a discovery lead time section with the number of days`() {
        val report = emptyReport().copy(
            leadTimes = listOf(CorrelationLeadTime(investmentName = "Osiedle X", signalTitle = "WZ decision", leadTimeDays = 14))
        )

        val output = ScanReportRenderer.render(report)
        output shouldContain "DISCOVERY LEAD TIME"
        output shouldContain "14 day(s) before developer publication"
    }

    @Test
    fun `renders none for the lead time section when there are no correlations`() {
        ScanReportRenderer.render(emptyReport()) shouldContain "DISCOVERY LEAD TIME"
    }
}
