package pl.marcin.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.scraping.PageFetcher
import java.net.URI

@Component
class SagarisSource(
    private val pageFetcher: PageFetcher,
    private val parser: SagarisParser = SagarisParser()
) : InvestmentSource {

    override val id: String = SagarisParser.SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val LIST_URL = "https://sagaris.pl/inwestycje/poznan/"
    }
}
