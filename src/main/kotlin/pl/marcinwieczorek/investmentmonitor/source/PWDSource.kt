package pl.marcinwieczorek.investmentmonitor.source

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import java.net.URI

/**
 * Fetches PWD Deweloper's "Osiedle Zagajnik" stage pages.
 *
 * Requires [pl.marcinwieczorek.investmentmonitor.scraping.PlaywrightPageFetcher] to
 * be enabled (see ADR-007) - `pwd-mieszkania.pl` is registered as a
 * browser-required host in
 * [pl.marcinwieczorek.investmentmonitor.registry.DeveloperRegistry].
 *
 * Unlike every other developer source, this fetches two separate,
 * hardcoded stage-page URLs rather than one list page - see [PWDParser]
 * KDoc for why each stage is its own [Investment]. A third stage
 * ("III Etap w przygotowaniu") is announced on-site but not yet built out
 * with its own page/units - not implemented since there is nothing to
 * verify a parser against yet (see AGENTS.md "no fake implementations");
 * add its URL here once it goes live.
 */
@Component
class PWDSource(
    private val pageFetcher: PageFetcher,
    private val parser: PWDParser = PWDParser()
) : InvestmentSource {

    override val id: String = SOURCE_ID

    override fun fetch(): List<Investment> =
        STAGE_URLS.flatMap { url ->
            runCatching { parser.parse(pageFetcher.fetch(URI(url)), url) }.getOrDefault(emptyList())
        }

    companion object {
        const val SOURCE_ID = "pwd"
        const val STAGE_1_URL = "https://pwd-mieszkania.pl/osiedle_zagajnik_e1/"
        const val STAGE_2_URL = "https://pwd-mieszkania.pl/osiedle_zagajnik_e2/"
        val STAGE_URLS = listOf(STAGE_1_URL, STAGE_2_URL)
    }
}
