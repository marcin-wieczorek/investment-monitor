package pl.marcin.investmentmonitor.source.detail

import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.AreaRange
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.source.InvestmentDetailParser
import pl.marcin.investmentmonitor.source.PolishAreaFormat

/**
 * Parses the Tercja investment's own dedicated site (tercja.eu), hosted
 * completely separately from chronos.poznan.pl. Structure verified against
 * the live page at https://www.tercja.eu.
 *
 * The site is a marketing single-pager with no structured price/unit-count
 * markup; the facts extracted here come from the descriptive paragraph in
 * the "Trzeci - na zawsze" section (wording verified against the live page,
 * see the fixture). This parser is deliberately specific to this one
 * investment's page rather than a shared template: Chronos investments each
 * get their own independently designed site, so a generic list-page-style
 * parser would not generalize (see [pl.marcin.investmentmonitor.source.InvestmentDetailParser]).
 *
 * Price and property type are not published on this page and are left
 * unset rather than guessed.
 */
@Component
class TercjaDetailParser : InvestmentDetailParser {

    override fun supports(investment: Investment): Boolean =
        investment.url.host?.removePrefix("www.") == "tercja.eu"

    override fun enrich(investment: Investment, html: String): Investment {
        val document = Jsoup.parse(html)
        val paragraph = document.select("#wakacje-w-kazde-popoludnie p")
            .firstOrNull { it.text().contains(SEGMENT_MARKER) }
            ?: return investment

        val text = paragraph.text()

        return investment.copy(
            units = UNITS.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: investment.units,
            houseArea = PolishAreaFormat.parse(text) ?: investment.houseArea,
            plotArea = plotAreaPerSegment(text) ?: investment.plotArea
        )
    }

    private fun plotAreaPerSegment(text: String): AreaRange? =
        PLOT_AREA_PER_SEGMENT.find(text)?.let { match ->
            AreaRange(min = match.groupValues[1].replace(',', '.').toDouble(), max = null)
        }

    private companion object {
        const val SEGMENT_MARKER = "segment"
        val UNITS = Regex("z\\s+([0-9]+)\\s+segment")
        val PLOT_AREA_PER_SEGMENT = Regex("ponad\\s+([0-9]+(?:[.,][0-9]+)?)\\s*m2\\s*dla\\s+jednego\\s+segmentu")
    }
}
