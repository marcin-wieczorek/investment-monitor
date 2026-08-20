package pl.marcinwieczorek.investmentmonitor.source.aggregator

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.source.AggregatorSource
import java.net.URI

@Component
class RynekPierwotnySource(
    private val pageFetcher: PageFetcher,
    private val parser: RynekPierwotnyParser = RynekPierwotnyParser()
) : AggregatorSource {

    override val id: String = SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val SOURCE_ID = "rynekpierwotny"
        const val LIST_URL = "https://www.rynekpierwotny.pl/s/nowe-domy-poznan/"
    }
}
