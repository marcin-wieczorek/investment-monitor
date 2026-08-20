package pl.marcinwieczorek.investmentmonitor.source.discovery

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Parses the Gmina Śrem BIP "warunki zabudowy" (zoning-conditions)
 * obwieszczenia register.
 *
 * Structure verified against the live index page at
 * [SremWzSource.INDEX_URL] and a live year page (e.g.
 * `http://bip.srem.pl/public/?id=238338` for 2026): unlike every other
 * discovery source implemented so far, this register is split one page per
 * calendar year (`li.element_podkategorii` links titled "Obwieszczenia,
 * komunikaty {year} r."), rather than a single evergreen feed - hence the
 * two-step fetch in [SremWzSource]. Each announcement document is itself
 * an `a.nazwa_pliku` link (a DOCX download, not an HTML page - still a
 * stable, unique URL for identity purposes) whose creation date is given
 * by the sibling `.wytworzyl_data` metadata field as `(yyyy-MM-dd)`.
 */
class SremWzParser {

    /** Finds the current year's register URL from the index page's year links. */
    fun findCurrentYearUrl(indexHtml: String, baseUri: String): String? {
        val document = Jsoup.parse(indexHtml, baseUri)
        return document.select("li.element_podkategorii a.nazwa_pliku")
            .mapNotNull { link -> YEAR.find(link.text())?.groupValues?.get(1)?.toIntOrNull()?.let { it to link } }
            .maxByOrNull { (year, _) -> year }
            ?.second
            ?.absUrl("href")
            ?.takeIf(String::isNotBlank)
    }

    fun parse(html: String, baseUri: String): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("li.element_podkategorii a.nazwa_pliku[href*=getFile]").mapNotNull(::toSignal)
    }

    private fun toSignal(link: Element): InvestmentSignal? {
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(java.net.URI::create) ?: return null
        val title = link.selectFirst("span")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null

        val item = link.closest("li.element_podkategorii")
        val detectedAt = item?.selectFirst(".wytworzyl_data")?.text()?.let { text ->
            DATE.find(text)?.value?.let { date -> runCatching { LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant() }.getOrNull() }
        } ?: Instant.EPOCH

        return InvestmentSignal(
            source = SourceId(SremWzSource.SOURCE_ID),
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(title),
            signalType = SignalType.WZ_DECISION,
            title = title,
            reference = null,
            detectedAt = detectedAt,
            url = url,
            rawFacts = emptyMap()
        )
    }

    companion object {
        const val MUNICIPALITY = "Śrem"
        private val YEAR = Regex("Obwieszczenia,\\s*komunikaty\\s*(\\d{4})")
        private val DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
    }
}
