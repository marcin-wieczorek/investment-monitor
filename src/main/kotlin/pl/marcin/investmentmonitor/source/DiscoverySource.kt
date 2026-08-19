package pl.marcin.investmentmonitor.source

import pl.marcin.investmentmonitor.domain.InvestmentSignal

/**
 * A source that observes early, official/public evidence of planned
 * residential development *before* a marketable investment necessarily
 * exists - e.g. a municipality's zoning-conditions ("warunki zabudowy")
 * register.
 *
 * Unlike [InvestmentSource], a discovery source does not claim to know a
 * project name, price or unit count: it returns [InvestmentSignal]s,
 * which are evidence, not investments (see docs/DISCOVERY.md).
 */
interface DiscoverySource {
    val id: String
    val municipality: String
    fun fetch(): List<InvestmentSignal>
}
