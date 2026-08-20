package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the ROBYG Poznań investment landing page.
 *
 * Structure verified against the live page at [RobygSource.LIST_URL]: this
 * CMS-built page renders investments in three different Swiper carousels
 * that overlap/duplicate each other (a hero slider, a pure photo gallery
 * with no investment identity, and a clean features slider) - this parser
 * scopes strictly to the clean one (`#module-4177`) to avoid duplicates and
 * noise. Thumbnails are lazy-loaded (`img.lazyload[data-src]`), with a
 * `<noscript>` fallback carrying the same real URL used here as a backup.
 *
 * No area, price, unit count or a real status enum is published on this
 * page - only marketing ribbon text (e.g. "Nowy etap").
 */
class RobygParser {

    fun parse(html: String, baseUri: String = RobygSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("#module-4177 div.swiper-slide.slide").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("h4 strong")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val link = card.selectFirst("a.slide-wrapper") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("p small")?.text()?.trim()?.ifBlank { null }

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
            imageUrl = extractImageUrl(card)
        )
    }

    private fun extractImageUrl(card: Element): String? =
        card.selectFirst("img.lazyload")?.absUrl("data-src")?.takeIf(String::isNotBlank)
            ?: card.selectFirst("noscript img")?.absUrl("src")?.takeIf(String::isNotBlank)

    companion object {
        const val SOURCE_ID = "robyg"
        const val DEVELOPER_NAME = "ROBYG"
    }
}
