package pl.marcin.investmentmonitor.monitoring

import org.springframework.stereotype.Service
import pl.marcin.investmentmonitor.analysis.InvestmentAnalyzer
import pl.marcin.investmentmonitor.detection.ChangeDetector
import pl.marcin.investmentmonitor.detection.ChangeType
import pl.marcin.investmentmonitor.detection.InvestmentChange
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.persistence.InvestmentRepository
import pl.marcin.investmentmonitor.persistence.MonitoringRunRepository
import pl.marcin.investmentmonitor.persistence.SourceSnapshot
import pl.marcin.investmentmonitor.persistence.SourceSnapshotRepository
import pl.marcin.investmentmonitor.reporting.AnalyzedChange
import pl.marcin.investmentmonitor.reporting.ScanReport
import pl.marcin.investmentmonitor.reporting.SourceReport
import pl.marcin.investmentmonitor.source.InvestmentDetailEnricher
import pl.marcin.investmentmonitor.source.InvestmentSource
import pl.marcin.investmentmonitor.validation.SourceValidator
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant

/**
 * Orchestrates a full one-shot monitoring scan:
 * fetch -> validate -> detect changes -> enrich + analyze new investments -> commit trusted state (if valid).
 *
 * A source whose result fails validation never overwrites the last trusted
 * snapshot (see docs/ADR-003-fail-closed-source-validation.md).
 *
 * Detail-page enrichment and LLM analysis only run for newly detected
 * investments: both are a one-time cost per investment, not a per-scan
 * cost, keeping routine scans lightweight (see docs/SOURCES.md two-stage
 * scraping note).
 */
@Service
class MonitoringService(
    private val sources: List<InvestmentSource>,
    private val sourceValidator: SourceValidator,
    private val changeDetector: ChangeDetector,
    private val detailEnricher: InvestmentDetailEnricher,
    private val investmentAnalyzer: InvestmentAnalyzer,
    private val investmentRepository: InvestmentRepository,
    private val sourceSnapshotRepository: SourceSnapshotRepository,
    private val monitoringRunRepository: MonitoringRunRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun scan(): ScanReport {
        val startedAt = Instant.now(clock)
        val runId = monitoringRunRepository.start(startedAt)

        val sourceReports = sources.map(::scanSource)
        val sourcesFailed = sourceReports.count { !it.fetchSucceeded || !it.validation.valid }
        val newInvestments = sourceReports.sumOf { report ->
            report.changes.count { it.change.type == ChangeType.NEW }
        }

        val finishedAt = Instant.now(clock)
        val status = if (sourcesFailed == 0) "SUCCESS" else "PARTIAL_FAILURE"
        monitoringRunRepository.finish(runId, finishedAt, status, sources.size, sourcesFailed, newInvestments)

        return ScanReport(startedAt, finishedAt, sourceReports)
    }

    private fun scanSource(source: InvestmentSource): SourceReport {
        val previousInvestments = investmentRepository.findAllBySource(source.id)
        val previousSnapshot = sourceSnapshotRepository.find(source.id)

        val fetchResult = runCatching { source.fetch() }
        val fetched = fetchResult.getOrDefault(emptyList())

        val validation = sourceValidator.validate(fetched, previousSnapshot?.investmentCount)
        val changes = changeDetector.detect(fetched, previousInvestments).map(::processIfNew)

        if (fetchResult.isSuccess && validation.valid) {
            commit(source.id, changes.map { it.change.current })
        }

        return SourceReport(source.id, fetchResult.isSuccess, validation, changes)
    }

    private fun processIfNew(change: InvestmentChange): AnalyzedChange {
        if (change.type != ChangeType.NEW) return AnalyzedChange(change, analysis = null)

        val enriched = detailEnricher.enrich(change.current)
        val analysis = investmentAnalyzer.analyze(enriched)
        return AnalyzedChange(change.copy(current = enriched), analysis)
    }

    private fun commit(sourceId: String, investments: List<Investment>) {
        val seenAt = Instant.now(clock)
        investments.forEach { investmentRepository.upsert(it, seenAt) }
        sourceSnapshotRepository.save(
            SourceSnapshot(
                source = sourceId,
                capturedAt = seenAt,
                investmentCount = investments.size,
                contentHash = contentHash(investments)
            )
        )
    }

    private fun contentHash(investments: List<Investment>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        investments.sortedBy { it.canonicalKey }.forEach { investment ->
            digest.update(investment.canonicalKey.toByteArray())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
