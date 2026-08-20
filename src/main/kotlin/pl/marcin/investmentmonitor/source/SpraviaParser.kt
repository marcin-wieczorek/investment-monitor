package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.AreaRange
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Spravia (formerly Budimex Nieruchomości) Poznań investment
 * list page.
 *
 * Structure verified against the live page at [SpraviaSource.LIST_URL]:
 * each investment is a `div.inwestycje__card` with labelled stat rows
 * (`div.inwestycje__card-stat`), one of which is `"Metraże:"` giving a
 * hyphen-separated area range (e.g. `"31-98 m2"`) - a different format from
 * [PolishAreaFormat]'s "od X do Y"/"do X" phrasing, so it is parsed locally.
 * The page only publishes price *per square metre* (`"od 13 500,00 zł / m2"`),
 * not a total price, so [Investment.price] is deliberately left null rather
 * than guessing a total from a per-m2 figure.
 */
class SpraviaParser {

    fun parse(html: String, baseUri: String = SpraviaSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.inwestycje__card").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("h2.inwestycje__card-name")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val link = card.selectFirst("a.inwestycje__card-link") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("p.inwestycje__card-address")?.text()?.trim()?.ifBlank { null }

        val areaText = card.select("div.inwestycje__card-stat").firstOrNull {
            it.selectFirst("span.inwestycje__card-stat-label")?.text()?.contains(AREA_LABEL) == true
        }?.selectFirst("span.inwestycje__card-stat-value")?.text()

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = location,
            propertyType = null,
            units = null,
            houseArea = areaText?.let(::parseHyphenRange),
            plotArea = null,
            price = null,
            status = null,
            imageUrl = card.selectFirst("div.inwestycje__card-image img")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    private fun parseHyphenRange(text: String): AreaRange? {
        val match = HYPHEN_RANGE.find(text) ?: return null
        val (min, max) = match.destructured
        return AreaRange(min.replace(',', '.').toDouble(), max.replace(',', '.').toDouble())
    }

    companion object {
        const val SOURCE_ID = "spravia"
        const val DEVELOPER_NAME = "Spravia"
        private const val AREA_LABEL = "Metraże"
        private val HYPHEN_RANGE = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*-\\s*([0-9]+(?:[.,][0-9]+)?)")
    }
}
