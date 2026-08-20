package pl.marcin.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.Investment
import java.net.URI

/**
 * Parses the Duda Development Poznań investment list page.
 *
 * Structure verified against the live page at [DudaSource.LIST_URL]: each
 * investment is a `div.investment-col > div.investment-box` card. The
 * detail link's class is the same (`a.btn-custom`) for both active and
 * sold-out investments, but active ones link to an absolute external site
 * while sold-out ones use a relative path on dudadevelopment.pl itself -
 * `absUrl` handles both uniformly. The thumbnail is a CSS `background-image`
 * on `div.investment-pic`, not an `<img>` tag, so it needs manual URL
 * resolution against [baseUri].
 *
 * Area and unit count are only ever mentioned in free-text prose
 * (`div.investment-desc`), not consistently formatted/positioned across
 * cards, so - per AGENTS.md "never guess unpublished fields" - they are
 * left null here rather than regex-extracted from prose.
 */
class DudaParser {

    fun parse(html: String, baseUri: String = DudaSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.investment-col").mapNotNull { card -> toInvestment(card, baseUri) }
    }

    private fun toInvestment(card: Element, baseUri: String): Investment? {
        val box = card.selectFirst("div.investment-box") ?: return null
        val name = box.selectFirst("div.investment-name")?.text()?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val link = box.selectFirst("a.btn-custom") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val location = box.selectFirst("p.investment-district")?.text()?.trim()?.ifBlank { null }

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
            status = extractStatus(box),
            imageUrl = extractImageUrl(box, baseUri)
        )
    }

    private fun extractStatus(box: Element): pl.marcin.investmentmonitor.domain.InvestmentStatus? {
        val sign = box.selectFirst("div.investment-sold-sign") ?: return null
        return when {
            sign.hasClass("status-w-sprzedazy") -> pl.marcin.investmentmonitor.domain.InvestmentStatus.FOR_SALE
            sign.hasClass("status-sprzedaz-zakonczona") -> pl.marcin.investmentmonitor.domain.InvestmentStatus.SOLD_OUT
            else -> pl.marcin.investmentmonitor.domain.InvestmentStatus.UNKNOWN
        }
    }

    private fun extractImageUrl(box: Element, baseUri: String): String? {
        val style = box.selectFirst("div.investment-pic")?.attr("style") ?: return null
        val path = IMAGE_URL.find(style)?.groupValues?.get(1)?.takeIf(String::isNotBlank) ?: return null
        return runCatching { URI(baseUri).resolve(path).toString() }.getOrNull()
    }

    companion object {
        const val SOURCE_ID = "duda"
        const val DEVELOPER_NAME = "Duda Development"
        private val IMAGE_URL = Regex("url\\(([^)]+)\\)")
    }
}
