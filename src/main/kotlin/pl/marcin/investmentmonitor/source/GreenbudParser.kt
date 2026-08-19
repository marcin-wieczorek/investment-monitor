package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Greenbud Development investment list page.
 *
 * Structure verified against the live page at [GreenbudSource.LIST_URL]:
 * the page is built with Elementor, so each investment renders as a
 * `div.e-con.e-child` container. Unlike Chronos, this developer publishes
 * location and house/plot area directly in a descriptive paragraph
 * (e.g. "Lokalizacja: ...<br>Pow. domu: ...<br>Pow. działek: ..."), and every
 * investment link stays on the greenbud.com.pl domain, so no separate
 * detail-page fetch is required to get these fields.
 *
 * Elementor's per-widget CSS classes (`elementor-element-<hash>`) are
 * regenerated whenever the page is edited, so the parser deliberately avoids
 * relying on them and only selects by stable structural classes
 * (`e-con.e-child`, `elementor-heading-title`, `elementor-button`,
 * `elementor-widget-text-editor`).
 */
class GreenbudParser {

    fun parse(html: String, baseUri: String = GreenbudSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.e-con.e-child").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("h2.elementor-heading-title")
            ?.text()
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: return null

        val details = card.select("div.elementor-widget-text-editor p")
            .firstOrNull { it.text().contains(LOCATION_PREFIX) }
            ?: return null

        val link = card.selectFirst("a.elementor-button") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        val lines = detailLines(details)
        val location = lines.firstNotNullOfOrNull { line -> valueAfterPrefix(line, LOCATION_PREFIX) }
        val houseAreaText = lines.firstNotNullOfOrNull { line -> valueAfterPrefix(line, HOUSE_AREA_PREFIX) }
        val plotAreaText = lines.firstNotNullOfOrNull { line ->
            valueAfterPrefix(line, PLOT_AREA_PREFIX) ?: valueAfterPrefix(line, GARDEN_AREA_PREFIX)
        }

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = location,
            propertyType = null,
            units = null,
            houseArea = houseAreaText?.let(PolishAreaFormat::parse),
            plotArea = plotAreaText?.let(PolishAreaFormat::parse),
            price = null,
            status = null,
            imageUrl = extractImageUrl(card)
        )
    }

    private fun extractImageUrl(card: Element): String? =
        card.selectFirst("div.elementor-widget-image img")
            ?.absUrl("src")
            ?.takeIf(String::isNotBlank)

    /** Splits a `<p>` containing `<br>`-separated fields into plain-text lines. */
    private fun detailLines(paragraph: Element): List<String> =
        paragraph.html()
            .split(Regex("<br\\s*/?>"))
            .map { fragment -> Jsoup.parse(fragment).text().trim() }
            .filter(String::isNotBlank)

    private fun valueAfterPrefix(line: String, prefix: String): String? =
        line.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.trim()

    companion object {
        const val SOURCE_ID = "greenbud"
        const val DEVELOPER_NAME = "Greenbud Development"
        private const val LOCATION_PREFIX = "Lokalizacja:"
        private const val HOUSE_AREA_PREFIX = "Pow. domu:"
        private const val PLOT_AREA_PREFIX = "Pow. działek:"
        private const val GARDEN_AREA_PREFIX = "Pow. ogrodów:"
    }
}
