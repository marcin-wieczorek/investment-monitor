package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Discovery source for Gmina Suchy Las's BIP planning-announcement
 * register.
 *
 * Verified live at [LIST_URL] (Logonet CMS, server-rendered). Note this is
 * the "Obwieszczenia NPP" (MPZP-related) category rather than the
 * differently-named "lokalizacja inwestycji celu publicznego i decyzji o
 * warunkach zabudowy" page, which was found empty of any announcements at
 * verification time - see [SuchyLasNppParser] for details.
 */
@Component
class SuchyLasNppSource(
    private val pageFetcher: PageFetcher,
    private val parser: SuchyLasNppParser = SuchyLasNppParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = SuchyLasNppParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val SOURCE_ID = "suchy-las-npp"
        const val LIST_URL = "https://bip.suchylas.pl/artykuly/planowanie-i-zagospodarowanie-przestrzenne-obwieszczenia-npp"
    }
}
