package pl.marcin.investmentmonitor.source.aggregator

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.AreaRange
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.PriceRange
import java.net.URI

/**
 * Parses RynekPierwotny.pl "nowe domy" (new houses) search-result listings.
 *
 * Structure verified against the live page at [RynekPierwotnySource.LIST_URL]
 * (server-rendered, no JavaScript required to obtain the listing itself).
 * The page's own CSS classes are content-hashed and regenerate on every
 * deploy (typical zero-runtime CSS-in-JS output), so this parser
 * deliberately never selects by class name. It instead anchors on the
 * `data-testid` attributes the site's own frontend tests rely on
 * (`property-container`, `offer-tile-offer-name`), which are far more
 * likely to stay stable across deploys, and falls back to plain-text
 * regex (`Metraż ... m2`, `... zł`) for numeric fields rather than
 * fragile structural selectors.
 *
 * As an aggregator, this source is a completeness/cross-check layer, not
 * a primary discovery mechanism (see docs/ARCHITECTURE.md source
 * precedence). Property type, unit count and status are not reliably
 * present on the listing tile and are deliberately left unset rather than
 * guessed.
 */
class RynekPierwotnyParser {

    fun parse(html: String, baseUri: String = RynekPierwotnySource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div[data-testid=property-container]").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val nameLink = card.selectFirst("a[data-testid=offer-tile-offer-name]") ?: return null
        val url = nameLink.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        val titleAttr = nameLink.attr("title").takeIf(String::isNotBlank) ?: nameLink.text()
        val (name, developer) = splitNameAndDeveloper(titleAttr)

        val location = nameLink.parent()
            ?.selectFirst("> p")
            ?.text()
            ?.substringAfterLast(',')
            ?.trim()
            ?.takeIf(String::isNotBlank)

        val cardText = card.text()
        val houseArea = AREA.find(cardText)?.let { match ->
            val min = toDouble(match.groupValues[1])
            val max = match.groupValues[2].takeIf(String::isNotBlank)?.let(::toDouble) ?: min
            AreaRange(min, max)
        }
        val price = PRICE_FROM.find(cardText)?.let { match ->
            PriceRange(min = toInt(match.groupValues[1]), max = null)
        }

        return Investment(
            source = RynekPierwotnySource.SOURCE_ID,
            developer = developer ?: DEFAULT_DEVELOPER,
            name = name,
            url = url,
            location = location,
            propertyType = null,
            units = null,
            houseArea = houseArea,
            plotArea = null,
            price = price,
            status = null,
            imageUrl = extractImageUrl(card)
        )
    }

    private fun extractImageUrl(card: Element): String? =
        card.selectFirst("img")?.absUrl("src")?.takeIf(String::isNotBlank)

    private fun splitNameAndDeveloper(titleAttr: String): Pair<String, String?> {
        val parts = titleAttr.split(" | ", limit = 2)
        val name = parts[0].trim()
        val developer = parts.getOrNull(1)?.trim()
        return name to developer
    }

    private fun toDouble(value: String): Double = value.replace(',', '.').toDouble()
    private fun toInt(value: String): Int = value.replace(WHITESPACE, "").toInt()

    companion object {
        const val DEFAULT_DEVELOPER = "Unknown (RynekPierwotny)"
        private val AREA = Regex("Metraż\\s+([0-9]+(?:[.,][0-9]+)?)(?:\\s*-\\s*([0-9]+(?:[.,][0-9]+)?))?\\s*m2")
        private val PRICE_FROM = Regex("od\\s*([0-9][0-9\\s]{2,9})\\s*zł(?!/)")
        private val WHITESPACE = Regex("\\s+")
    }
}
