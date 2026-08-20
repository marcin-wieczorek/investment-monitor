package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the ATAL "mieszkania w Poznaniu" investment list page.
 *
 * Structure verified against the live page at [ATALSource.LIST_URL]: each
 * investment renders as a `div.promotions__list__item` wrapping a
 * `div.investmentBox` with three `div.investmentBox__data__line` rows -
 * location, name (`--name` modifier) and an "Dostępne mieszkania: N" unit
 * count - plus a lazy-loaded thumbnail (`data-lazy-src`, *not* `src`, which
 * is a placeholder data-URI).
 *
 * ATAL publishes multi-stage investments (e.g. "ATAL Idea Swarzędz" and
 * "ATAL Idea Swarzędz II") as separate cards that link to the *same*
 * external investment site - since [Investment.canonicalKey] is derived
 * from the URL, only the first card observed per URL is kept; later
 * stages of the same site are intentionally not treated as separate
 * investments (see docs/SOURCES.md ATAL note).
 *
 * No price, property type, or area data is published on this list page.
 */
class ATALParser {

    fun parse(html: String, baseUri: String = ATALSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        val investments = document.select("div.promotions__list__item").mapNotNull(::toInvestment)
        return investments.distinctBy { it.url }
    }

    private fun toInvestment(card: Element): Investment? {
        val box = card.selectFirst("div.investmentBox") ?: return null
        val lines = box.select("div.investmentBox__data__line")
        val name = lines.firstOrNull { it.hasClass("investmentBox__data__line--name") }
            ?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val location = lines.firstOrNull { !it.hasClass("investmentBox__data__line--name") && !it.text().contains(UNITS_PREFIX) }
            ?.text()?.trim()?.ifBlank { null }
        val units = lines.firstOrNull { it.text().contains(UNITS_PREFIX) }
            ?.text()
            ?.let { UNITS_PATTERN.find(it)?.groupValues?.get(1)?.toIntOrNull() }

        val link = box.selectFirst("a.investmentBox__link") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = location,
            propertyType = null,
            units = units,
            houseArea = null,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = extractImageUrl(box)
        )
    }

    private fun extractImageUrl(box: Element): String? =
        box.selectFirst("picture.investmentBox__image__picture--default source")
            ?.attr("data-lazy-srcset")
            ?.takeIf(String::isNotBlank)
            ?: box.selectFirst("picture.investmentBox__image__picture--default img")
                ?.attr("data-lazy-src")
                ?.takeIf(String::isNotBlank)

    companion object {
        const val SOURCE_ID = "atal"
        const val DEVELOPER_NAME = "ATAL"
        private const val UNITS_PREFIX = "Dostępne mieszkania"
        private val UNITS_PATTERN = Regex("Dostępne mieszkania:\\s*(\\d+)")
    }
}
