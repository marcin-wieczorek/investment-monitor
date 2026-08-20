package pl.marcinwieczorek.investmentmonitor.monitoring

import org.springframework.stereotype.Service
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.EvidenceOwner
import pl.marcinwieczorek.investmentmonitor.domain.ExtractionMethod
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.PriceRange
import pl.marcinwieczorek.investmentmonitor.domain.SourceCategory
import pl.marcinwieczorek.investmentmonitor.domain.SourceEvidence
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.persistence.EvidenceRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.persistence.SignalRepository
import java.net.URI
import java.time.Instant

/**
 * Records one [SourceEvidence] row per non-null fact a source actually
 * published (see AGENTS.md section 16), so provenance can answer "which
 * source(s) confirm this specific price/area/location", not just "this
 * source saw this investment". Split out of [MonitoringService] so
 * provenance recording is independently testable/reusable - both the
 * primary per-source commit path and [CrossSourceEnrichmentService] need it.
 */
@Service
class EvidenceRecordingService(
    private val evidenceRepository: EvidenceRepository,
    private val investmentRepository: InvestmentRepository,
    private val signalRepository: SignalRepository
) {

    fun recordInvestmentEvidence(investment: Investment, sourceId: SourceId, category: SourceCategory, seenAt: Instant) {
        val investmentId = investmentRepository.findIdByCanonicalKey(investment.canonicalKey) ?: return
        recordFacts(
            investmentFacts(investment),
            EvidenceOwner.ForInvestment(investmentId),
            sourceId,
            category,
            investment.url,
            seenAt
        )
    }

    fun recordSignalEvidence(signal: InvestmentSignal, seenAt: Instant) {
        val signalId = signalRepository.findIdByCanonicalKey(signal.canonicalKey) ?: return
        recordFacts(
            signalFacts(signal),
            EvidenceOwner.ForSignal(signalId),
            signal.source,
            SourceCategory.DISCOVERY,
            signal.url,
            seenAt
        )
    }

    /** Used by [CrossSourceEnrichmentService] to attribute borrowed facts to the partner source that originally published them. */
    fun recordBorrowedFacts(
        investmentId: Long,
        borrowedFacts: List<Pair<String, String>>,
        sourceId: SourceId,
        category: SourceCategory,
        url: URI,
        seenAt: Instant
    ) {
        recordFacts(borrowedFacts, EvidenceOwner.ForInvestment(investmentId), sourceId, category, url, seenAt)
    }

    private fun recordFacts(
        facts: List<Pair<String, String>>,
        owner: EvidenceOwner,
        sourceId: SourceId,
        category: SourceCategory,
        url: URI,
        seenAt: Instant
    ) {
        facts.forEach { (fieldName, fieldValue) ->
            evidenceRepository.save(
                SourceEvidence(
                    owner = owner,
                    sourceId = sourceId.value,
                    sourceCategory = category,
                    capturedAt = seenAt,
                    url = url,
                    extractionMethod = ExtractionMethod.PARSER,
                    fieldName = fieldName,
                    fieldValue = fieldValue
                )
            )
        }
    }

    fun investmentFacts(investment: Investment): List<Pair<String, String>> = buildList {
        add("name" to investment.name)
        investment.location?.let { add("location" to it) }
        investment.propertyType?.let { add("propertyType" to it.name) }
        investment.units?.let { add("units" to it.toString()) }
        investment.houseArea?.let(::formatAreaRange)?.let { add("houseArea" to it) }
        investment.plotArea?.let(::formatAreaRange)?.let { add("plotArea" to it) }
        investment.price?.let(::formatPriceRange)?.let { add("price" to it) }
        investment.status?.let { add("status" to it.name) }
        investment.imageUrl?.let { add("imageUrl" to it.toString()) }
    }

    private fun signalFacts(signal: InvestmentSignal): List<Pair<String, String>> = buildList {
        add("title" to signal.title)
        add("signalType" to signal.signalType.name)
        add("detectedAt" to signal.detectedAt.toString())
        signal.location?.let { add("location" to it) }
        signal.reference?.let { add("reference" to it) }
    }

    companion object {
        fun formatAreaRange(range: AreaRange): String? = when {
            range.min != null && range.max != null -> "${range.min}-${range.max}"
            range.min != null -> range.min.toString()
            range.max != null -> range.max.toString()
            else -> null
        }

        fun formatPriceRange(range: PriceRange): String? = when {
            range.min != null && range.max != null -> "${range.min}-${range.max}"
            range.min != null -> range.min.toString()
            range.max != null -> range.max.toString()
            else -> null
        }
    }
}
