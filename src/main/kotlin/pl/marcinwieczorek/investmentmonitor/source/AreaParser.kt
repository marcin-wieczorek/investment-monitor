package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Area Development "Nasza oferta" (currently marketed
 * investments) slider.
 *
 * Structure verified against the live page at [AreaSource.LIST_URL]: each
 * investment is a `div.panel` block with a name heading, a single detail
 * link and one or more photos - no separate location, area, price, unit
 * or status field is published, only free-text descriptions, so those
 * stay null rather than guessed. As of the last verification all four
 * currently marketed investments are on the coast (Ustronie Morskie,
 * Dziwnów) - Area Development's one Poznań investment
 * ("garsteckiego - wille piątkowo") is a completed project shown
 * separately under "Realizacje", not "Nasza oferta". This parser tracks
 * whatever this URL actually publishes so a new Poznań investment would
 * be picked up automatically without code changes.
 */
class AreaParser {

    fun parse(html: String, baseUri: String = AreaSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.panel").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("h3")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val link = card.selectFirst("a[href]") ?: return null
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
            imageUrl = card.selectFirst("img")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "area"
        const val DEVELOPER_NAME = "Area Development"
    }
}
