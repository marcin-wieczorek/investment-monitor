package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Konimpex-Invest full investment portfolio page.
 *
 * Structure verified against the live page at [KonimpexSource.LIST_URL]
 * (`/pl/inwestycje-w-sprzedazy`, titled "Zestawienie inwestycji" - not the
 * `/pl/inwestycje-2` path, which turned out to be a single investment's own
 * landing page, not a listing). Each investment is a `div.card.card-hover`
 * portfolio tile. The card body contains malformed, duplicated `<a>` tags
 * (a template bug on the live site) that make the visible `h4.card-title`
 * link's own text unreliable once Jsoup's parser auto-corrects the nesting -
 * so this parser instead reads the stable `a.portfolio-img[title]`
 * attribute, which carries the full "NAME, District City" text and is
 * unaffected by the broken nesting. Stripping the well-known
 * `/layout/frame` lightbox suffix recovers the investment's real page URL
 * without inventing anything not already present in the markup.
 *
 * This page lists Konimpex-Invest's full built portfolio, not only
 * currently-for-sale investments, so [Investment.status] is left null
 * rather than guessed - no sale-status field is published here.
 */
class KonimpexParser {

    fun parse(html: String, baseUri: String = KonimpexSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.card.card-hover").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val link = card.selectFirst("a.portfolio-img") ?: return null
        val fullText = link.attr("title").trim().takeIf(String::isNotBlank) ?: return null
        val (name, location) = splitNameAndLocation(fullText)

        val rawUrl = link.absUrl("href").takeIf(String::isNotBlank) ?: return null
        val url = URI(rawUrl.removeSuffix("/layout/frame"))

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
            imageUrl = link.selectFirst("img.card-img-top")?.absUrl("src")?.takeIf(String::isNotBlank)
        )
    }

    /** Card titles are formatted "NAME, District City" - splits on the last comma. */
    private fun splitNameAndLocation(text: String): Pair<String, String?> {
        val lastComma = text.lastIndexOf(',')
        if (lastComma < 0) return text to null
        return text.substring(0, lastComma).trim() to text.substring(lastComma + 1).trim().ifBlank { null }
    }

    companion object {
        const val SOURCE_ID = "konimpex"
        const val DEVELOPER_NAME = "Konimpex-Invest"
    }
}
