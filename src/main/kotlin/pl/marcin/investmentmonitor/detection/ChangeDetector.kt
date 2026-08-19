package pl.marcin.investmentmonitor.detection

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment

enum class ChangeType { NEW, CHANGED, UNCHANGED }

data class InvestmentChange(
    val type: ChangeType,
    val current: Investment,
    val previous: Investment?
)

@Component
class ChangeDetector {

    fun detect(current: List<Investment>, previous: Map<String, Investment>): List<InvestmentChange> {
        return current.map { now ->
            val old = previous[now.canonicalKey]
            val type = when {
                old == null -> ChangeType.NEW
                old == now -> ChangeType.UNCHANGED
                else -> ChangeType.CHANGED
            }
            InvestmentChange(type, now, old)
        }
    }
}
