package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the EBF Development Poznań investment list page.
 *
 * Structure verified against the live page at [EBFSource.LIST_URL]: cards
 * are scoped to `div.invests__carousel a.invests__item` to avoid an
 * identical duplicate list rendered in the top-nav dropdown menu.
 * Thumbnails always have a static "no photo" placeholder in `img[src]`
 * (`ebf-nofoto.png`) - the real, lazy-loaded image lives in `data-src`.
 *
 * This list includes both residential investments and non-residential
 * listings (parking/storage units) published on the same page - the
 * parser extracts facts as published and leaves classification to
 * downstream domain logic, per AGENTS.md ("parser extracts facts").
 */
class EBFParser {

    fun parse(html: String, baseUri: String = EBFSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.invests__carousel a.invests__item").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("div.title .h1")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val url = card.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("div.title .ttu")?.text()?.trim()?.ifBlank { null }

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
            imageUrl = card.selectFirst("figure.img img")?.absUrl("data-src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "ebf"
        const val DEVELOPER_NAME = "EBF Development"
    }
}
