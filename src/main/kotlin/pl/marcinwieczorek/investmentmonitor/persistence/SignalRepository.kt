package pl.marcinwieczorek.investmentmonitor.persistence

import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import java.time.Instant

interface SignalRepository {
    fun findAllBySource(source: SourceId): Map<String, InvestmentSignal>
    fun findAll(): List<InvestmentSignal>
    fun upsert(signal: InvestmentSignal, seenAt: Instant)
    fun findIdByCanonicalKey(canonicalKey: String): Long?
}
