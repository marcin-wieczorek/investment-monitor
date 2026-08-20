package pl.marcin.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.scraping.PageFetcher
import java.net.URI

@Component
class KonimpexSource(
    private val pageFetcher: PageFetcher,
    private val parser: KonimpexParser = KonimpexParser()
) : InvestmentSource {

    override val id: String = KonimpexParser.SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val LIST_URL = "https://www.konimpex-invest.pl/pl/inwestycje-w-sprzedazy"
    }
}
