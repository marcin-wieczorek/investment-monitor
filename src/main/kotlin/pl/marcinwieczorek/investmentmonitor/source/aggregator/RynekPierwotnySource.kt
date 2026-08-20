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

    /**
     * Fetches every page of the listing (`?page=N`) until a page yields no
     * results, or yields only offers already seen, or [MAX_PAGES] is hit -
     * the "od 4" (4+ rooms) filter's own page reports ~30 total offers at
     * 10 per page, more than a single fetch would cover.
     *
     * In practice `?page=N` has been observed to behave inconsistently on
     * this site for this particular pretty-URL route (an empty page 2
     * followed by page 3 silently repeating page 1's content) - both stop
     * conditions above exist specifically to make that safe: an aggregator
     * fetch here is a best-effort completeness pass, never worth retrying
     * or treating as a hard failure over.
     */
    override fun fetch(): List<Investment> {
        val seen = LinkedHashMap<String, Investment>()
        for (page in 1..MAX_PAGES) {
            val url = if (page == 1) LIST_URL else "$LIST_URL?page=$page"
            val html = pageFetcher.fetch(URI(url))
            val parsed = parser.parse(html)
            if (parsed.isEmpty()) break

            val newOnes = parsed.filterNot { seen.containsKey(it.canonicalKey) }
            if (newOnes.isEmpty()) break // site clamped to the last page - stop rather than loop

            newOnes.forEach { seen[it.canonicalKey] = it }
        }
        return seen.values.toList()
    }

    companion object {
        const val SOURCE_ID = "rynekpierwotny"

        /**
         * Filters to houses with 4+ rooms across the whole Wielkopolskie
         * voivodeship (broader than the Poznań metro area in scope, but
         * out-of-scope results are harmless - they just surface with their
         * raw location and are never treated as within-scope for
         * correlation/scoring purposes).
         */
        const val LIST_URL = "https://rynekpierwotny.pl/s/nowe-domy-wielkopolskie-liczba-pokoi-od-4/"
        private const val MAX_PAGES = 5
    }
}
