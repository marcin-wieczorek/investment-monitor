package pl.marcinwieczorek.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import java.net.URI

/**
 * Fetches Archicom's Poznań investment listing.
 *
 * Requires [pl.marcinwieczorek.investmentmonitor.scraping.PlaywrightPageFetcher] to
 * be enabled (see ADR-007) - `archicom.pl` is registered as a
 * browser-required host in
 * [pl.marcinwieczorek.investmentmonitor.registry.DeveloperRegistry].
 */
@Component
class ArchicomSource(
    private val pageFetcher: PageFetcher,
    private val parser: ArchicomParser = ArchicomParser()
) : InvestmentSource {

    override val id: String = SOURCE_ID

    override fun fetch(): List<Investment> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html)
    }

    companion object {
        const val SOURCE_ID = "archicom"
        const val LIST_URL = "https://archicom.pl/poznan"
    }
}
