package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Discovery source for Gmina Czerwonak's BIP "Obwieszczenia" register.
 *
 * Verified live at [LIST_URL] (Rekord BIP CMS, server-rendered, no
 * JavaScript required) - see [RekordBipParser] for the shared markup this
 * source has in common with [TarnowoPodgorneWzSource].
 */
@Component
class CzerwonakObwieszczeniaSource(
    private val pageFetcher: PageFetcher,
    private val parser: RekordBipParser = RekordBipParser(MUNICIPALITY, SourceId(SOURCE_ID))
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html, LIST_URL)
    }

    companion object {
        const val SOURCE_ID = "czerwonak-obwieszczenia"
        const val MUNICIPALITY = "Czerwonak"
        const val LIST_URL = "https://bip.czerwonak.pl/6469"
    }
}
