package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import java.net.URI
import java.util.Locale

/**
 * Parses the Vastbouw Poznań investment archive.
 *
 * Structure verified against the live page at [VastbouwSource.LIST_URL]:
 * each investment is an `article.investment-item` card. No plain-text
 * investment name is published on this archive page itself (only a
 * logo image and a slogan) - only the URL slug identifies it - so the
 * name is derived from the last path segment of the detail URL, the same
 * fallback [ChronosParser] uses when a card publishes no name text.
 */
class VastbouwParser {

    fun parse(html: String, baseUri: String = VastbouwSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("article.investment-item").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val link = card.selectFirst("a.link-absolute") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val name = extractName(url) ?: return null
        val location = card.selectFirst("div.investment-short-desc p strong")?.text()?.trim()?.ifBlank { null }

        return Investment(
            source = SourceId(SOURCE_ID),
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
            imageUrl = extractImageUrl(card)?.let(::URI)
        )
    }

    private fun extractName(url: URI): String? {
        val slug = url.path.trimEnd('/').substringAfterLast('/').takeIf(String::isNotBlank) ?: return null
        return slug.split('-').joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }

    private fun extractImageUrl(card: Element): String? {
        val style = card.selectFirst("div.post-bg")?.attr("style") ?: return null
        return CssBackgroundImage.extractUrl(style)
    }

    companion object {
        const val SOURCE_ID = "vastbouw"
        const val DEVELOPER_NAME = "Vastbouw"
    }
}
