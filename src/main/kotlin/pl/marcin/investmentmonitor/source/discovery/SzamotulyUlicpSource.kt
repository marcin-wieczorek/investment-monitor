package pl.marcin.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.scraping.PageFetcher
import pl.marcin.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Fetches the Gmina Szamotuły BIP "Ustalenie lokalizacji inwestycji celu
 * publicznego / Obwieszczenia - wszczęcie postępowania" register.
 *
 * Requires [pl.marcin.investmentmonitor.scraping.PlaywrightPageFetcher] to
 * be enabled (see ADR-007) - `bip.szamotuly.pl` is registered as a
 * browser-required host in
 * [pl.marcin.investmentmonitor.registry.DiscoverySourceRegistry].
 *
 * Unlike every other discovery source, this one does a per-announcement
 * detail fetch (see [SzamotulyUlicpParser] KDoc): the list page only
 * enumerates article URLs, and each article's own page holds the real
 * description. A failure fetching/parsing any single article is not fatal
 * to the whole source - it is skipped, same fail-open philosophy as
 * [pl.marcin.investmentmonitor.source.InvestmentDetailEnricher] - since
 * the register is small (single page, no pagination as of this writing).
 */
@Component
class SzamotulyUlicpSource(
    private val pageFetcher: PageFetcher,
    private val parser: SzamotulyUlicpParser = SzamotulyUlicpParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = SzamotulyUlicpParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val listHtml = pageFetcher.fetch(URI(LIST_URL))
        val articleUrls = parser.findArticleUrls(listHtml, LIST_URL)
        return articleUrls.mapNotNull { url ->
            runCatching {
                val articleHtml = pageFetcher.fetch(URI(url))
                parser.parseArticle(articleHtml, url)
            }.getOrNull()
        }
    }

    companion object {
        const val SOURCE_ID = "szamotuly-ulicp"
        const val LIST_URL = "https://bip.szamotuly.pl/m,2101,obwieszczenia-wszczecie-postepowania.html"
    }
}
