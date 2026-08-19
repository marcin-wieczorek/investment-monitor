package pl.marcin.investmentmonitor.monitoring

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.marcin.investmentmonitor.analysis.InvestmentAnalyzer
import pl.marcin.investmentmonitor.archival.RawHtmlArchiver
import pl.marcin.investmentmonitor.correlation.CorrelationCandidate
import pl.marcin.investmentmonitor.correlation.InvestmentCorrelator
import pl.marcin.investmentmonitor.detection.ChangeDetector
import pl.marcin.investmentmonitor.detection.ChangeType
import pl.marcin.investmentmonitor.detection.InvestmentChange
import pl.marcin.investmentmonitor.domain.Correlation
import pl.marcin.investmentmonitor.domain.ExtractionMethod
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.domain.LocationCatalog
import pl.marcin.investmentmonitor.domain.SourceCategory
import pl.marcin.investmentmonitor.domain.SourceEvidence
import pl.marcin.investmentmonitor.persistence.CorrelationRepository
import pl.marcin.investmentmonitor.persistence.EvidenceRepository
import pl.marcin.investmentmonitor.persistence.InvestmentRepository
import pl.marcin.investmentmonitor.persistence.MonitoringRunRepository
import pl.marcin.investmentmonitor.persistence.RunStatus
import pl.marcin.investmentmonitor.persistence.SignalRepository
import pl.marcin.investmentmonitor.persistence.SourceSnapshot
import pl.marcin.investmentmonitor.persistence.SourceSnapshotRepository
import pl.marcin.investmentmonitor.reporting.AnalyzedChange
import pl.marcin.investmentmonitor.reporting.DiscoverySourceReport
import pl.marcin.investmentmonitor.reporting.ScanReport
import pl.marcin.investmentmonitor.reporting.SourceReport
import pl.marcin.investmentmonitor.source.AggregatorSource
import pl.marcin.investmentmonitor.source.DiscoverySource
import pl.marcin.investmentmonitor.source.InvestmentDetailEnricher
import pl.marcin.investmentmonitor.source.InvestmentSource
import pl.marcin.investmentmonitor.source.SourceRegistry
import pl.marcin.investmentmonitor.validation.SourceValidator
import pl.marcin.investmentmonitor.validation.ValidationResult
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant

/**
 * Orchestrates a full one-shot monitoring scan across all three source
 * categories (see docs/ARCHITECTURE.md):
 *
 * 1. Developer sources: fetch -> validate -> detect changes -> enrich +
 *    analyze new investments -> commit trusted state (if valid).
 * 2. Aggregator sources: same pipeline, minus enrichment/analysis - they
 *    are a completeness/cross-check layer, not a primary ranking target.
 * 3. Discovery sources: fetch official/public signals, detect which are
 *    new, commit trusted state.
 * 4. Cross-source correlation: deterministically link discovery signals
 *    to investments that likely describe the same project.
 *
 * A source whose result fails validation never overwrites the last
 * trusted snapshot (see docs/ADR-003-fail-closed-source-validation.md).
 */
@Service
class MonitoringService(
    private val sourceRegistry: SourceRegistry,
    private val sourceValidator: SourceValidator,
    private val changeDetector: ChangeDetector,
    private val detailEnricher: InvestmentDetailEnricher,
    private val investmentAnalyzer: InvestmentAnalyzer,
    private val investmentRepository: InvestmentRepository,
    private val sourceSnapshotRepository: SourceSnapshotRepository,
    private val monitoringRunRepository: MonitoringRunRepository,
    private val signalRepository: SignalRepository,
    private val evidenceRepository: EvidenceRepository,
    private val correlationRepository: CorrelationRepository,
    private val correlator: InvestmentCorrelator,
    private val rawHtmlArchiver: RawHtmlArchiver,
    private val clock: Clock = Clock.systemUTC()
) {

    fun scan(): ScanReport {
        val startedAt = Instant.now(clock)
        val runId = monitoringRunRepository.start(startedAt)
        logger.info(
            "Scan started ({} developer, {} discovery, {} aggregator sources)",
            sourceRegistry.developerSources().size,
            sourceRegistry.discoverySources().size,
            sourceRegistry.aggregatorSources().size
        )

        val developerReports = sourceRegistry.developerSources().map(::scanDeveloperSource)
        val aggregatorReports = sourceRegistry.aggregatorSources().map(::scanAggregatorSource)
        val discoveryReports = sourceRegistry.discoverySources().map(::scanDiscoverySource)

        val correlations = runCorrelation()
        val aggregatorOnlyDiscoveries = findAggregatorOnlyDiscoveries(aggregatorReports)

        rawHtmlArchiver.cleanup()

        val sourcesFailed = developerReports.count { !it.fetchSucceeded || !it.validation.valid } +
            aggregatorReports.count { !it.fetchSucceeded || !it.validation.valid } +
            discoveryReports.count { !it.fetchSucceeded }
        val sourcesChecked = developerReports.size + aggregatorReports.size + discoveryReports.size
        val newInvestments = developerReports.sumOf { r -> r.changes.count { it.change.type == ChangeType.NEW } }

        val finishedAt = Instant.now(clock)
        val status = if (sourcesFailed == 0) RunStatus.SUCCESS else RunStatus.PARTIAL_FAILURE
        monitoringRunRepository.finish(runId, finishedAt, status, sourcesChecked, sourcesFailed, newInvestments)
        logger.info(
            "Scan finished: status={} sourcesFailed={} newInvestments={} newSignals={} duration={}ms",
            status, sourcesFailed, newInvestments,
            discoveryReports.sumOf { it.newSignals.size },
            finishedAt.toEpochMilli() - startedAt.toEpochMilli()
        )

        return ScanReport(
            startedAt = startedAt,
            finishedAt = finishedAt,
            developerReports = developerReports,
            aggregatorReports = aggregatorReports,
            discoveryReports = discoveryReports,
            correlations = correlations,
            aggregatorOnlyDiscoveries = aggregatorOnlyDiscoveries
        )
    }

    // ---------------------------------------------------------------- developer

    private fun scanDeveloperSource(source: InvestmentSource): SourceReport =
        scanInvestmentSource(source.id, SourceCategory.DEVELOPER, source::fetch, analyze = true)

    // ---------------------------------------------------------------- aggregator

    private fun scanAggregatorSource(source: AggregatorSource): SourceReport =
        scanInvestmentSource(source.id, SourceCategory.AGGREGATOR, source::fetch, analyze = false)

    private fun scanInvestmentSource(
        sourceId: String,
        category: SourceCategory,
        fetch: () -> List<Investment>,
        analyze: Boolean
    ): SourceReport {
        val previousInvestments = investmentRepository.findAllBySource(sourceId)
        val previousSnapshot = sourceSnapshotRepository.find(sourceId)

        val fetchResult = runCatching(fetch)
        fetchResult.onFailure { error -> logger.warn("Fetch failed for source '{}': {}", sourceId, error.message) }
        val fetched = fetchResult.getOrDefault(emptyList())

        val validation = sourceValidator.validate(fetched, previousSnapshot?.investmentCount)
        if (!validation.valid) {
            logger.warn("Validation failed for source '{}': {}", sourceId, validation.reason)
        }
        val changes = changeDetector.detect(fetched, previousInvestments)
            .map { change -> if (analyze) processIfNew(change) else AnalyzedChange(change, analysis = null) }

        if (fetchResult.isSuccess && validation.valid) {
            commitInvestments(sourceId, category, changes.mapNotNull { it.change.current })
        } else {
            logger.info("Source '{}' not committed - trusted snapshot unchanged", sourceId)
        }

        return SourceReport(sourceId, fetchResult.isSuccess, validation, changes)
    }

    private fun processIfNew(change: InvestmentChange): AnalyzedChange {
        val current = change.current
        if (change.type != ChangeType.NEW || current == null) {
            return AnalyzedChange(change, analysis = null)
        }

        val enriched = detailEnricher.enrich(current)
        val analysis = investmentAnalyzer.analyze(enriched)
        return AnalyzedChange(change.copy(current = enriched), analysis)
    }

    private fun commitInvestments(sourceId: String, category: SourceCategory, investments: List<Investment>) {
        val seenAt = Instant.now(clock)
        investments.forEach { investment ->
            investmentRepository.upsert(investment, seenAt)
            recordInvestmentEvidence(investment, sourceId, category, seenAt)
        }
        sourceSnapshotRepository.save(
            SourceSnapshot(
                source = sourceId,
                capturedAt = seenAt,
                investmentCount = investments.size,
                contentHash = identityHash(investments.map { it.canonicalKey }),
                sourceCategory = category
            )
        )
    }

    private fun recordInvestmentEvidence(investment: Investment, sourceId: String, category: SourceCategory, seenAt: Instant) {
        val investmentId = investmentRepository.findIdByCanonicalKey(investment.canonicalKey) ?: return
        evidenceRepository.save(
            SourceEvidence(
                investmentId = investmentId,
                signalId = null,
                sourceId = sourceId,
                sourceCategory = category,
                capturedAt = seenAt,
                url = investment.url,
                extractionMethod = ExtractionMethod.PARSER,
                fieldName = "investment",
                fieldValue = investment.name
            )
        )
    }

    // ---------------------------------------------------------------- discovery

    private fun scanDiscoverySource(source: DiscoverySource): DiscoverySourceReport {
        val previousSignals = signalRepository.findAllBySource(source.id)

        val fetchResult = runCatching(source::fetch)
        fetchResult.onFailure { error -> logger.warn("Fetch failed for discovery source '{}': {}", source.id, error.message) }
        val fetched = fetchResult.getOrDefault(emptyList())

        val newSignals = fetched.filter { it.canonicalKey !in previousSignals }

        if (fetchResult.isSuccess) {
            val seenAt = Instant.now(clock)
            fetched.forEach { signal ->
                signalRepository.upsert(signal, seenAt)
                recordSignalEvidence(signal, seenAt)
            }
            sourceSnapshotRepository.save(
                SourceSnapshot(
                    source = source.id,
                    capturedAt = seenAt,
                    investmentCount = fetched.size,
                    contentHash = identityHash(fetched.map { it.canonicalKey }),
                    sourceCategory = SourceCategory.DISCOVERY
                )
            )
        } else {
            logger.info("Discovery source '{}' not committed - trusted snapshot unchanged", source.id)
        }

        return DiscoverySourceReport(
            sourceId = source.id,
            municipality = source.municipality,
            fetchSucceeded = fetchResult.isSuccess,
            totalSignals = fetched.size,
            newSignals = newSignals
        )
    }

    private fun recordSignalEvidence(signal: InvestmentSignal, seenAt: Instant) {
        val signalId = signalRepository.findIdByCanonicalKey(signal.canonicalKey) ?: return
        evidenceRepository.save(
            SourceEvidence(
                investmentId = null,
                signalId = signalId,
                sourceId = signal.source,
                sourceCategory = SourceCategory.DISCOVERY,
                capturedAt = seenAt,
                url = signal.url,
                extractionMethod = ExtractionMethod.PARSER,
                fieldName = "signal",
                fieldValue = signal.title
            )
        )
    }

    // ---------------------------------------------------------------- correlation

    private fun runCorrelation(): List<CorrelationCandidate> {
        val allInvestments = investmentRepository.findAll()
        val allSignals = signalRepository.findAll()
        val candidates = correlator.correlate(allInvestments, allSignals)

        val now = Instant.now(clock)
        candidates.forEach { candidate ->
            val investmentId = investmentRepository.findIdByCanonicalKey(candidate.investment.canonicalKey)
            val signalId = signalRepository.findIdByCanonicalKey(candidate.signal.canonicalKey)
            if (investmentId != null && signalId != null) {
                correlationRepository.save(
                    Correlation(
                        investmentId = investmentId,
                        signalId = signalId,
                        confidence = candidate.confidence,
                        matchedFeatures = candidate.matchedFeatures,
                        reason = candidate.reason,
                        createdAt = now
                    )
                )
            }
        }
        return candidates
    }

    // ---------------------------------------------------------------- aggregator-only discoveries

    private fun findAggregatorOnlyDiscoveries(aggregatorReports: List<SourceReport>): List<Investment> {
        val newAggregatorInvestments = aggregatorReports
            .flatMap { it.changes }
            .filter { it.change.type == ChangeType.NEW }
            .mapNotNull { it.change.current }
        if (newAggregatorInvestments.isEmpty()) return emptyList()

        val developerLocations = sourceRegistry.developerSources()
            .flatMap { investmentRepository.findAllBySource(it.id).values }
            .mapNotNull { it.location?.let(LocationCatalog::findIn) }
            .toSet()

        return newAggregatorInvestments.filter { investment ->
            val location = investment.location?.let(LocationCatalog::findIn)
            location == null || location !in developerLocations
        }
    }

    /**
     * Hashes the set of canonical keys currently known for a source - i.e.
     * "which investments exist", not "what their fields contain". Field-level
     * changes are already detected precisely by [ChangeDetector]; this hash
     * is only a cheap identity fingerprint for the snapshot record.
     */
    private fun identityHash(canonicalKeys: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        canonicalKeys.sorted().forEach { key -> digest.update(key.toByteArray()) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(MonitoringService::class.java)
    }
}
