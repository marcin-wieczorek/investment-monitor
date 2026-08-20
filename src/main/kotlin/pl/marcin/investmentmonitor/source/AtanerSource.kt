package pl.marcin.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.scraping.PageFetcher
import java.net.URI

@Component
class AtanerSource(
    private val pageFetcher: PageFetcher,
    private val parser: AtanerParser = AtanerParser()
) : InvestmentSource {

    override val id: String = AtanerParser.SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val LIST_URL = "https://www.ataner.pl/pl/mieszkania-start"
    }
}
