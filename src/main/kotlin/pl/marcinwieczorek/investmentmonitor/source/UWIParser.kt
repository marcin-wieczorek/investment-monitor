package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import java.net.URI

/**
 * Parses the UWI "Malta Wołkowyska" investment page.
 *
 * UWI currently has a single active investment, published as one static
 * landing page rather than a list. Its per-unit availability tables are
 * populated client-side by `tabulator.js` via an AJAX call not present in
 * the server-rendered HTML at all - per AGENTS.md ("don't fake it, document
 * as PLANNED" for anything JS-only), this parser does not attempt to
 * extract unit/area/price data from those tables. Only the investment's
 * name is reliably published in static HTML.
 */
class UWIParser {

    fun parse(html: String, baseUri: String = UWISource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        val name = document.selectFirst("h1")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return emptyList()

        return listOf(
            Investment(
                source = SourceId(SOURCE_ID),
                developer = DEVELOPER_NAME,
                name = name,
                url = URI(baseUri),
                location = "Poznań",
                propertyType = null,
                units = null,
                houseArea = null,
                plotArea = null,
                price = null,
                status = null,
                imageUrl = null
            )
        )
    }

    companion object {
        const val SOURCE_ID = "uwi"
        const val DEVELOPER_NAME = "UWI"
    }
}
