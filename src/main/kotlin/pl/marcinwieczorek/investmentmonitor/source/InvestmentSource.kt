package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.Investment

interface InvestmentSource {
    val id: String
    fun fetch(): List<Investment>
}
