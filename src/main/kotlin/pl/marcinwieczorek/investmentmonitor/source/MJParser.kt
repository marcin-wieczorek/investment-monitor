package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the MJ Deweloper homepage, which acts as a small hub linking out
 * to each investment's own independent domain (e.g. `naramowicka100.pl`,
 * `mieszkaniakolobrzeg.pl`) rather than hosting investment content itself -
 * the same pattern as [GGWParser].
 *
 * Structure verified against the live page at [MJSource.LIST_URL]: the hub
 * carousel (`.pxl-swiper-sliders .pxl-item--inner`) currently lists three
 * investments across MJ Deweloper's three cities (Rogowo, Kołobrzeg,
 * Poznań). Only name, external URL and a hero image are published here -
 * no location, area, price or unit data, so those stay null rather than
 * guessed.
 */
class MJParser {

    fun parse(html: String, baseUri: String = MJSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select(".pxl-swiper-sliders .pxl-item--inner").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst(".pxl-item--subtitle")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val link = card.selectFirst(".item--button") ?: return null
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
            imageUrl = extractImageUrl(card)
        )
    }

    private fun extractImageUrl(card: Element): String? {
        val style = card.selectFirst(".item--image")?.attr("style") ?: return null
        return IMAGE_URL.find(style)?.groupValues?.get(1)?.takeIf(String::isNotBlank)
    }

    companion object {
        const val SOURCE_ID = "mj"
        const val DEVELOPER_NAME = "MJ Deweloper"
        private val IMAGE_URL = Regex("url\\(['\"]?([^'\"()]+)['\"]?\\)")
    }
}
