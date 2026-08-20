package pl.marcinwieczorek.investmentmonitor.monitoring

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.marcinwieczorek.investmentmonitor.detection.ChangeDetector
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.SourceCategory
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.persistence.SignalRepository
import pl.marcinwieczorek.investmentmonitor.persistence.SourceSnapshot
import pl.marcinwieczorek.investmentmonitor.persistence.SourceSnapshotRepository
import java.security.MessageDigest
import java.time.Instant

/**
 * Commits a source's fetched result (investments or signals) plus its
 * [SourceSnapshot] and per-fact [pl.marcinwieczorek.investmentmonitor.domain.SourceEvidence]
 * atomically.
 *
 * Deliberately a separate Spring bean (not methods on [MonitoringService])
 * so `@Transactional` actually takes effect - Spring's proxy-based AOP
 * never intercepts self-invocation (`this.someTransactionalMethod()`), so
 * these methods must be called through a different bean's proxy to get
 * transactional semantics (see docs review - "no explicit transaction
 * boundaries" finding).
 */
@Service
class SourceCommitService(
    private val investmentRepository: InvestmentRepository,
    private val signalRepository: SignalRepository,
    private val sourceSnapshotRepository: SourceSnapshotRepository,
    private val evidenceRecordingService: EvidenceRecordingService
) {

    @Transactional
    fun commitInvestments(sourceId: SourceId, category: SourceCategory, investments: List<Investment>, seenAt: Instant) {
        investments.forEach { investment ->
            investmentRepository.upsert(investment, seenAt)
            evidenceRecordingService.recordInvestmentEvidence(investment, sourceId, category, seenAt)
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

    @Transactional
    fun commitSignals(sourceId: SourceId, signals: List<InvestmentSignal>, seenAt: Instant) {
        signals.forEach { signal ->
            signalRepository.upsert(signal, seenAt)
            evidenceRecordingService.recordSignalEvidence(signal, seenAt)
        }
        sourceSnapshotRepository.save(
            SourceSnapshot(
                source = sourceId,
                capturedAt = seenAt,
                investmentCount = signals.size,
                contentHash = identityHash(signals.map { it.canonicalKey }),
                sourceCategory = SourceCategory.DISCOVERY
            )
        )
    }

    /**
     * Hashes the set of canonical keys currently known for a source - i.e.
     * "which investments/signals exist", not "what their fields contain".
     * Field-level changes are detected separately and precisely by
     * [ChangeDetector]; this hash is only a cheap identity fingerprint for
     * the snapshot record.
     */
    private fun identityHash(canonicalKeys: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        canonicalKeys.sorted().forEach { key -> digest.update(key.toByteArray()) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
