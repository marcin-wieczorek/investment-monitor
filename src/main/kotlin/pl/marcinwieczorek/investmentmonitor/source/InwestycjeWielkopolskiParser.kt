package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import java.net.URI

/**
 * Parses the Inwestycje Wielkopolski "Realizacje" (completed projects)
 * page.
 *
 * Structure verified against the live page at
 * [InwestycjeWielkopolskiSource.LIST_URL]: each completed investment is a
 * `section` containing a link to its own `/realizacja/{slug}/` detail
 * page, a name, and a "{years}, UL. {street}, {city}" text line. The page
 * heading itself ("Zrealizowane PROJEKTY") is the only explicit status
 * signal published, so every investment here is marked [InvestmentStatus.SOLD_OUT]
 * rather than inferring it per-card.
 *
 * A separate "W sprzedaży" page exists but, as of the last verification,
 * only teases an unannounced upcoming project with no dedicated URL of
 * its own ("przed oficjalnym startem sprzedaży") - nothing stable enough
 * to parse without inventing an identity for it.
 */
class InwestycjeWielkopolskiParser {

    fun parse(html: String, baseUri: String = InwestycjeWielkopolskiSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("section")
            .filter { it.selectFirst("a[href*=/realizacja/]") != null }
            .mapNotNull(::toInvestment)
    }

    private fun toInvestment(section: Element): Investment? {
        val link = section.selectFirst("a[href*=/realizacja/]") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val texts = section.select("div.bde-text")
        val name = texts.getOrNull(0)?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val location = texts.getOrNull(1)?.text()?.trim()?.let { LEADING_YEARS.replace(it, "") }?.ifBlank { null }

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
            status = InvestmentStatus.SOLD_OUT,
            imageUrl = extractImageUrl(section)
        )
    }

    private fun extractImageUrl(section: Element): String? =
        section.select("img.bde-image2")
            .lastOrNull { !it.attr("src").contains("icon", ignoreCase = true) }
            ?.absUrl("src")
            ?.takeIf(String::isNotBlank)

    companion object {
        const val SOURCE_ID = "inwestycje_wielkopolski"
        const val DEVELOPER_NAME = "Inwestycje Wielkopolski"
        private val LEADING_YEARS = Regex("^[0-9,\\-\\s]+")
    }
}
