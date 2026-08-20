package pl.marcinwieczorek.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import java.net.URI

@Component
class SpraviaSource(
    private val pageFetcher: PageFetcher,
    private val parser: SpraviaParser = SpraviaParser()
) : InvestmentSource {

    override val id: String = SpraviaParser.SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val LIST_URL = "https://spravia.pl/inwestycje/poznan/"
    }
}
