package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Fetches the Gmina Kórnik BIP "Obwieszczenia i ogłoszenia" register.
 *
 * Requires [pl.marcinwieczorek.investmentmonitor.scraping.PlaywrightPageFetcher] to
 * be enabled (see ADR-007) - `bip.kornik.pl` is registered as a
 * browser-required host in
 * [pl.marcinwieczorek.investmentmonitor.registry.DiscoverySourceRegistry].
 *
 * Like [SremWzSource]/[BukObwieszczeniaSource], this register is split
 * one page per calendar year - see [KornikObwieszczeniaParser] KDoc -
 * hence the two-step fetch.
 */
@Component
class KornikObwieszczeniaSource(
    private val pageFetcher: PageFetcher,
    private val parser: KornikObwieszczeniaParser = KornikObwieszczeniaParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = KornikObwieszczeniaParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val indexHtml = pageFetcher.fetch(URI(INDEX_URL))
        val currentYearUrl = parser.findCurrentYearUrl(indexHtml, INDEX_URL) ?: return emptyList()
        val yearHtml = pageFetcher.fetch(URI(currentYearUrl))
        return parser.parse(yearHtml, currentYearUrl)
    }

    companion object {
        const val SOURCE_ID = "kornik-obwieszczenia"
        const val INDEX_URL = "https://bip.kornik.pl/obwieszczenia-i-ogloszenia"
    }
}
