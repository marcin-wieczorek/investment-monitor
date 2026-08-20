package pl.marcin.investmentmonitor.source.discovery

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.domain.LocationCatalog
import pl.marcin.investmentmonitor.domain.SignalType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Parses the Gmina Kórnik BIP "Obwieszczenia i ogłoszenia" register (a
 * Drupal 11 site, fetched via [pl.marcin.investmentmonitor.scraping.PlaywrightPageFetcher]
 * per ADR-007 - plain HTTP returns an effectively empty server-rendered
 * shell, only hydrated client-side).
 *
 * Like [BukObwieszczeniaParser], the register is split one page per
 * calendar year - see [findCurrentYearUrl]. Each year page groups
 * announcements into accordion sections by department
 * (`div.site-accordion`); only "Wydział Planowania Przestrzennego"
 * (Department of Spatial Planning) publishes zoning-related decisions -
 * every other department (property management, environment, education,
 * ...) is irrelevant and skipped by matching the accordion header text
 * exactly, rather than trying to classify every department's content.
 *
 * Each announcement is an `<li>` with a title link followed by a
 * trailing ", VillageName" text node - both are extracted, but only the
 * title is used as [InvestmentSignal.title]; the village is not parsed
 * out directly (would require handling several inconsistent formats
 * observed live, e.g. "), Radzewo" or "Szczytniki, Koninko") - instead
 * [LocationCatalog.findIn] is run over the combined text, same approach
 * as `SwarzedzWzParser`.
 *
 * The list publishes each case's real document date as free text ("z dnia
 * 4 grudnia 2025 r.") rather than a machine-readable date - parsed via
 * [POLISH_MONTHS] rather than falling back to [Instant.EPOCH], since it's
 * reliably present on every single entry (verified: 290 entries in 2025
 * alone, all matching this exact phrasing).
 */
class KornikObwieszczeniaParser {

    /** Finds the current (highest) year's register URL from the index/category page. */
    fun findCurrentYearUrl(indexHtml: String, baseUri: String): String? {
        val document = Jsoup.parse(indexHtml, baseUri)
        return document.select("a[href]")
            // The sidebar nav tree duplicates every "<year> rok" link with a
            // relative href pointing at an unrelated section ("Wnioski o
            // udostępnienie informacji publicznej", not obwieszczenia) -
            // only the real in-body link uses an absolute https:// href,
            // verified against the live page for every year available.
            .filter { it.attr("href").startsWith("http") }
            .mapNotNull { link -> YEAR_LINK_TEXT.matchEntire(link.text().trim())?.groupValues?.get(1)?.toIntOrNull()?.let { it to link } }
            .maxByOrNull { (year, _) -> year }
            ?.second
            ?.absUrl("href")
            ?.takeIf(String::isNotBlank)
    }

    fun parse(html: String, baseUri: String): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        val planningAccordion = document.select("div.site-accordion").firstOrNull { accordion ->
            accordion.selectFirst(".site-accordion__header")?.text()?.trim() == PLANNING_DEPARTMENT
        } ?: return emptyList()

        return planningAccordion.select(".site-accordion__body li").mapNotNull(::toSignal)
    }

    private fun toSignal(item: Element): InvestmentSignal? {
        val link = item.selectFirst("a") ?: return null
        val title = link.text().trim().takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(java.net.URI::create) ?: return null
        val fullText = item.text().trim()

        return InvestmentSignal(
            source = KornikObwieszczeniaSource.SOURCE_ID,
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(fullText),
            signalType = toSignalType(title),
            title = title,
            reference = REFERENCE.find(title)?.groupValues?.get(1),
            detectedAt = parseDate(title),
            url = url,
            rawFacts = emptyMap()
        )
    }

    private fun toSignalType(title: String): SignalType = when {
        title.contains("warunkach zabudowy", ignoreCase = true) ||
            title.contains("warunków zabudowy", ignoreCase = true) -> SignalType.WZ_DECISION
        title.contains("celu publicznego", ignoreCase = true) -> SignalType.LAND_DEVELOPMENT_SIGNAL
        else -> SignalType.OTHER
    }

    private fun parseDate(title: String): Instant {
        val match = DATE_IN_TEXT.find(title) ?: return Instant.EPOCH
        val (day, monthName, year) = match.destructured
        val month = POLISH_MONTHS[monthName.lowercase()] ?: return Instant.EPOCH
        return LocalDate.of(year.toInt(), month, day.toInt()).atStartOfDay(ZoneOffset.UTC).toInstant()
    }

    companion object {
        const val MUNICIPALITY = "Kórnik"
        private const val PLANNING_DEPARTMENT = "Wydział Planowania Przestrzennego"
        private val YEAR_LINK_TEXT = Regex("(\\d{4}) rok")
        private val REFERENCE = Regex("\\(([A-Z0-9-]+\\.\\d+(?:\\.\\d+)+)\\)")
        private val DATE_IN_TEXT = Regex("z dnia (\\d{1,2}) (\\p{L}+) (\\d{4}) r\\.")
        private val POLISH_MONTHS = mapOf(
            "stycznia" to 1, "lutego" to 2, "marca" to 3, "kwietnia" to 4,
            "maja" to 5, "czerwca" to 6, "lipca" to 7, "sierpnia" to 8,
            "września" to 9, "października" to 10, "listopada" to 11, "grudnia" to 12
        )
    }
}
