package pl.marcinwieczorek.investmentmonitor.monitoring

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.marcinwieczorek.investmentmonitor.analysis.DeterministicScorer
import pl.marcinwieczorek.investmentmonitor.analysis.InvestmentAnalyzer
import pl.marcinwieczorek.investmentmonitor.analysis.locationProfileFor
import pl.marcinwieczorek.investmentmonitor.archival.RawHtmlArchiver
import pl.marcinwieczorek.investmentmonitor.correlation.CorrelationCandidate
import pl.marcinwieczorek.investmentmonitor.correlation.DuplicateCandidate
import pl.marcinwieczorek.investmentmonitor.correlation.InvestmentCorrelator
import pl.marcinwieczorek.investmentmonitor.correlation.InvestmentDeduplicator
import pl.marcinwieczorek.investmentmonitor.detection.ChangeDetector
import pl.marcinwieczorek.investmentmonitor.detection.ChangeType
import pl.marcinwieczorek.investmentmonitor.detection.InvestmentChange
import pl.marcinwieczorek.investmentmonitor.domain.Correlation
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentDuplicate
import pl.marcinwieczorek.investmentmonitor.domain.SourceCategory
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentScoreRepository
import pl.marcinwieczorek.investmentmonitor.persistence.MonitoringRunRepository
import pl.marcinwieczorek.investmentmonitor.persistence.RunStatus
import pl.marcinwieczorek.investmentmonitor.persistence.SignalRepository
import pl.marcinwieczorek.investmentmonitor.persistence.SourceSnapshotRepository
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentDuplicateRepository
import pl.marcinwieczorek.investmentmonitor.reporting.AnalyzedChange
import pl.marcinwieczorek.investmentmonitor.reporting.DiscoverySourceReport
import pl.marcinwieczorek.investmentmonitor.reporting.ScanReport
import pl.marcinwieczorek.investmentmonitor.reporting.SourceReport
import pl.marcinwieczorek.investmentmonitor.source.AggregatorSource
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import pl.marcinwieczorek.investmentmonitor.source.InvestmentDetailEnricher
import pl.marcinwieczorek.investmentmonitor.source.InvestmentSource
import pl.marcinwieczorek.investmentmonitor.source.SourceRegistry
import pl.marcinwieczorek.investmentmonitor.validation.SourceValidator
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
 *
 * Cross-source concerns that don't belong to the per-source scan loop
 * itself are delegated to dedicated services: [EvidenceRecordingService]
 * (provenance), [CrossSourceEnrichmentService] (gap-filling from HIGH
 * duplicates) and [AggregatorDiscoveryService] (aggregator-only detection
 * + unknown-developer candidates).
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
    private val evidenceRecordingService: EvidenceRecordingService,
    private val correlationRepository: CorrelationRepository,
    private val correlator: InvestmentCorrelator,
    private val duplicateRepository: InvestmentDuplicateRepository,
    private val deduplicator: InvestmentDeduplicator,
    private val rawHtmlArchiver: RawHtmlArchiver,
    private val crossSourceEnrichmentService: CrossSourceEnrichmentService,
    private val aggregatorDiscoveryService: AggregatorDiscoveryService,
    private val sourceCommitService: SourceCommitService,
    private val scorer: DeterministicScorer,
    private val investmentScoreRepository: InvestmentScoreRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    fun scan(): ScanReport {
        val startedAt = Instant.now(clock)
        val runId = monitoringRunRepository.start(startedAt)
        val developerSources = sourceRegistry.developerSources()
        val discoverySources = sourceRegistry.discoverySources()
        val aggregatorSources = sourceRegistry.aggregatorSources()
        val totalSources = developerSources.size + discoverySources.size + aggregatorSources.size
        logger.info(
            "Scan started ({} developer, {} discovery, {} aggregator sources)",
            developerSources.size,
            discoverySources.size,
            aggregatorSources.size
        )

        var sourcesScanned = 0
        val developerReports = developerSources.map { source ->
            sourcesScanned++
            logger.info("Scanning source [{}/{}]: '{}'", sourcesScanned, totalSources, source.id)
            scanDeveloperSource(source)
        }
        val aggregatorReports = aggregatorSources.map { source ->
            sourcesScanned++
            logger.info("Scanning source [{}/{}]: '{}'", sourcesScanned, totalSources, source.id)
            scanAggregatorSource(source)
        }
        val discoveryReports = discoverySources.map { source ->
            sourcesScanned++
            logger.info("Scanning source [{}/{}]: '{}'", sourcesScanned, totalSources, source.id)
            scanDiscoverySource(source)
        }

        val correlations = runCorrelation()
        val duplicates = runDeduplication()
        crossSourceEnrichmentService.enrichFromDuplicates(duplicates)
        val aggregatorOnlyDiscoveries = aggregatorDiscoveryService.findAggregatorOnlyDiscoveries(aggregatorReports)
        aggregatorDiscoveryService.recordUnknownDeveloperCandidates(aggregatorOnlyDiscoveries)
        aggregatorDiscoveryService.updateAggregatorOnlyDiscoveryFlags()

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
            aggregatorOnlyDiscoveries = aggregatorOnlyDiscoveries,
            leadTimes = correlationRepository.findAllWithLeadTime(),
            duplicates = duplicates
        )
    }

    // ---------------------------------------------------------------- developer

    private fun scanDeveloperSource(source: InvestmentSource): SourceReport =
        scanInvestmentSource(SourceId(source.id), SourceCategory.DEVELOPER, source::fetch, analyze = true)

    // ---------------------------------------------------------------- aggregator

    private fun scanAggregatorSource(source: AggregatorSource): SourceReport =
        scanInvestmentSource(SourceId(source.id), SourceCategory.AGGREGATOR, source::fetch, analyze = false)

    private fun scanInvestmentSource(
        sourceId: SourceId,
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
            val seenAt = Instant.now(clock)
            sourceCommitService.commitInvestments(sourceId, category, changes.mapNotNull { it.change.current }, seenAt)
        } else {
            logger.info("Source '{}' not committed - trusted snapshot unchanged", sourceId)
        }

        return SourceReport(sourceId.value, fetchResult.isSuccess, validation, changes)
    }

    private fun processIfNew(change: InvestmentChange): AnalyzedChange {
        val current = change.current
        if (change.type != ChangeType.NEW || current == null) {
            return AnalyzedChange(change, analysis = null)
        }

        val enriched = detailEnricher.enrich(current)
        val locationProfile = locationProfileFor(enriched)

        val scoring = scorer.score(enriched, locationProfile, userPreferencesRepository.effectiveScoringProfile())
        investmentScoreRepository.save(enriched.canonicalKey, scoring, Instant.now(clock))

        val analysis = investmentAnalyzer.analyze(enriched, locationProfile)
        return AnalyzedChange(change.copy(current = enriched), analysis)
    }

    // ---------------------------------------------------------------- discovery

    private fun scanDiscoverySource(source: DiscoverySource): DiscoverySourceReport {
        val sourceId = SourceId(source.id)
        val previousSignals = signalRepository.findAllBySource(sourceId)

        val fetchResult = runCatching(source::fetch)
        fetchResult.onFailure { error -> logger.warn("Fetch failed for discovery source '{}': {}", sourceId, error.message) }
        val fetched = fetchResult.getOrDefault(emptyList())

        val newSignals = fetched.filter { it.canonicalKey !in previousSignals }

        if (fetchResult.isSuccess) {
            sourceCommitService.commitSignals(sourceId, fetched, Instant.now(clock))
        } else {
            logger.info("Discovery source '{}' not committed - trusted snapshot unchanged", sourceId)
        }

        return DiscoverySourceReport(
            sourceId = sourceId.value,
            municipality = source.municipality,
            fetchSucceeded = fetchResult.isSuccess,
            totalSignals = fetched.size,
            newSignals = newSignals
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

    // ---------------------------------------------------------------- deduplication

    /**
     * Finds cross-source duplicate investments over the FULL current
     * investment set (not just this run's new ones - same rationale as
     * [runCorrelation]), so a duplicate is detected as soon as both sides
     * exist, regardless of which scan first discovered which side.
     */
    private fun runDeduplication(): List<DuplicateCandidate> {
        val allInvestments = investmentRepository.findAll()
        val candidates = deduplicator.findDuplicates(allInvestments)

        val now = Instant.now(clock)
        candidates.forEach { candidate ->
            val idA = investmentRepository.findIdByCanonicalKey(candidate.investmentA.canonicalKey)
            val idB = investmentRepository.findIdByCanonicalKey(candidate.investmentB.canonicalKey)
            if (idA != null && idB != null) {
                duplicateRepository.save(
                    InvestmentDuplicate(
                        investmentIdA = idA,
                        investmentIdB = idB,
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

    /**
     * Hashes the set of canonical keys currently known for a source - i.e.
     * "which investments exist", not "what their fields contain". Field-level
     * changes are already detected precisely by [ChangeDetector]; this hash
     * is only a cheap identity fingerprint for the snapshot record - see
     * [SourceCommitService.commitInvestments]/[SourceCommitService.commitSignals].
     */
    private companion object {
        val logger = LoggerFactory.getLogger(MonitoringService::class.java)
    }
}
