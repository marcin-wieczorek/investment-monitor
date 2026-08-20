package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.net.URI
import java.time.Instant

/**
 * Parses the Gmina Dopiewo BIP "Decyzje o warunkach zabudowy" (zoning-
 * conditions decisions) register (a Next.js/Nefeni-hosted BIP, fetched
 * via [pl.marcinwieczorek.investmentmonitor.scraping.PlaywrightPageFetcher] per
 * ADR-007 - the category index page itself is server-rendered, but this
 * particular category's article list is fetched client-side).
 *
 * Like [BukObwieszczeniaParser]/[SremWzParser], the register is split one
 * page per calendar year - see [findCurrentYearUrl]. Each year page's
 * real content lives in `div#category` (a stable, semantic id - not a
 * styled-components/Tailwind-utility hash), as a numbered list of
 * `<a title="...">` links whose `title` attribute holds the full case
 * text (reference + description) - more reliable than the link's own
 * text, which is identical here but `title` is what the site itself
 * treats as canonical (also used for the `<title>`-adjacent nav
 * duplicate of the same content elsewhere on the page, which this
 * parser avoids entirely by scoping to `#category`).
 *
 * No per-item date is published anywhere in this list (unlike Kórnik's
 * "z dnia ..." free text) - [InvestmentSignal.detectedAt] falls back to
 * [Instant.EPOCH], same documented fallback as `PoznanUlicpParser`.
 */
class DopiewoWzParser {

    /** Finds the current (highest) year's register URL from the index/category page. */
    fun findCurrentYearUrl(indexHtml: String, baseUri: String): String? {
        val document = Jsoup.parse(indexHtml, baseUri)
        return document.select("a[href]")
            .mapNotNull { link -> YEAR_IN_HREF.find(link.attr("href"))?.groupValues?.get(1)?.toIntOrNull()?.let { it to link } }
            .maxByOrNull { (year, _) -> year }
            ?.second
            ?.absUrl("href")
            ?.takeIf(String::isNotBlank)
    }

    fun parse(html: String, baseUri: String): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("#category a[title]").mapNotNull(::toSignal)
    }

    private fun toSignal(link: Element): InvestmentSignal? {
        val title = link.attr("title").trim().takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        return InvestmentSignal(
            source = DopiewoWzSource.SOURCE_ID,
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(title),
            signalType = SignalType.WZ_DECISION,
            title = title,
            reference = REFERENCE.find(title)?.value,
            detectedAt = Instant.EPOCH,
            url = url,
            rawFacts = emptyMap()
        )
    }

    companion object {
        const val MUNICIPALITY = "Dopiewo"
        private val YEAR_IN_HREF = Regex("/kategorie/\\d+-(\\d{4})(?:\\?|$)")
        private val REFERENCE = Regex("^[A-Z]{2,6}\\.\\d+(?:\\.\\d+)+")
    }
}
