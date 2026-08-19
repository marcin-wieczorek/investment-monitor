package pl.marcin.investmentmonitor.tools

import pl.marcin.investmentmonitor.scraping.JsoupPageFetcher
import pl.marcin.investmentmonitor.source.ChronosSource
import pl.marcin.investmentmonitor.source.GreenbudSource
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

/**
 * Entry point for deliberate fixture capture.
 *
 * Captured HTML becomes test input and must be reviewed before committing.
 */
fun main() {
    val fetcher = JsoupPageFetcher()
    val targets = mapOf(
        "chronos" to URI(ChronosSource.LIST_URL),
        "greenbud" to URI(GreenbudSource.LIST_URL)
    )
    val fixturesDir = Path.of("src/test/resources/fixtures")

    targets.forEach { (sourceId, uri) -> capture(fetcher, fixturesDir, sourceId, uri) }

    println("Review captured HTML before committing it as a fixture.")
}

private fun capture(fetcher: JsoupPageFetcher, fixturesDir: Path, sourceId: String, uri: URI) {
    val result = runCatching { fetcher.fetch(uri) }

    result.fold(
        onSuccess = { html ->
            val dir = fixturesDir.resolve(sourceId)
            Files.createDirectories(dir)
            val file = dir.resolve("investment-list.html")
            Files.writeString(file, html)
            println("Captured $sourceId -> $file (${html.length} bytes) at ${Instant.now()}")
        },
        onFailure = { error ->
            println("Failed to capture $sourceId: ${error.message}")
        }
    )
}
