package pl.marcin.investmentmonitor.scraping

import java.net.URI

fun interface PageFetcher {
    fun fetch(uri: URI): String
}
