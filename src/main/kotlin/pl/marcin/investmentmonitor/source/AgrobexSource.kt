package pl.marcin.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.scraping.PageFetcher
import java.net.URI

@Component
class AgrobexSource(
    private val pageFetcher: PageFetcher,
    private val parser: AgrobexParser = AgrobexParser()
) : InvestmentSource {

    override val id: String = AgrobexParser.SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val LIST_URL = "https://www.agrobex.pl"
    }
}
