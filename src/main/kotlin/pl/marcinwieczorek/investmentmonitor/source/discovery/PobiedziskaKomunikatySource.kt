package pl.marcinwieczorek.investmentmonitor.source.discovery

import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.source.DiscoverySource
import java.net.URI

/**
 * Fetches the Gmina Pobiedziska BIP "Komunikaty" (planning announcements)
 * register.
 *
 * Requires [pl.marcinwieczorek.investmentmonitor.scraping.PlaywrightPageFetcher] to
 * be enabled (see ADR-007) - `bip.pobiedziska.pl` is registered as a
 * browser-required host in
 * [pl.marcinwieczorek.investmentmonitor.registry.DiscoverySourceRegistry].
 *
 * A single-page fetch, unlike [BukObwieszczeniaSource] (year-split) or
 * [SzamotulyUlicpSource] (per-announcement detail fetch) - see
 * [PobiedziskaKomunikatyParser] KDoc for why. The register currently fits
 * on one page (no pagination observed); if it grows past the platform's
 * default page size in the future, this would need the same
 * "find current period" indirection Śrem/Buk already use.
 */
@Component
class PobiedziskaKomunikatySource(
    private val pageFetcher: PageFetcher,
    private val parser: PobiedziskaKomunikatyParser = PobiedziskaKomunikatyParser()
) : DiscoverySource {

    override val id: String = SOURCE_ID
    override val municipality: String = PobiedziskaKomunikatyParser.MUNICIPALITY

    override fun fetch(): List<InvestmentSignal> {
        val html = pageFetcher.fetch(URI(LIST_URL))
        return parser.parse(html, LIST_URL)
    }

    companion object {
        const val SOURCE_ID = "pobiedziska-komunikaty"
        const val LIST_URL = "https://bip.pobiedziska.pl/m,150,komunikaty.html"
    }
}
