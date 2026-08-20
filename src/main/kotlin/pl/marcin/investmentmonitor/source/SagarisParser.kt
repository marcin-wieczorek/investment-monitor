package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Sagaris Poznań investment list page.
 *
 * Structure verified against the live page at [SagarisSource.LIST_URL]:
 * investments are `article.post-item` cards scoped inside
 * `div.city-investments` (the page's hero slider references investments
 * too, but lacks the `investment-short-desc`/`investment-info` structure
 * used here, so it is not selected). Both name and location appear twice
 * per card (`post-header` and `post-content`); this parser scopes to
 * `div.post-content-box` and takes the first match of each. Units and
 * house area are published as free-text bullet points (e.g. "247 mieszkań",
 * "metraże od 28 do 115 m2") rather than structured fields, so they are
 * regex/[PolishAreaFormat]-parsed from the `<li>` text.
 */
class SagarisParser {

    fun parse(html: String, baseUri: String = SagarisSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.city-investments article.post-item").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val box = card.selectFirst("div.post-content-box") ?: return null
        val name = box.selectFirst("h3")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val link = box.selectFirst("a.link-absolute") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = box.selectFirst("div.investment-info")?.text()?.trim()?.ifBlank { null }

        val bullets = box.select("div.investment-short-desc li").map { it.text() }
        val units = bullets.firstNotNullOfOrNull { UNITS_PATTERN.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val houseArea = bullets.firstOrNull { it.contains("m2") || it.contains("m²") }?.let(PolishAreaFormat::parse)

        return Investment(
            source = SOURCE_ID,
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = location,
            propertyType = null,
            units = units,
            houseArea = houseArea,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = extractImageUrl(card)
        )
    }

    private fun extractImageUrl(card: Element): String? {
        val style = card.selectFirst("div.post-bgimg")?.attr("style") ?: return null
        return IMAGE_URL.find(style)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)
    }

    companion object {
        const val SOURCE_ID = "sagaris"
        const val DEVELOPER_NAME = "Sagaris"
        private val UNITS_PATTERN = Regex("([0-9]+)\\s*mieszka")
        private val IMAGE_URL = Regex("url\\(([^)]+)\\)")
    }
}
