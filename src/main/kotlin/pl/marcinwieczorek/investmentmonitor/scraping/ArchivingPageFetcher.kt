package pl.marcinwieczorek.investmentmonitor.scraping

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.archival.RawHtmlArchiver
import java.net.URI

/**
 * Decorates [JsoupPageFetcher] to transparently archive every fetched page
 * (see docs/ARCHITECTURE.md raw source archival section), without any
 * source or parser needing to know archival exists.
 *
 * Also transparently routes fetches for hosts in [browserRequiredHosts] to
 * [PlaywrightPageFetcher] instead of [JsoupPageFetcher] (see ADR-007) when
 * that fetcher is enabled - again without any `*Source`/`*Parser` needing
 * to know or care how its HTML was retrieved. Falls back to
 * [JsoupPageFetcher] whenever Playwright is disabled
 * (`investment-monitor.playwright.enabled=false`, the default) or the
 * host isn't flagged, so this decorator behaves identically to before
 * ADR-007 for the ~20 sources that don't need a browser.
 *
 * Every fetch - regardless of which underlying fetcher is selected - is
 * additionally wrapped in [RetryingPageFetcher] so a single transient
 * network failure doesn't drop an entire source for the whole scan.
 *
 * Archived under the URL's host (e.g. `chronos.poznan.pl`, `tercja.eu`) -
 * a coarse but always-available "what did this look like" key, since
 * [PageFetcher] callers (sources and detail parsers alike) never expose a
 * logical source id at fetch time.
 *
 * Marked [Primary] so every existing constructor that depends on the
 * [PageFetcher] interface keeps working unchanged and automatically gets
 * archival; [JsoupPageFetcher] itself is injected here by its concrete
 * type, bypassing the interface ambiguity this would otherwise cause.
 */
@Primary
@Component
class ArchivingPageFetcher(
    private val delegate: JsoupPageFetcher,
    private val archiver: RawHtmlArchiver,
    private val playwrightFetcher: PlaywrightPageFetcher? = null,
    private val browserRequiredHosts: Set<String> = emptySet()
) : PageFetcher {

    override fun fetch(uri: URI): String {
        val fetcher = if (playwrightFetcher != null && uri.host in browserRequiredHosts) {
            playwrightFetcher
        } else {
            delegate
        }
        val html = RetryingPageFetcher(fetcher).fetch(uri)
        archiver.archive(uri.host ?: "unknown-host", html)
        return html
    }
}
