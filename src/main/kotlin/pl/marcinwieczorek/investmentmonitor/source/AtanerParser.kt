package pl.marcinwieczorek.investmentmonitor.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import java.net.URI

/**
 * Parses the Ataner homepage investment teaser slider.
 *
 * Structure verified against the live page at [AtanerSource.LIST_URL]: the
 * slider is duplicated for desktop (`ul.bxslider`) and mobile
 * (`ul.bxslider-sm`) with the same investments but, in the fixture,
 * *disagreeing* unit counts between the two - so only the desktop
 * `ul.bxslider` is parsed. Each `span.sub-title` packs "Liczba mieszkań: N"
 * and the district into two `<br>`-separated lines, the same
 * `<br>`-splitting technique [GreenbudParser] uses. The thumbnail is a CSS
 * `background: url(...)` on `div.image`, and at least one card uses a
 * `<video>` instead of any static image.
 */
class AtanerParser {

    fun parse(html: String, baseUri: String = AtanerSource.LIST_URL): List<Investment> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("ul.bxslider > li").mapNotNull(::toInvestment)
    }

    private fun toInvestment(card: Element): Investment? {
        val link = card.selectFirst("a.box") ?: return null
        val name = link.selectFirst("span.title")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        val subtitle = link.selectFirst("span.sub-title")
        val lines = subtitle?.html()
            ?.split(Regex("<br\\s*/?>"))
            ?.map { fragment -> Jsoup.parse(fragment).text().trim() }
            ?.filter(String::isNotBlank)
            ?: emptyList()
        val units = lines.firstNotNullOfOrNull { line -> UNITS_PATTERN.find(line)?.groupValues?.get(1)?.toIntOrNull() }
        val location = lines.firstOrNull { !it.contains(UNITS_PREFIX) }

        return Investment(
            source = SourceId(SOURCE_ID),
            developer = DEVELOPER_NAME,
            name = name,
            url = url,
            location = location,
            propertyType = null,
            units = units,
            houseArea = null,
            plotArea = null,
            price = null,
            status = null,
            imageUrl = extractImageUrl(link)?.let(::URI)
        )
    }

    private fun extractImageUrl(link: Element): String? {
        val style = link.selectFirst("div.image")?.attr("style") ?: return null
        val path = IMAGE_URL.find(style)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank) ?: return null
        return runCatching { link.baseUri().let(::URI).resolve(path).toString() }.getOrNull()
    }

    companion object {
        const val SOURCE_ID = "ataner"
        const val DEVELOPER_NAME = "Ataner"
        private const val UNITS_PREFIX = "Liczba mieszkań"
        private val UNITS_PATTERN = Regex("Liczba mieszkań:\\s*(\\d+)")
        private val IMAGE_URL = Regex("url\\(\\s*'?([^')]+)'?\\s*\\)")
    }
}
