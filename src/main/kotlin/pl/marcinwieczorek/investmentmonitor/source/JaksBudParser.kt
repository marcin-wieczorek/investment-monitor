package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import java.net.URI

/**
 * Parses the JakśBud "Znajdź mieszkanie" page.
 *
 * Unlike every other developer parser in this codebase, this page publishes
 * a single investment ("Osiedle Natura Biedrusko" in Biedrusko, near
 * Poznań) as a per-unit availability table (a WordPress `wpDataTable`)
 * rather than a list of investment cards. This parser therefore returns at
 * most one [Investment], aggregating unit count and house/plot area from
 * the table rows rather than reading them from a single labelled field.
 *
 * The table's config declares `"serverSide":true`, but the real rows are
 * still present in the initial server-rendered HTML (a no-JS/SEO
 * fallback) - verified directly against the live page - alongside a
 * decorative loading-skeleton of empty `div` rows that must not be
 * mistaken for real data (this parser only selects `tr[id^=table_]`).
 */
class JaksBudParser {

    fun parse(html: String, baseUri: String = JaksBudSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        val name = document.select("div.elementor-widget-heading h2.elementor-heading-title")
            .lastOrNull()?.text()?.trim()?.takeIf(String::isNotBlank) ?: return emptyList()

        val rows = document.select("table tbody tr[id^=table_]")
        if (rows.isEmpty()) return emptyList()

        val houseAreas = rows.mapNotNull { row -> cellText(row, HOUSE_AREA_COLUMN)?.toDoubleOrNull() }
        val plotAreas = rows.mapNotNull { row ->
            cellText(row, PLOT_AREA_COLUMN)?.removeSuffix("m2")?.trim()?.toDoubleOrNull()
        }
        val propertyType = rows.mapNotNull { row -> cellText(row, TYPE_COLUMN) }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?.let(::mapPropertyType)

        return listOf(
            Investment(
                source = SOURCE_ID,
                developer = DEVELOPER_NAME,
                name = name,
                url = URI(baseUri),
                location = "Biedrusko",
                propertyType = propertyType,
                units = rows.size,
                houseArea = houseAreas.toAreaRange(),
                plotArea = plotAreas.toAreaRange(),
                price = null,
                status = null,
                imageUrl = document.selectFirst("div.elementor-widget-image img")?.absUrl("src")?.takeIf(String::isNotBlank)
            )
        )
    }

    private fun cellText(row: Element, index: Int): String? =
        row.select("td").getOrNull(index)?.text()?.trim()?.takeIf(String::isNotBlank)

    private fun mapPropertyType(text: String): PropertyType? = when {
        text.contains("szereg", ignoreCase = true) -> PropertyType.TERRACED
        text.contains("bliźni", ignoreCase = true) -> PropertyType.SEMI_DETACHED
        text.contains("wolnostoj", ignoreCase = true) -> PropertyType.DETACHED
        else -> null
    }

    private fun List<Double>.toAreaRange(): AreaRange? =
        if (isEmpty()) null else AreaRange(minOrNull(), maxOrNull())

    companion object {
        const val SOURCE_ID = "jaksbud"
        const val DEVELOPER_NAME = "JakśBud"
        private const val TYPE_COLUMN = 3
        private const val HOUSE_AREA_COLUMN = 4
        private const val PLOT_AREA_COLUMN = 8
    }
}
