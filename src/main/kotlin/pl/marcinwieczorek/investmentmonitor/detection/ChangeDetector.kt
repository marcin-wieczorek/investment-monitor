package pl.marcinwieczorek.investmentmonitor.detection

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment

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
        if (currentByKey.size != current.size) {
            // canonicalKey is meant to be unique by construction (source:url) - a
            // duplicate here almost certainly means a parser bug producing two
            // cards for the same URL. Logged rather than thrown: this must not
            // crash the whole scan, but it should be visible in the report.
            val duplicateKeys = current.groupingBy { it.canonicalKey }.eachCount().filterValues { it > 1 }.keys
            logger.warn("Duplicate canonicalKey(s) in current fetch result, only the last will be diffed: {}", duplicateKeys)
        }

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

    private companion object {
        val logger = LoggerFactory.getLogger(ChangeDetector::class.java)
    }
}
