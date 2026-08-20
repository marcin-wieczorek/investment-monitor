package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import java.net.URI

@Component
class SwarzedzWzSource(
    private val pageFetcher: PageFetcher,
    private val parser: SwarzedzWzParser = SwarzedzWzParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = SwarzedzWzParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val SOURCE_ID = "swarzedz-wz"
        const val LIST_URL = "https://bip.swarzedz.pl/index.php?id=344"
    }
}
