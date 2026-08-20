package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import java.net.URI

@Component
class MurowanaGoslinaObwieszczeniaSource(
    private val pageFetcher: PageFetcher,
    private val parser: MurowanaGoslinaObwieszczeniaParser = MurowanaGoslinaObwieszczeniaParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = MurowanaGoslinaObwieszczeniaParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html, LIST_URL)
    }

    companion object {
        const val SOURCE_ID = "murowana-goslina-obwieszczenia"
        const val LIST_URL = "https://bip.murowana-goslina.pl/wiadomosci/9179/lista/1/obwieszczenia_inne"
    }
}
