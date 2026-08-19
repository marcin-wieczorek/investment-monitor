package pl.marcin.investmentmonitor.source.discovery

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.domain.LocationCatalog
import pl.marcin.investmentmonitor.domain.SignalType
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Parses the Gmina Swarzędz BIP "Warunki zabudowy" (zoning-conditions
 * decisions) register.
 *
 * Structure verified against the live page at [SwarzedzWzSource.LIST_URL]
 * (TYPO3 CMS, server-rendered, no JavaScript required): every decision
 * document is a `<a class="download">` link whose text starts with a case
 * reference such as `WAU.6730.23.2026`, followed by a free-text description
 * that (for residential cases) names the property type, plot numbers and
 * often a village name, e.g.:
 *
 * > WAU.6730.23.2026 - budowa 74 budynków mieszkalnych jednorodzinnych w
 * > zabudowie bliźniaczej oraz 150 budynków mieszkalnych jednorodzinnych w
 * > zabudowie szeregowej, dz. 50/6-15, 50/17, Kruszewnia - decyzja końcowa
 *
 * Some links are graphical attachments ("Załącznik graficzny") without a
 * case reference; these are skipped rather than guessed at. The publish
 * date is not read from free text - it is embedded deterministically in
 * every document's own URL path as `.../<dd>_<mm>_<yyyy>/<file>.pdf`.
 *
 * The register itself occasionally reuses the exact same document URL for
 * two unrelated cases (a publishing mistake on the municipality's side, not
 * a parsing issue). Since signal identity follows the same
 * `source:normalized-url` scheme as [pl.marcin.investmentmonitor.domain.Investment]
 * (see docs/ADR-002-deterministic-diff.md), such a collision means only one
 * of the two colliding signals survives persistence - an accepted, rare
 * trade-off rather than a reason to diverge from the established identity
 * model.
 */
class SwarzedzWzParser {

    fun parse(html: String, baseUri: String = SwarzedzWzSource.LIST_URL): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("a.download").mapNotNull(::toSignal)
    }

    private fun toSignal(link: Element): InvestmentSignal? {
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val text = link.text().trim()
        val reference = REFERENCE.find(text)?.groupValues?.get(1) ?: return null

        val description = text
            .removePrefix(reference)
            .trim()
            .removePrefix("-")
            .trim()
            .replace(SIZE_SUFFIX, "")
            .trim()

        val detectedAt = DATE_IN_PATH.find(url.path)?.let { match ->
            val (day, month, year) = match.destructured
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
        } ?: Instant.EPOCH

        return InvestmentSignal(
            source = SwarzedzWzSource.SOURCE_ID,
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(description),
            signalType = SignalType.WZ_DECISION,
            title = description,
            reference = reference,
            detectedAt = detectedAt,
            url = url,
            rawFacts = mapOf("originalText" to text)
        )
    }

    companion object {
        const val MUNICIPALITY = "Swarzędz"
        private val REFERENCE = Regex("^([A-Z]{2,6}(?:\\.\\d+){2,4})\\s*-")
        private val SIZE_SUFFIX = Regex("\\(\\s*[\\d.,]+\\s*[KM]B\\s*\\.pdf\\s*\\)\\s*$")
        private val DATE_IN_PATH = Regex("/(\\d{2})_(\\d{2})_(\\d{4})/[^/]+$")
    }
}
