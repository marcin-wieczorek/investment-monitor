package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.PropertyType
import java.net.URI

/**
 * Parses the Ronson Poznań investment listing page.
 *
 * Structure verified against the live page at [RonsonSource.LIST_URL]:
 * each active investment is a `div.investition-item` card with a clean
 * name/location/type structure. Only one investment is currently listed
 * for Poznań (`Grunwald Między Drzewami`) - Ronson's other cities
 * (Warszawa, Wrocław, Szczecin) are excluded by this dedicated URL.
 *
 * Price is only published per square metre ("od 11 794,89 zł za m2"), not
 * as a total price range, so [Investment.price] stays null rather than
 * guessing a total from a per-m2 figure.
 */
class RonsonParser {

    fun parse(html: String, baseUri: String = RonsonSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.investition-item").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("h2.investition-item__header")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val link = card.selectFirst("a.item-investition-a") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("div.investition-item__locations")?.text()?.trim()?.ifBlank { null }
        val types = card.select("span.item-investition-type").map { it.text().trim() }

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = location,
            propertyType = if (types.any { it.equals("Mieszkania", ignoreCase = true) }) PropertyType.APARTMENT else null,
            units = null,
            houseArea = null,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = extractImageUrl(card)
        )
    }

    private fun extractImageUrl(card: Element): String? =
        card.selectFirst("img.item-investition-img")?.absUrl("data-src")?.takeIf(String::isNotBlank)

    companion object {
        const val SOURCE_ID = "ronson"
        const val DEVELOPER_NAME = "Ronson"
    }
}
