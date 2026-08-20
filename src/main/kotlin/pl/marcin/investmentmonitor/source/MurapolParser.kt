package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Murapol Poznań investment list page.
 *
 * Structure verified against the live page at [MurapolSource.LIST_URL]:
 * the entire investment list is rendered twice - once in a desktop row
 * (`div.row.d-none.d-md-flex`) and once in an identical mobile row
 * (`div.row.d-block.d-md-none`) - so this parser scopes strictly to the
 * desktop row to avoid double-counting every investment.
 *
 * Only unit count and price-per-square-metre are published on the card;
 * no total area or total price, so [Investment.price] stays null rather
 * than guessing a total from a per-m2 figure (same reasoning as
 * [SpraviaParser]).
 */
class MurapolParser {

    fun parse(html: String, baseUri: String = MurapolSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.row.d-none.d-md-flex div.investments-item").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("h4.investments-item__desc__title")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val link = card.selectFirst("a[href]") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("span.investments-item__desc__address")?.text()?.trim()?.ifBlank { null }
        val units = card.selectFirst("span.investments-item__desc__flats")
            ?.text()
            ?.let { UNITS_PATTERN.find(it)?.groupValues?.get(1)?.toIntOrNull() }

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = location,
            propertyType = null,
            units = units,
            houseArea = null,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = card.selectFirst("picture.investment-image img")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "murapol"
        const val DEVELOPER_NAME = "Murapol"
        private val UNITS_PATTERN = Regex("([0-9]+)")
    }
}
