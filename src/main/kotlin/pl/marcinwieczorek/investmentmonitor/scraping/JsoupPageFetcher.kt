package pl.marcinwieczorek.investmentmonitor.scraping

import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI

@Component
class JsoupPageFetcher(
    @param:Value("\${investment-monitor.jsoup.timeout-ms:30000}") private val timeoutMs: Int = 30_000
) : PageFetcher {
    override fun fetch(uri: URI): String = Jsoup.connect(uri.toString())
        .userAgent("Mozilla/5.0 (compatible; InvestmentMonitor/0.1)")
        .timeout(timeoutMs)
        .get()
        .outerHtml()
}
