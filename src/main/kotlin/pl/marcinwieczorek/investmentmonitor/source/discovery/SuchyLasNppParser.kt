package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.net.URI
import java.time.Instant

/**
 * Parses Gmina Suchy Las's BIP "Obwieszczenia NPP" (MPZP-related
 * announcements) list.
 *
 * Structure verified against the live page at [SuchyLasNppSource.LIST_URL]
 * (Logonet CMS): each announcement is an `article.search-website` with a
 * linked `<h2>` title and a `div.content` summary. Neither a case reference
 * nor a publish date is published anywhere on this list page (unlike
 * Swarzędz/Rekord-BIP sources) - both are left null/[Instant.EPOCH] rather
 * than guessed.
 */
class SuchyLasNppParser {

    fun parse(html: String, baseUri: String = SuchyLasNppSource.LIST_URL): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("article.search-website").mapNotNull(::toSignal)
    }

    private fun toSignal(article: Element): InvestmentSignal? {
        val link = article.selectFirst("h2 a") ?: return null
        val title = link.text().trim().takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        return InvestmentSignal(
            source = SuchyLasNppSource.SOURCE_ID,
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(title),
            signalType = SignalType.MPZP_CHANGE,
            title = title,
            reference = null,
            detectedAt = Instant.EPOCH,
            url = url,
            rawFacts = emptyMap()
        )
    }

    companion object {
        const val MUNICIPALITY = "Suchy Las"
    }
}
