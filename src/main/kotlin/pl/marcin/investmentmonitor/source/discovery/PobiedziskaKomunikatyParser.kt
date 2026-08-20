package pl.marcin.investmentmonitor.source.discovery

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.domain.LocationCatalog
import pl.marcin.investmentmonitor.domain.SignalType
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Parses the Gmina Pobiedziska BIP "Komunikaty" (planning announcements)
 * register, under "Planowanie przestrzenne".
 *
 * Same "Madkom SIDAS BIP" platform as [BukObwieszczeniaParser], but a
 * third sub-pattern: unlike Buk (per-attachment `<li>` descriptions on a
 * yearly article) and Szamotuły (generic list titles, real description
 * only on each row's own article page), Pobiedziska's list page
 * ([PobiedziskaKomunikatySource.LIST_URL]) already publishes each
 * announcement's *full* description directly in the list table's title
 * column - verified against the live page, where every `<td>` in the
 * first column contains the complete case text (500+ characters), not a
 * truncated preview. No detail-page fetch is needed here at all - a
 * single page read, same shape as [RekordBipParser].
 *
 * The dedicated "Warunki zabudowy" category
 * (`m,155,warunki-zabudowy.html`) is currently empty ("Brak artykułów") -
 * not implemented since there is nothing to verify a parser against (see
 * AGENTS.md "no fake implementations"); this register mixes
 * public-purpose siting ("celu publicznego") decisions with occasional
 * summary/index articles, classified by keyword in [toSignalType].
 */
class PobiedziskaKomunikatyParser {

    fun parse(html: String, baseUri: String): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("[data-testid=test-Table] table tbody tr").mapNotNull(::toSignal)
    }

    private fun toSignal(row: Element): InvestmentSignal? {
        val title = row.selectFirst("td")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val link = row.selectFirst("a[href*=\"/a,\"]") ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        val detectedAt = row.select("td").getOrNull(1)?.text()
            ?.let { text -> DATE_IN_CELL.find(text)?.value }
            ?.let { date -> runCatching { LocalDateTime.parse(date, DATE_FORMAT).toInstant(ZoneOffset.UTC) }.getOrNull() }
            ?: Instant.EPOCH

        return InvestmentSignal(
            source = PobiedziskaKomunikatySource.SOURCE_ID,
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(title),
            signalType = toSignalType(title),
            title = title,
            reference = REFERENCE.find(title)?.groupValues?.get(1),
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
        const val MUNICIPALITY = "Pobiedziska"
        private val REFERENCE = Regex("nr\\.?\\s*(\\d+/\\d{2,4})", RegexOption.IGNORE_CASE)
        private val DATE_IN_CELL = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
