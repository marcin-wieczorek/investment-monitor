package pl.marcin.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.scraping.PageFetcher
import java.net.URI

/**
 * Fetches Nickel Development's apartment search results (see [NickelParser]
 * KDoc for why this page, not the homepage/listing pages, is the real
 * source of data).
 *
 * Plain `JsoupPageFetcher` works fine here - no `PlaywrightPageFetcher`/
 * ADR-007 involved, unlike every other developer added after the initial
 * "Investigated but not implemented" pass. Results are paginated; this
 * fetches page 1 first to discover the last page number and the
 * `id_loc[]` investment-URL mapping, then every remaining page, before
 * aggregating.
 */
@Component
class NickelSource(
    private val pageFetcher: PageFetcher,
    private val parser: NickelParser = NickelParser()
) : InvestmentSource {

    override val id: String = SOURCE_ID

    override fun fetch(): List<Investment> {
        val firstPageHtml = pageFetcher.fetch(URI(LIST_URL))
        val lastPage = parser.findLastPage(firstPageHtml)
        val investmentUrls = parser.findInvestmentUrls(firstPageHtml, LIST_URL)

        val allUnits = parser.parseUnits(firstPageHtml) +
            (2..lastPage).flatMap { page -> parser.parseUnits(pageFetcher.fetch(URI("$LIST_URL/p/$page"))) }

        return parser.aggregate(allUnits, investmentUrls)
    }

    companion object {
        const val SOURCE_ID = "nickel"
        const val LIST_URL = "https://nickel.com.pl/pl/wyszukiwarka-mieszkan"
    }
}
