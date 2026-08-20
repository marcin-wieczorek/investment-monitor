package pl.marcin.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.scraping.PageFetcher
import pl.marcin.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Fetches the Gmina Śrem BIP "Obwieszczenia, komunikaty" (warunki
 * zabudowy announcements) register.
 *
 * Unlike every other discovery source implemented so far, this register is
 * split into one page per calendar year rather than a single evergreen
 * feed - see [SremWzParser] KDoc. This source therefore does a two-step
 * fetch: the index page (to find the current year's URL) then that year's
 * page (for the actual announcements), rather than a single hardcoded
 * [LIST_URL] that would silently start returning nothing every January.
 */
@Component
class SremWzSource(
    private val pageFetcher: PageFetcher,
    private val parser: SremWzParser = SremWzParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = SremWzParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val indexHtml = pageFetcher.fetch(URI(INDEX_URL))
        val currentYearUrl = parser.findCurrentYearUrl(indexHtml, INDEX_URL) ?: return emptyList()
        val yearHtml = pageFetcher.fetch(URI(currentYearUrl))
        return parser.parse(yearHtml, currentYearUrl)
    }

    companion object {
        const val SOURCE_ID = "srem-wz"
        const val INDEX_URL = "http://bip.srem.pl/public/?id=73563"
    }
}
