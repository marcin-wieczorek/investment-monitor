package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Linea (suburban Poznań developer) investment list page.
 *
 * Structure verified against the live page at [LineaSource.LIST_URL]: each
 * investment is a `div.card.listing-cards-invest` card whose heading mixes
 * a leading city text node with a `span.sub-title` estate name
 * (`ownText()` is used to isolate the city, since `.text()` would
 * concatenate both). Price and area figures are only published in a
 * separate `application/ld+json` block keyed by URL, not in the card
 * markup itself, so - to keep this parser's extraction logic simple and
 * directly tied to visible card content - they are left null here.
 */
class LineaParser {

    fun parse(html: String, baseUri: String = LineaSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.card.listing-cards-invest").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val heading = card.selectFirst("div.content h3") ?: return null
        val subtitle = heading.selectFirst("span.sub-title")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val city = heading.ownText().trim().ifBlank { null }

        val link = card.selectFirst("a.image-wrapper") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = subtitle,
            url = url,
            location = city,
            propertyType = null,
            units = null,
            houseArea = null,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = link.selectFirst("img")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "linea"
        const val DEVELOPER_NAME = "Linea"
    }
}
