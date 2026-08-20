package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
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
 * No area, price or unit data is published on this list page, but each
 * card does carry a free-text readiness "ribbon" (`div.ribbon p`, e.g.
 * "ostatnie wolne mieszkania" / "gotowe do odbioru" / "w trakcie
 * realizacji" / "inwestycja zakończona"), mapped to [InvestmentStatus] by
 * keyword.
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
            status = extractStatus(card),
            imageUrl = card.selectFirst("img")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    private fun extractStatus(card: Element): InvestmentStatus? {
        val text = card.selectFirst("div.ribbon p")?.text()?.trim()?.lowercase() ?: return null
        return when {
            text.contains("ostatni") -> InvestmentStatus.LAST_UNITS
            text.contains("gotowe") && text.contains("odbioru") -> InvestmentStatus.READY_FOR_HANDOVER
            text.contains("w trakcie realizacji") -> InvestmentStatus.UNDER_CONSTRUCTION
            text.contains("zakończona") || text.contains("zakonczona") -> InvestmentStatus.SOLD_OUT
            else -> null
        }
    }

    companion object {
        const val SOURCE_ID = "jakon-inwest"
        const val DEVELOPER_NAME = "Jakon"
    }
}
