package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import pl.marcin.investmentmonitor.domain.AreaRange
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.PropertyType
import java.net.URI

/**
 * Parses a single PWD Deweloper "Osiedle Zagajnik" stage page (Etap I,
 * Etap II - see [PWDSource]).
 *
 * The developer's real site is `pwd-mieszkania.pl` - `pwd.com.pl` (the
 * domain previously recorded in this registry) is now an unrelated,
 * expired/parked domain (see docs/SOURCES.md). Fetched via
 * [pl.marcin.investmentmonitor.scraping.PlaywrightPageFetcher] (see
 * ADR-007): the page is a Leaflet SVG site-plan whose per-unit popups
 * (`div.hotspot-info`) are present in the initial HTML, but the plan
 * image/SVG itself is drawn by JS.
 *
 * Unlike every list-of-cards developer parser, this page publishes one
 * *stage* as a per-unit site-plan popup list rather than a list of
 * separate investments - same shape as `JaksBudParser`, which this
 * mirrors: aggregate unit count and house/plot area across all units
 * into a single [Investment] per stage page. Each unit's sale status
 * (sold/reserved/available) is encoded in its own `da-style-*` CSS class
 * (`sprzedane`/`rezerwacja`/`dostepny`) rather than a page-level label,
 * and stages mix all three - so [Investment.status] is deliberately left
 * `null` rather than force-fit into a single value (no price is
 * published anywhere on the page either, so [Investment.price] is also
 * `null`). The site-plan additionally contains a couple of purely
 * informational markers ("II Etap – już w sprzedaży", "III Etap w
 * przygotowaniu") with no unit data - excluded by requiring a `da-style-*`
 * class, which only real units have.
 */
class PWDParser {

    fun parse(html: String, baseUri: String): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        val name = document.title().substringBeforeLast(" – PWDdeweloper").trim().takeIf(String::isNotBlank)
            ?: return emptyList()

        val units = document.select("div.hotspot-info[class*=da-style-]")
        if (units.isEmpty()) return emptyList()

        val houseAreas = units.mapNotNull { unit ->
            unit.selectFirst("p.powierzchnia")?.text()?.let(AREA_NUMBER::find)?.groupValues?.get(1)?.let(::toDouble)
        }
        val plotAreas = units.mapNotNull { unit ->
            unit.selectFirst("p.dzialka")?.text()
                ?.takeIf { it.contains("działki", ignoreCase = true) }
                ?.let(AREA_NUMBER::find)?.groupValues?.get(1)?.let(::toDouble)
        }
        val propertyType = units.mapNotNull { unit -> unit.selectFirst("h2.hotspot-title")?.text() }
            .mapNotNull(::mapPropertyType)
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        return listOf(
            Investment(
                source = SOURCE_ID,
                developer = DEVELOPER_NAME,
                name = name,
                url = URI(baseUri),
                location = LOCATION,
                propertyType = propertyType,
                units = units.size,
                houseArea = houseAreas.toAreaRange(),
                plotArea = plotAreas.toAreaRange(),
                price = null,
                status = null,
                imageUrl = null
            )
        )
    }

    private fun mapPropertyType(title: String): PropertyType? = when {
        title.contains("bliźniak", ignoreCase = true) -> PropertyType.SEMI_DETACHED
        title.contains("szereg", ignoreCase = true) -> PropertyType.TERRACED
        title.contains("wolnostoj", ignoreCase = true) -> PropertyType.DETACHED
        else -> null
    }

    private fun toDouble(text: String): Double? = text.replace(",", ".").toDoubleOrNull()

    private fun List<Double>.toAreaRange(): AreaRange? =
        if (isEmpty()) null else AreaRange(minOrNull(), maxOrNull())

    companion object {
        const val SOURCE_ID = "pwd"
        const val DEVELOPER_NAME = "PWD Deweloper"
        const val LOCATION = "Poznań, Umultowo"
        private val AREA_NUMBER = Regex("([\\d.,]+)")
    }
}
