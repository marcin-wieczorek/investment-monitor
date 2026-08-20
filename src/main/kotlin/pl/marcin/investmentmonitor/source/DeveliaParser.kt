package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Develia Poznań investment list page.
 *
 * Structure verified against the live page at [DeveliaSource.LIST_URL]:
 * the same investments are rendered three times on this page (a plain
 * card list, a visible per-district map list, and hidden per-district map
 * lists), all reusing similar class names - so this parser scopes strictly
 * to `div.cities-investments__row div.investment-box` to avoid triple-
 * counting, and uses `selectFirst` for the detail link since it also
 * appears twice per card with an identical href.
 *
 * No area, price or unit data is published on this list page - Develia
 * would need a per-investment detail-page fetch (like
 * [pl.marcin.investmentmonitor.source.detail.TercjaDetailParser]) to get
 * those, which is out of scope for this parser.
 */
class DeveliaParser {

    fun parse(html: String, baseUri: String = DeveliaSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.cities-investments__row div.investment-box").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("p.investment-box__title")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val link = card.selectFirst("a.investment-box__card-link") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("div.investment-box__address")?.text()?.trim()?.ifBlank { null }

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = location,
            propertyType = null,
            units = null,
            houseArea = null,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = card.selectFirst("header.investment-box__header img.investment-box__thumbnail")
                ?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "develia"
        const val DEVELOPER_NAME = "Develia"
    }
}
