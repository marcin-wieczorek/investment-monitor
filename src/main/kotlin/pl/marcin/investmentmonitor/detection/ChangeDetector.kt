package pl.marcin.investmentmonitor.detection

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment

enum class ChangeType { NEW, CHANGED, UNCHANGED, REMOVED }

/**
 * A single detected change. [current] is null only for [ChangeType.REMOVED]
 * (the investment disappeared from the source); [previous] is null only for
 * [ChangeType.NEW]. Both are non-null for [ChangeType.CHANGED] and
 * [ChangeType.UNCHANGED].
 */
data class InvestmentChange(
    val type: ChangeType,
    val current: Investment?,
    val previous: Investment?
)

@Component
class ChangeDetector {

    fun detect(current: List<Investment>, previous: Map<String, Investment>): List<InvestmentChange> {
        val currentByKey = current.associateBy { it.canonicalKey }

        val presentChanges = current.map { now ->
            val old = previous[now.canonicalKey]
            val type = when {
                old == null -> ChangeType.NEW
                old == now -> ChangeType.UNCHANGED
                else -> ChangeType.CHANGED
            }
            InvestmentChange(type, now, old)
        }

        val removedChanges = previous.values
            .filterNot { it.canonicalKey in currentByKey }
            .map { InvestmentChange(ChangeType.REMOVED, current = null, previous = it) }

        return presentChanges + removedChanges
    }
}
