package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Agrobex investment list published on the developer's homepage.
 *
 * Structure verified against the live page at [AgrobexSource.LIST_URL]:
 * each investment is an `article.investment-block` card with a city label,
 * title, an optional free-text readiness status, and a link to its own
 * `agrobex.pl` detail page. The unit/area/price stats block
 * (`div.hover-content__apartments`) is populated client-side via jQuery and
 * is empty in the server-rendered HTML, so no numeric data is extracted
 * from this list page.
 *
 * Agrobex's homepage is not Poznań-filtered - it lists every investment
 * nationwide, so some results (e.g. Zielona Góra, Sulechów) fall outside
 * the Metropolia Poznań scope. This parser deliberately does not filter
 * by location: that judgement belongs to [pl.marcin.investmentmonitor.domain.LocationCatalog]/
 * location scoring, not the parser (see AGENTS.md "parser extracts facts").
 */
class AgrobexParser {

    fun parse(html: String, baseUri: String = AgrobexSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("article.investment-block").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("h3.investment-block__title")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val link = card.selectFirst("a.investment-block__content") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("header.investment-block__header > span.investment-block__city")
            ?.text()?.trim()?.ifBlank { null }

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
            imageUrl = card.selectFirst("figure.investment-block__picture img")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        const val SOURCE_ID = "agrobex"
        const val DEVELOPER_NAME = "Agrobex"
    }
}
