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
 * Shared parser for the "Rekord BIP" municipal CMS used - with identical
 * markup - by both Gmina Czerwonak and Gmina Tarnowo Podgórne's BIP sites
 * (verified against both live pages). Each announcement is a
 * `div.list.list_date-sym` block with a case symbol, an ISO publish date,
 * and a linked title.
 *
 * Unlike Swarzędz's TYPO3 register, the symbol here is not always an
 * ASCII case-reference pattern (Czerwonak publishes bare numeric symbols
 * like `31027.2026` alongside `WOŚ.6220.31.2025`-style ones), so it is read
 * directly from its own `span.text-uppercase` element rather than
 * extracted with a reference regex.
 *
 * This is a genuinely shared, verified vendor template (not a speculative
 * universal scraper) - see docs/SOURCES.md for the two municipalities that
 * use it.
 */
class RekordBipParser(private val municipality: String, private val sourceId: String) {

    fun parse(html: String, baseUri: String): List<InvestmentSignal> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("div.list.list_date-sym").mapNotNull(::toSignal)
    }

    private fun toSignal(item: Element): InvestmentSignal? {
        val link = item.selectFirst("div.col-12.font-weight-bold a") ?: return null
        val title = link.text().trim().takeIf(String::isNotBlank) ?: return null
        val url = link.absUrl("href").takeIf(String::isNotBlank)?.let(::URI) ?: return null
        val reference = item.selectFirst("div.date_sym span.text-uppercase")?.text()?.trim()?.ifBlank { null }
        val detectedAt = item.selectFirst("div.date_sym div.col-8")
            ?.text()?.trim()
            ?.let { text -> runCatching { LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant() }.getOrNull() }
            ?: Instant.EPOCH

        return InvestmentSignal(
            source = sourceId,
            municipality = municipality,
            location = LocationCatalog.findIn(title),
            signalType = SignalType.WZ_DECISION,
            title = title,
            reference = reference,
            detectedAt = detectedAt,
            url = url,
            rawFacts = emptyMap()
        )
    }
}
