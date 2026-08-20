package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.Investment

/**
 * Parses an investment's own detail page (which may live on a completely
 * different domain than the developer's list page - e.g. Chronos publishes
 * each investment on its own dedicated site) into additional domain fields
 * not available on the list page (property type, area, price, status).
 *
 * Implementations are matched to an investment generically by URL/host,
 * not by developer, so a single investment with its own site can be
 * supported without special-casing the owning [InvestmentSource].
 */
interface InvestmentDetailParser {
    fun supports(investment: Investment): Boolean
    fun enrich(investment: Investment, html: String): Investment
}
