package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import java.net.URI

/**
 * Parses the Develia Poznań investment list page.
 *
 * Structure verified against the live page at [DeveliaSource.LIST_URL]:
 * the same investments are rendered three times on this page (a plain
 * card list, a visible per-district map list, and hidden per-district map
 * lists), all reusing similar class names - so this parser scopes strictly
 * to `div.cities-investments__row div.investment-box` to avoid triple-
 * counting, and uses `selectFirst` for the detail link since it also
 * appears twice per card with an identical href.
 *
 * No area, price or unit data is published on this list page - Develia
 * would need a per-investment detail-page fetch (like
 * [pl.marcinwieczorek.investmentmonitor.source.detail.TercjaDetailParser]) to get
 * those, which is out of scope for this parser.
 *
 * Each card may carry a `div.investment-box__new-label` marketing badge,
 * but only some of them describe sale readiness (verified: "Ostatnie
 * mieszkania!", "Gotowe do odbioru!", "Sprzedaż zakończona") - others are
 * generic marketing copy ("Top inwestycja", a neighbourhood tagline). Only
 * the recognized readiness keywords are mapped to [InvestmentStatus];
 * anything else is left null rather than guessed.
 */
class DeveliaParser {

    fun parse(html: String, baseUri: String = DeveliaSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.cities-investments__row div.investment-box").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val name = card.selectFirst("p.investment-box__title")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val link = card.selectFirst("a.investment-box__card-link") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = card.selectFirst("div.investment-box__address")?.text()?.trim()?.ifBlank { null }

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
            imageUrl = card.selectFirst("header.investment-box__header img.investment-box__thumbnail")
                ?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    private fun extractStatus(card: Element): InvestmentStatus? {
        val text = card.selectFirst("div.investment-box__new-label")?.text()?.trim()?.lowercase() ?: return null
        return when {
            text.contains("ostatni") -> InvestmentStatus.LAST_UNITS
            text.contains("gotowe") && text.contains("odbioru") -> InvestmentStatus.READY_FOR_HANDOVER
            text.contains("zakończona") || text.contains("zakonczona") -> InvestmentStatus.SOLD_OUT
            else -> null
        }
    }

    companion object {
        const val SOURCE_ID = "develia"
        const val DEVELOPER_NAME = "Develia"
    }
}
