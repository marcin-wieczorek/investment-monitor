package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Discovery source for Gmina Tarnowo Podgórne's BIP "Ogłoszenia ws.
 * decyzji lokalizacyjnych" register.
 *
 * Verified live at [LIST_URL] (same Rekord BIP CMS as
 * [CzerwonakObwieszczeniaSource] - see [RekordBipParser]).
 */
@Component
class TarnowoPodgorneWzSource(
    private val pageFetcher: PageFetcher,
    private val parser: RekordBipParser = RekordBipParser(MUNICIPALITY, SOURCE_ID)
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html, LIST_URL)
    }

    companion object {
        const val SOURCE_ID = "tarnowo-podgorne-wz"
        const val MUNICIPALITY = "Tarnowo Podgórne"
        const val LIST_URL = "http://bip2.tarnowo-podgorne.pl/6037"
    }
}
