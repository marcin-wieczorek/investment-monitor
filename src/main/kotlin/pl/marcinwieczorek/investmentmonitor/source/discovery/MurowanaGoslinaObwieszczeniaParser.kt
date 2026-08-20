package pl.marcinwieczorek.investmentmonitor.source.discovery

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.net.URI
import java.time.Instant

/**
 * Parses the Gmina Murowana Goślina BIP "Obwieszczenia inne" (other
 * announcements) register.
 *
 * Structure verified against the live page at
 * [MurowanaGoslinaObwieszczeniaSource.LIST_URL]: each announcement is a
 * `div.t1.clickable` card with a single `p.title > a` link - both
 * zoning-conditions ("warunki zabudowy") and public-purpose siting
 * ("lokalizacja inwestycji celu publicznego") decisions are mixed
 * together here, classified by keyword in [toSignalType]. The list page
 * publishes no per-item date (only a case reference, e.g.
 * "PP.6730.298.2025", embedded in the title), so [InvestmentSignal.detectedAt]
 * falls back to [Instant.EPOCH] - the same documented fallback
 * [pl.marcinwieczorek.investmentmonitor.source.discovery.PoznanUlicpParser] uses
 * when no date can be parsed.
 */
class MurowanaGoslinaObwieszczeniaParser {

    fun parse(html: String, baseUri: String = MurowanaGoslinaObwieszczeniaSource.LIST_URL): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.t1.clickable p.title a[href]").mapNotNull(::toSignal)
    }

    private fun toSignal(link: Element): InvestmentSignal? {
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val title = link.text().trim().takeIf(String::isNotBlank) ?: return null
        val reference = REFERENCE.find(title)?.groupValues?.get(1)

        return InvestmentSignal(
            source = SourceId(MurowanaGoslinaObwieszczeniaSource.SOURCE_ID),
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(title),
            signalType = SignalTypeClassifier.fromTitle(title),
            title = title,
            reference = reference,
            detectedAt = Instant.EPOCH,
            url = url,
            rawFacts = emptyMap()
        )
    }

    companion object {
        const val MUNICIPALITY = "Murowana Goślina"
        private val REFERENCE = Regex("nr sprawy ([A-Z]{2,4}\\.\\d+(?:\\.\\d+)*)")
    }
}
