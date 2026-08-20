package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Jakon (Jakon Inwest) investment list page.
 *
 * Structure verified against the live page at [JakonInwestSource.LIST_URL]:
 * scoped strictly to `div.type-locations div.loc-boxes > div[data-location]`
 * to avoid the near-duplicate mega-menu at the top of the page which lists
 * similar investment names/links. Some investments link to entirely
 * external domains (`target="_blank"`) rather than staying on
 * jakon-inwest.pl, and a completed investment ("Kameralna Konotopska") has
 * no link at all, only a bare heading - such cards are skipped since an
 * [Investment] requires a URL.
 *
 * No area, price or unit data is published on this list page - only a
 * free-text readiness "ribbon" (e.g. "gotowe do odbioru").
 */
class JakonInwestParser {

    fun parse(html: String, baseUri: String = JakonInwestSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.type-locations div.loc-boxes > div[data-location]").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val link = card.selectFirst("h6.text-uppercase a") ?: return null
        val name = link.text().trim().takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("p.text-muted")?.text()?.trim()?.ifBlank { null }

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
            imageUrl = card.selectFirst("img")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "jakon-inwest"
        const val DEVELOPER_NAME = "Jakon"
    }
}
