package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Parses the Gmina Buk BIP "Obwieszczenia i komunikaty" register.
 *
 * Structure verified against the live pages at [BukObwieszczeniaSource.INDEX_URL]
 * and its current year's article, fetched via [pl.marcinwieczorek.investmentmonitor.scraping.PlaywrightPageFetcher]
 * (see ADR-007) - this BIP is a React SPA (the "Madkom SIDAS BIP" platform,
 * also used by Oborniki/Pobiedziska/Szamotuły) whose content only exists
 * after client-side hydration; plain HTTP (Jsoup) returns an empty shell.
 *
 * Like [SremWzParser], this register is split one page per calendar year
 * rather than a single evergreen feed - see [findCurrentYearUrl] - hence
 * the two-step fetch in [BukObwieszczeniaSource].
 *
 * Every announcement is a file attachment rendered as an `<li>` containing
 * a `<p>` description and a `Data: yyyy-MM-dd HH:mm:ss` metadata span,
 * identified by a `button#attachment-download-button-{fileId}` (a stable,
 * semantic id assigned by the app's own code - not a styled-components
 * hash, which regenerates on every deploy, see `RynekPierwotnyParser`
 * KDoc for why that distinction matters for selector stability). The
 * download itself is a plain GET at `/api/files/{fileId}` - confirmed to
 * serve the real attachment directly, without needing a browser - so
 * [InvestmentSignal.url] points there directly, a stable and unique
 * identity URL per announcement.
 *
 * Mixes zoning-conditions ("warunki zabudowy"), public-purpose siting
 * ("lokalizacja inwestycji celu publicznego") and various other municipal
 * announcements (MPZP consultations, environmental decisions, unrelated
 * administrative notices) in the same feed - classified by keyword in
 * [toSignalType], same approach as [MurowanaGoslinaObwieszczeniaParser].
 * Roughly half of all entries publish no case-reference number in their
 * visible text at all (only in the attached document itself) - left
 * `null` rather than guessed, per the project's "no fake implementations"
 * rule.
 */
class BukObwieszczeniaParser {

    /** Finds the current calendar year's register article URL from the index/category page. */
    fun findCurrentYearUrl(indexHtml: String, baseUri: String): String? {
        val document = Jsoup.parse(indexHtml, baseUri)
        return document.select("a[href*=obwieszczenia-i-komunikaty-]")
            .mapNotNull { link -> YEAR_IN_HREF.find(link.attr("href"))?.groupValues?.get(1)?.toIntOrNull()?.let { it to link } }
            .maxByOrNull { (year, _) -> year }
            ?.second
            ?.absUrl("href")
            ?.takeIf(String::isNotBlank)
    }

    fun parse(html: String, baseUri: String): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("li:has(button[id^=attachment-download-button-])").mapNotNull(::toSignal)
    }

    private fun toSignal(item: Element): InvestmentSignal? {
        val title = item.selectFirst("p")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val buttonId = item.selectFirst("button[id^=attachment-download-button-]")?.id() ?: return null
        val fileId = buttonId.substringAfterLast('-').toIntOrNull() ?: return null
        val url = URI("$FILES_BASE_URL$fileId")

        val detectedAt = item.select("span")
            .map { it.text() }
            .firstOrNull { it.startsWith("Data:") }
            ?.removePrefix("Data:")
            ?.trim()
            ?.let { text -> runCatching { LocalDateTime.parse(text, DATE_FORMAT).toInstant(ZoneOffset.UTC) }.getOrNull() }
            ?: Instant.EPOCH

        val reference = REFERENCE.find(title)?.value

        return InvestmentSignal(
            source = BukObwieszczeniaSource.SOURCE_ID,
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(title),
            signalType = toSignalType(title),
            title = title,
            reference = reference,
            detectedAt = detectedAt,
            url = url,
            rawFacts = emptyMap()
        )
    }

    private fun toSignalType(title: String): SignalType = when {
        title.contains("warunkach zabudowy", ignoreCase = true) ||
            title.contains("warunków zabudowy", ignoreCase = true) -> SignalType.WZ_DECISION
        title.contains("celu publicznego", ignoreCase = true) -> SignalType.LAND_DEVELOPMENT_SIGNAL
        title.contains("planu zagospodarowania", ignoreCase = true) ||
            title.contains("planu miejscowego", ignoreCase = true) ||
            title.contains("planu ogólnego", ignoreCase = true) -> SignalType.MPZP_CHANGE
        else -> SignalType.OTHER
    }

    companion object {
        const val MUNICIPALITY = "Buk"
        const val FILES_BASE_URL = "https://bip.buk.gmina.pl/api/files/"
        private val YEAR_IN_HREF = Regex("obwieszczenia-i-komunikaty-(\\d{4})-rok")
        private val REFERENCE = Regex("[A-ZĄĆĘŁŃÓŚŹŻ]{2,6}(?:-[A-Z]{1,4})?\\.\\d+(?:\\.\\d+){1,4}(?:\\.[A-Z]{1,3})?")
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
