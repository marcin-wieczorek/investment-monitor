package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.AreaRange
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.PriceRange
import java.net.URI

/**
 * Parses Nickel Development's "Wyszukiwarka mieszkań" (apartment search)
 * results, [NickelSource.LIST_URL].
 *
 * Unlike the homepage/investment-listing pages investigated earlier
 * (see docs/SOURCES.md - those only ever show generic promotional tiles,
 * no real cards), this page is a traditional server-rendered Yii1/jQuery
 * search results grid - reachable with plain `JsoupPageFetcher`, no
 * headless browser needed at all, verified by fetching it with a plain
 * HTTP client directly. It publishes one row per available/reserved
 * *unit* (157 as of verification), paginated (`/p/2`, `/p/3`, ...), mixing
 * Poznań-area residential investments with seaside/mountain resort
 * properties ("Nickel Resort & ..." - excluded, see [isResort]).
 *
 * Since the grid is per-unit rather than per-investment, this parser
 * aggregates unit rows into one [Investment] per investment name (same
 * shape as `JaksBudParser`/`PWDParser`): unit count and area/price ranges
 * across all of that investment's currently-listed units. Property type,
 * location and per-investment status are not reliably published anywhere
 * on this page (units mix "Wolne"/"Rezerwacja" per row, no page-level
 * label) and are left `null` per the project's "no fake implementations"
 * rule - same as those two parsers.
 *
 * Each investment's identity (`Investment.url`) uses the search page's
 * own location filter (`id_loc[]`) as a real, navigable, per-investment
 * URL (e.g. `?id_loc%5B%5D=28` for Warzelnia II) rather than the shared
 * unfiltered search URL, which is essential: without it, every
 * aggregated investment from this source would collide on the same
 * canonical key (see docs/ADR-002-deterministic-diff.md). The
 * `id_loc[]` checkbox values/labels are read from the same page (any
 * page of the paginated results carries the full filter form), so this
 * requires no separate page/guessed URL scheme.
 */
class NickelParser {

    data class NickelUnit(val investmentName: String, val area: Double?, val price: Int?)

    /** Extracts every unit row's investment name, area and price from one results page. */
    fun parseUnits(html: String): List<NickelUnit> {
        val document = Jsoup.parse(html)
        return document.select("div.target-row").mapNotNull(::toUnit)
    }

    /** Finds the highest page number linked from the pagination control (1 if none/unpaginated). */
    fun findLastPage(html: String): Int {
        val document = Jsoup.parse(html)
        return document.select("div.pagination a[href]")
            .mapNotNull { link -> PAGE_NUMBER.find(link.attr("href"))?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 1
    }

    /** Maps investment name -> a real, filtered, per-investment search URL, from the `id_loc[]` filter checkboxes. */
    fun findInvestmentUrls(html: String, baseUri: String): Map<String, String> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("input[type=checkbox]")
            .filter { it.attr("name") == "id_loc[]" }
            .mapNotNull { checkbox ->
                val value = checkbox.attr("value").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val name = checkbox.parent()?.selectFirst("div")?.text()?.trim()?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                name to "$baseUri?id_loc%5B%5D=$value"
            }
            .toMap()
    }

    /** Aggregates unit rows into one [Investment] per (non-resort) investment name. */
    fun aggregate(units: List<NickelUnit>, investmentUrls: Map<String, String>): List<Investment> =
        units
            .filterNot { isResort(it.investmentName) }
            .groupBy { it.investmentName }
            .mapNotNull { (name, unitsForInvestment) ->
                val url = investmentUrls[name]?.let(::URI) ?: return@mapNotNull null
                val areas = unitsForInvestment.mapNotNull { it.area }
                val prices = unitsForInvestment.mapNotNull { it.price }

                Investment(
                    source = NickelSource.SOURCE_ID,
                    developer = DEVELOPER_NAME,
                    name = name,
                    url = url,
                    location = null,
                    propertyType = null,
                    units = unitsForInvestment.size,
                    houseArea = areas.toAreaRange(),
                    plotArea = null,
                    price = prices.toPriceRange(),
                    status = null,
                    imageUrl = null
                )
            }

    private fun toUnit(row: Element): NickelUnit? {
        val name = row.selectFirst("div.address strong")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val area = row.select("td:contains(Powierzchnia) + th").text().let(::parseArea)
        val price = row.selectFirst("div.price-offer h4 strong")?.text()?.let(::parsePrice)
        return NickelUnit(name, area, price)
    }

    private fun isResort(investmentName: String): Boolean = investmentName.contains("Resort", ignoreCase = true)

    private fun parseArea(text: String): Double? =
        AREA_NUMBER.find(text)?.value?.replace(",", ".")?.toDoubleOrNull()

    private fun parsePrice(text: String): Int? {
        val cleaned = text.replace("\u00A0", "").replace(" ", "")
        return cleaned.substringBefore(",").filter(Char::isDigit).toIntOrNull()
    }

    private fun List<Double>.toAreaRange(): AreaRange? =
        if (isEmpty()) null else AreaRange(minOrNull(), maxOrNull())

    private fun List<Int>.toPriceRange(): PriceRange? =
        if (isEmpty()) null else PriceRange(minOrNull(), maxOrNull())

    companion object {
        const val DEVELOPER_NAME = "Nickel Development"
        private val PAGE_NUMBER = Regex("/p/(\\d+)")
        private val AREA_NUMBER = Regex("[\\d,]+")
    }
}
