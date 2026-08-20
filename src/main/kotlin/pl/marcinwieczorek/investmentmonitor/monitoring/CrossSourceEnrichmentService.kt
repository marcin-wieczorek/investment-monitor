package pl.marcinwieczorek.investmentmonitor.monitoring

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.marcinwieczorek.investmentmonitor.analysis.DeterministicScorer
import pl.marcinwieczorek.investmentmonitor.analysis.locationProfileFor
import pl.marcinwieczorek.investmentmonitor.correlation.DuplicateCandidate
import pl.marcinwieczorek.investmentmonitor.domain.DuplicateConfidence
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.SourceCategory
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentScoreRepository
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository
import pl.marcinwieczorek.investmentmonitor.source.SourceRegistry
import java.time.Clock
import java.time.Instant

/**
 * Closes data gaps (see AGENTS.md scoring completeness issue: most
 * developer list pages never publish price/plotArea/propertyType) by
 * borrowing already-parsed facts from a confirmed same-project duplicate
 * on another source - e.g. filling in the price RynekPierwotny published
 * for a developer-sourced investment that has none.
 *
 * Deliberately restricted to [DuplicateConfidence.HIGH] pairs only (never
 * MEDIUM/LOW) - enrichment silently changes what a user sees for an
 * investment, so it must be at least as certain as an outright merge, not
 * just "possibly the same project". Never overwrites a field the target
 * already has: developer-published facts remain authoritative over
 * borrowed ones (see [SourceCategory] ordering) - this only ever fills
 * gaps, never contradicts what a source already published about its own
 * investment.
 *
 * Split out of [MonitoringService] for independent testability.
 */
@Service
class CrossSourceEnrichmentService(
    private val investmentRepository: InvestmentRepository,
    private val evidenceRecordingService: EvidenceRecordingService,
    private val scorer: DeterministicScorer,
    private val investmentScoreRepository: InvestmentScoreRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sourceRegistry: SourceRegistry,
    private val clock: Clock = Clock.systemUTC()
) {

    fun enrichFromDuplicates(duplicates: List<DuplicateCandidate>) {
        val now = Instant.now(clock)
        duplicates.filter { it.confidence == DuplicateConfidence.HIGH }.forEach { candidate ->
            enrichFromPartner(target = candidate.investmentA, partner = candidate.investmentB, seenAt = now)
            enrichFromPartner(target = candidate.investmentB, partner = candidate.investmentA, seenAt = now)
        }
    }

    private fun enrichFromPartner(target: Investment, partner: Investment, seenAt: Instant) {
        val borrowedFacts = mutableListOf<Pair<String, String>>()
        val propertyType = target.propertyType ?: partner.propertyType?.also { borrowedFacts += "propertyType" to it.name }
        val houseArea = target.houseArea ?: partner.houseArea?.also {
            EvidenceRecordingService.formatAreaRange(it)?.let { v -> borrowedFacts += "houseArea" to v }
        }
        val plotArea = target.plotArea ?: partner.plotArea?.also {
            EvidenceRecordingService.formatAreaRange(it)?.let { v -> borrowedFacts += "plotArea" to v }
        }
        val price = target.price ?: partner.price?.also {
            EvidenceRecordingService.formatPriceRange(it)?.let { v -> borrowedFacts += "price" to v }
        }
        if (borrowedFacts.isEmpty()) return

        val enriched = target.copy(propertyType = propertyType, houseArea = houseArea, plotArea = plotArea, price = price)
        investmentRepository.upsert(enriched, seenAt)
        logger.info(
            "Enriched '{}' ({}) with {} borrowed from '{}' ({})",
            enriched.name, enriched.source, borrowedFacts.map { it.first }, partner.name, partner.source
        )

        val investmentId = investmentRepository.findIdByCanonicalKey(enriched.canonicalKey) ?: return
        val partnerCategory = categoryOf(partner.source) ?: return
        evidenceRecordingService.recordBorrowedFacts(
            investmentId, borrowedFacts, partner.source, partnerCategory, partner.url, seenAt
        )

        val locationProfile = locationProfileFor(enriched)
        val scoring = scorer.score(enriched, locationProfile, userPreferencesRepository.effectiveScoringProfile())
        investmentScoreRepository.save(enriched.canonicalKey, scoring, seenAt)
    }

    private fun categoryOf(sourceId: SourceId): SourceCategory? = when (sourceId.value) {
        in sourceRegistry.developerSources().map { it.id } -> SourceCategory.DEVELOPER
        in sourceRegistry.aggregatorSources().map { it.id } -> SourceCategory.AGGREGATOR
        else -> null
    }

    private companion object {
        val logger = LoggerFactory.getLogger(CrossSourceEnrichmentService::class.java)
    }
}
