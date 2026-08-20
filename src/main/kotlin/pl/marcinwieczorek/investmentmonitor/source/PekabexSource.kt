package pl.marcinwieczorek.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import java.net.URI

@Component
class PekabexSource(
    private val pageFetcher: PageFetcher,
    private val parser: PekabexParser = PekabexParser()
) : InvestmentSource {

    override val id: String = PekabexParser.SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val LIST_URL = "https://pekabexdevelopment.com"
    }
}
