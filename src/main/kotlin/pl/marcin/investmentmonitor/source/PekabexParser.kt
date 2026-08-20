package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Pekabex Development homepage investment slider.
 *
 * Structure verified against the live page at [PekabexSource.LIST_URL]:
 * this is a Webflow-built infinite-loop carousel, so every investment card
 * (`a.investment_slide_link_block`) is cloned several times in the raw
 * HTML for the seamless-loop effect - this parser deduplicates by URL to
 * compensate. No area, price or unit data is published on the homepage;
 * it would require a per-investment detail-page fetch.
 */
class PekabexParser {

    fun parse(html: String, baseUri: String = PekabexSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        val investments = document.select("a.investment_slide_link_block").mapNotNull(::toInvestment)
        return investments.distinctBy { it.url }
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("div.h5.font_color_transition")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val hrefAttr = card.attr("href").trim()
        if (hrefAttr.isBlank() || hrefAttr == "#") return null
        val url = card.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("div.text_14px")?.text()?.trim()?.ifBlank { null }

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
            imageUrl = card.selectFirst("img.investment_slide_image")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "pekabex"
        const val DEVELOPER_NAME = "Pekabex Development"
    }
}
