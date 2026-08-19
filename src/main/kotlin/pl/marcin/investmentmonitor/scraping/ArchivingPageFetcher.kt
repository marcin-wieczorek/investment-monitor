package pl.marcin.investmentmonitor.scraping

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.archival.RawHtmlArchiver
import java.net.URI

/**
 * Decorates [JsoupPageFetcher] to transparently archive every fetched page
 * (see docs/ARCHITECTURE.md raw source archival section), without any
 * source or parser needing to know archival exists.
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
    private val archiver: RawHtmlArchiver
) : PageFetcher {

    override fun fetch(uri: URI): String {
        val html = delegate.fetch(uri)
        archiver.archive(uri.host ?: "unknown-host", html)
        return html
    }
}
