package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.jsoup.Jsoup
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Parses the Gmina Szamotuły BIP "Ustalenie lokalizacji inwestycji celu
 * publicznego / Obwieszczenia - wszczęcie postępowania" register.
 *
 * Same "Madkom SIDAS BIP" platform as [BukObwieszczeniaParser], but a
 * different sub-pattern: the list page
 * ([SzamotulyUlicpSource.LIST_URL]) only publishes a generic per-row title
 * ("Obwieszczenie Burmistrza Miasta i Gminy Szamotuły" for every row) - the
 * actual case description only exists on each row's own article page. This
 * therefore requires an index+detail fetch per announcement (one list
 * fetch, then one fetch per article), unlike every other discovery source
 * implemented so far which reads everything from a single page - see
 * [SzamotulyUlicpSource.fetch].
 *
 * Each article's real content lives in the `#article-section` element
 * (a stable, semantic id - not a styled-components hash, see
 * `RynekPierwotnyParser` KDoc for why that distinction matters). Its
 * WYSIWYG-authored body markup is inconsistent between articles (some use
 * `<p class="Standard">`, others a bare unstyled `<div>`), so the whole
 * section's text is taken as-is rather than targeting a specific child
 * element/class. The sibling `#attachments-container` (metadata, download
 * counts, change history) is deliberately excluded by only reading
 * `#article-section`.
 *
 * The dedicated "Decyzje o warunkach zabudowy" category
 * (`m,2096,decyzje-o-warunkach-zabudowy-na-podstawie-art-49a-kpa.html`) is
 * currently empty ("Brak artykułów") - not implemented since there is
 * nothing to verify a parser against (see AGENTS.md "no fake
 * implementations"); this register only covers public-purpose siting
 * ("celu publicznego"), classified accordingly by [toSignalType].
 */
class SzamotulyUlicpParser {

    /** Extracts every announcement's own article URL from the list page. */
    fun findArticleUrls(listHtml: String, baseUri: String): List<String> {
        val document = Jsoup.parse(listHtml, baseUri)
        return document.select("[data-testid=test-Table] table tbody tr a[href*=\"/a,\"]")
            .mapNotNull { link -> link.absUrl("href").takeIf(String::isNotBlank) }
    }

    /** Parses a single announcement's own article page. */
    fun parseArticle(articleHtml: String, articleUrl: String): InvestmentSignal? {
        val document = Jsoup.parse(articleHtml, articleUrl)
        val section = document.selectFirst("#article-section") ?: return null
        val title = section.text().trim().takeIf(String::isNotBlank) ?: return null

        val detectedAt = document.select("span")
            .map { it.text() }
            .firstOrNull { it.startsWith("Data:") }
            ?.removePrefix("Data:")
            ?.trim()
            ?.let { text -> runCatching { LocalDateTime.parse(text, DATE_FORMAT).toInstant(ZoneOffset.UTC) }.getOrNull() }
            ?: Instant.EPOCH

        return InvestmentSignal(
            source = SzamotulyUlicpSource.SOURCE_ID,
            municipality = MUNICIPALITY,
            location = LocationCatalog.findIn(title),
            signalType = toSignalType(title),
            title = title,
            reference = REFERENCE.find(title)?.value,
            detectedAt = detectedAt,
            url = URI(articleUrl),
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
        const val MUNICIPALITY = "Szamotuły"
        private val REFERENCE = Regex("[A-ZĄĆĘŁŃÓŚŹŻ]{2,6}(?:-[A-Z]{1,4})?\\.\\d+(?:\\.\\d+){1,4}(?:\\.[A-Z]{1,3})?")
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
