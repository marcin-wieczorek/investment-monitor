package pl.marcin.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.scraping.PageFetcher
import pl.marcin.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Fetches the Gmina Buk BIP "Obwieszczenia i komunikaty" register.
 *
 * Requires [pl.marcin.investmentmonitor.scraping.PlaywrightPageFetcher] to
 * be enabled (`investment-monitor.playwright.enabled=true`, see ADR-007) -
 * `bip.buk.gmina.pl` is registered as a browser-required host in
 * [pl.marcin.investmentmonitor.registry.DiscoverySourceRegistry], so
 * [pl.marcin.investmentmonitor.scraping.ArchivingPageFetcher] routes
 * fetches for it there transparently; this class itself has no
 * Playwright-specific code and is unaffected either way.
 *
 * Like [SremWzSource], this register is split one page per calendar year -
 * see [BukObwieszczeniaParser] KDoc - hence the two-step fetch.
 */
@Component
class BukObwieszczeniaSource(
    private val pageFetcher: PageFetcher,
    private val parser: BukObwieszczeniaParser = BukObwieszczeniaParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = BukObwieszczeniaParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val indexHtml = pageFetcher.fetch(URI(INDEX_URL))
        val currentYearUrl = parser.findCurrentYearUrl(indexHtml, INDEX_URL) ?: return emptyList()
        val yearHtml = pageFetcher.fetch(URI(currentYearUrl))
        return parser.parse(yearHtml, currentYearUrl)
    }

    companion object {
        const val SOURCE_ID = "buk-obwieszczenia"
        const val INDEX_URL = "https://bip.buk.gmina.pl/m,1745,obwieszczenia-i-komunikaty.html"
    }
}
