package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.PropertyType
import java.net.URI

/**
 * Parses Cordia's dedicated Poznań city page.
 *
 * Structure verified against the live page at [CordiaSource.LIST_URL]:
 * each investment is an `article.c-investment` card, with the location
 * published as the first entry of `ul.c-investment--main-list`. Only one
 * investment ("Modena") is currently listed for Poznań - Cordia's other
 * cities (Gdańsk, Kraków, Sopot, Warszawa) are excluded by this
 * city-scoped URL.
 *
 * The only unit figure shown ("63 wolne mieszkania") is a count of
 * currently available flats, not the total unit count for the
 * investment, so [Investment.units] stays null rather than misreporting
 * it. No area or price range is published on this page either.
 */
class CordiaParser {

    fun parse(html: String, baseUri: String = CordiaSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("article.c-investment").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val link = card.selectFirst("a.c-investment--wrapper") ?: return null
        val name = card.selectFirst("h3")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("ul.c-investment--main-list li span")?.text()?.trim()?.ifBlank { null }

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = location,
            propertyType = PropertyType.APARTMENT,
            units = null,
            houseArea = null,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = card.selectFirst("div.c-investment--top-img img")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "cordia"
        const val DEVELOPER_NAME = "Cordia"
    }
}
