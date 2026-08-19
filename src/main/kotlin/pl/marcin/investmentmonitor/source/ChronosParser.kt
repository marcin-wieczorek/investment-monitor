package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI
import java.util.Locale

/**
 * Parses the Chronos Development investment list page.
 *
 * Structure verified against the live page at [ChronosSource.LIST_URL]:
 * each investment renders as two `div.investment.investment-<slug>` cards
 * (a media card and a call-to-action card), but only the media card contains
 * the `a.investment-more` link with the external investment site URL and the
 * location text. The investment name is not published as plain text on this
 * page; it is only available via the card's CSS class suffix (e.g.
 * `investment-tercja` -> "Tercja"), so the parser derives the name from it.
 *
 * The list page does not publish property type, unit count, house/plot area,
 * price or status - those live on each investment's own external site and are
 * out of scope for this parser (see docs/SOURCES.md two-stage scraping note).
 * The media card's `div.investment-img` background-image is used as the
 * investment's thumbnail.
 */
class ChronosParser {

    fun parse(html: String, baseUri: String = ChronosSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("a.investment-more").mapNotNull(::toInvestment)
    }

    private fun toInvestment(link: Element): Investment? {
        val card = link.closest("div.investment") ?: return null
        val name = extractName(card) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = link.text()
            .replace(WHITESPACE, " ")
            .trim()
            .ifBlank { null }

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
            imageUrl = extractImageUrl(card)
        )
    }

    private fun extractImageUrl(card: Element): String? {
        val style = card.selectFirst("div.investment-img")?.attr("style") ?: return null
        return IMAGE_URL.find(style)?.groupValues?.get(1)?.takeIf(String::isNotBlank)
    }

    private fun extractName(card: Element): String? {
        val slug = card.classNames()
            .firstOrNull { it != "investment" && it.startsWith("investment-") }
            ?.removePrefix("investment-")
            ?.takeIf(String::isNotBlank)
            ?: return null

        return slug.split('-').joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }

    companion object {
        const val SOURCE_ID = "chronos"
        const val DEVELOPER_NAME = "Chronos Development"
        private val WHITESPACE = Regex("\\s+")
        private val IMAGE_URL = Regex("url\\(['\"]?([^'\"()]+)['\"]?\\)")
    }
}
