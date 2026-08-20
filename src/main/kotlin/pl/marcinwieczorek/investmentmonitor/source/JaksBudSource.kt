package pl.marcinwieczorek.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import java.net.URI

@Component
class JaksBudSource(
    private val pageFetcher: PageFetcher,
    private val parser: JaksBudParser = JaksBudParser()
) : InvestmentSource {

    override val id: String = JaksBudParser.SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val LIST_URL = "https://jaksbud.pl/znajdz-mieszkanie/"
    }
}
