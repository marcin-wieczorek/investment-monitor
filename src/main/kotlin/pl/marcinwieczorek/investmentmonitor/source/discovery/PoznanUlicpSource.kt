package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Discovery source for the City of Poznań's public-purpose siting
 * ("ustalenie lokalizacji inwestycji celu publicznego" - ULICP)
 * obwieszczenia register.
 *
 * Verified live at [LIST_URL] (custom BIP CMS, server-rendered). Poznań
 * also exposes this same data as an XML/JSON API
 * (`bip.poznan.pl/api-json/bip/news/-,c,8440/`) - a future revision could
 * prefer that over HTML parsing, but the HTML structure is stable and
 * already verified, so it is used here for parity with the other
 * discovery sources.
 */
@Component
class PoznanUlicpSource(
    private val pageFetcher: PageFetcher,
    private val parser: PoznanUlicpParser = PoznanUlicpParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = PoznanUlicpParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val SOURCE_ID = "poznan-ulicp"
        const val LIST_URL =
            "https://bip.poznan.pl/bip/news/obwieszczenia-dotyczace-postepowan-o-ustalenie-lokalizacji-inwestycji-celu-publicznego-19,c,8440/"
    }
}
