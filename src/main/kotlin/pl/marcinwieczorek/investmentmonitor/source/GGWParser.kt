package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the GGW Development homepage, which acts as a small hub linking
 * out to each investment's own independent domain (e.g. `bojerowa.pl`,
 * `hawelanska.pl`) rather than hosting investment content itself.
 *
 * Structure verified against the live page at [GGWSource.LIST_URL]: each
 * card is a `div.main-offer > a.nav` block. Only name, external URL and a
 * hero image are published here - no location, area, price or unit data,
 * so those stay null rather than guessed.
 */
class GGWParser {

    fun parse(html: String, baseUri: String = GGWSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.main-offer").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val link = card.selectFirst("a.nav") ?: return null
        val name = link.selectFirst("div.layer h3")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = null,
            propertyType = null,
            units = null,
            houseArea = null,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = link.select("div.image-fit img").lastOrNull()?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "ggw"
        const val DEVELOPER_NAME = "GGW Development"
    }
}
