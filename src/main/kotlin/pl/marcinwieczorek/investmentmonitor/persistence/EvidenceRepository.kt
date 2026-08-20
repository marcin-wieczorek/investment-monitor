package pl.marcinwieczorek.investmentmonitor.persistence

import pl.marcinwieczorek.investmentmonitor.domain.SourceEvidence

/** Append-only provenance log - evidence is never updated or deleted, only added. */
interface EvidenceRepository {
    fun save(evidence: SourceEvidence)
    fun findByInvestment(investmentId: Long): List<SourceEvidence>
    fun findBySignal(signalId: Long): List<SourceEvidence>
}
