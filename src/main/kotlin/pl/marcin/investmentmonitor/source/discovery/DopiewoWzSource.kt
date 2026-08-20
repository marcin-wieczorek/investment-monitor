package pl.marcin.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.scraping.PageFetcher
import pl.marcin.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Fetches the Gmina Dopiewo BIP "Decyzje o warunkach zabudowy" register.
 *
 * Requires [pl.marcin.investmentmonitor.scraping.PlaywrightPageFetcher] to
 * be enabled (see ADR-007) - `bip.dopiewo.pl` is registered as a
 * browser-required host in
 * [pl.marcin.investmentmonitor.registry.DiscoverySourceRegistry].
 *
 * Like [SremWzSource]/[BukObwieszczeniaSource]/[KornikObwieszczeniaSource],
 * this register is split one page per calendar year - see
 * [DopiewoWzParser] KDoc - hence the two-step fetch.
 */
@Component
class DopiewoWzSource(
    private val pageFetcher: PageFetcher,
    private val parser: DopiewoWzParser = DopiewoWzParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = DopiewoWzParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val indexHtml = pageFetcher.fetch(URI(INDEX_URL))
        val currentYearUrl = parser.findCurrentYearUrl(indexHtml, INDEX_URL) ?: return emptyList()
        val yearHtml = pageFetcher.fetch(URI(currentYearUrl))
        return parser.parse(yearHtml, currentYearUrl)
    }

    companion object {
        const val SOURCE_ID = "dopiewo-wz"
        const val INDEX_URL = "https://bip.dopiewo.pl/kategorie/125-decyzje-o-warunkach-zabudowy"
    }
}
