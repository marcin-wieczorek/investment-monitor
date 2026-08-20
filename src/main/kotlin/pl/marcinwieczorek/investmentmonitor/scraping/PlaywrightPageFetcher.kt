package pl.marcinwieczorek.investmentmonitor.scraping

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.net.URI

/**
 * Headless-browser [PageFetcher], for the subset of sources that return an
 * empty/shell HTML body via plain HTTP because their real content is
 * rendered client-side (JS SPA, React/Next.js, AJAX-hydrated listings -
 * see ADR-007 and docs/SOURCES.md "Investigated but not implemented").
 *
 * Disabled by default ([ConditionalOnProperty]) so a fresh checkout never
 * downloads or launches a Chromium binary unless explicitly opted into via
 * `investment-monitor.playwright.enabled=true` - consistent with how
 * [pl.marcinwieczorek.investmentmonitor.llm.OllamaInvestmentAnalyzer] is gated
 * (ADR-006). One [Browser] instance is reused across fetches; a fresh
 * [Page] is opened and closed per call to keep fetches isolated from each
 * other (no shared cookies/storage bleeding between sources).
 *
 * Not wired directly into any [pl.marcinwieczorek.investmentmonitor.source.InvestmentSource]
 * - see [ArchivingPageFetcher] for the transparent per-host routing that
 * decides when this fetcher is used instead of [JsoupPageFetcher].
 */
@Component
@ConditionalOnProperty("investment-monitor.playwright.enabled", havingValue = "true")
class PlaywrightPageFetcher(
    @param:Value("\${investment-monitor.playwright.timeout-ms:30000}") private val timeoutMs: Long
) : PageFetcher, AutoCloseable {

    private val playwright: Playwright = Playwright.create()
    private val browser: Browser = playwright.chromium().launch(
        BrowserType.LaunchOptions().setHeadless(true)
    )

    override fun fetch(uri: URI): String {
        val page = browser.newPage()
        return try {
            page.navigate(uri.toString(), Page.NavigateOptions().setTimeout(timeoutMs.toDouble()))
            page.waitForLoadState(LoadState.NETWORKIDLE, Page.WaitForLoadStateOptions().setTimeout(timeoutMs.toDouble()))
            page.content()
        } finally {
            page.close()
        }
    }

    override fun close() {
        browser.close()
        playwright.close()
    }
}
