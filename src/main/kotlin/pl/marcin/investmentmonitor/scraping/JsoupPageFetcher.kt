package pl.marcin.investmentmonitor.scraping

import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.net.URI

@Component
class JsoupPageFetcher : PageFetcher {
    override fun fetch(uri: URI): String = Jsoup.connect(uri.toString())
        .userAgent("Mozilla/5.0 (compatible; InvestmentMonitor/0.1)")
        .timeout(30_000)
        .get()
        .outerHtml()
}
