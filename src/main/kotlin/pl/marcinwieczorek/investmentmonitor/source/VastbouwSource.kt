package pl.marcinwieczorek.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import java.net.URI

@Component
class VastbouwSource(
    private val pageFetcher: PageFetcher,
    private val parser: VastbouwParser = VastbouwParser()
) : InvestmentSource {

    override val id: String = VastbouwParser.SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val LIST_URL = "https://vastbouw.pl/inwestycje/mieszkania-domy-poznan/"
    }
}
