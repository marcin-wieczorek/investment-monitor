package pl.marcinwieczorek.investmentmonitor.source.discovery

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Parses the City of Poznań ULICP (public-purpose siting decision)
 * obwieszczenia table.
 *
 * Structure verified against the live page at [PoznanUlicpSource.LIST_URL]:
 * each row (`table.table > tbody > tr`) links its case reference
 * (`UA-IV.6733.<case>.<year>`) as the row's own title text, followed by a
 * `div` containing a large block of fixed legal boilerplate repeated on
 * every row. Rather than treat that whole block as a title, this parser
 * extracts just the investment description sentence following "określonej
 * przez inwestora/wnioskodawcę jako: "..."" - the same "strip boilerplate,
 * don't misuse the whole blob" approach [SwarzedzWzParser] takes. The
 * `td.center` validity-window text (`"od yyyy-MM-dd do yyyy-MM-dd"`) gives
 * the announcement's start date.
 */
class PoznanUlicpParser {

    fun parse(html: String, baseUri: String = PoznanUlicpSource.LIST_URL): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("table.table tbody tr").mapNotNull(::toSignal)
    }

    private fun toSignal(row: Element): InvestmentSignal? {
        val link = row.selectFirst("td.left a") ?: return null
        val reference = link.text().trim().takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null

        val bodyText = row.selectFirst("td.left div")?.text().orEmpty()
        val description = DESCRIPTION.find(bodyText)?.groupValues?.get(1)?.trim()
        val title = description ?: reference

        val detectedAt = row.selectFirst("td.center")?.text()?.let { text ->
            START_DATE.find(text)?.groupValues?.get(1)
                ?.let { date -> runCatching { LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant() }.getOrNull() }
        } ?: Instant.EPOCH

        return InvestmentSignal(
            source = SourceId(PoznanUlicpSource.SOURCE_ID),
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(bodyText),
            signalType = SignalType.LAND_DEVELOPMENT_SIGNAL,
            title = title,
            reference = reference,
            detectedAt = detectedAt,
            url = url,
            rawFacts = emptyMap()
        )
    }

    companion object {
        const val MUNICIPALITY = "Poznań"
        private val DESCRIPTION = Regex("okre[śs]lonej przez (?:inwestora|wnioskodawc[eę]) jako:\\s*\"([^\"]+)\"")
        private val START_DATE = Regex("od\\s+(\\d{4}-\\d{2}-\\d{2})")
    }
}
