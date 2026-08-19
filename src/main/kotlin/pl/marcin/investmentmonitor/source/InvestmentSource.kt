package pl.marcin.investmentmonitor.source

import pl.marcin.investmentmonitor.domain.Investment

interface InvestmentSource {
    val id: String
    fun fetch(): List<Investment>
}
