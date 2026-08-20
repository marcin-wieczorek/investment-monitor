package pl.marcin.investmentmonitor.tools

import pl.marcin.investmentmonitor.scraping.JsoupPageFetcher
import pl.marcin.investmentmonitor.source.AgrobexSource
import pl.marcin.investmentmonitor.source.AreaSource
import pl.marcin.investmentmonitor.source.ATALSource
import pl.marcin.investmentmonitor.source.AtanerSource
import pl.marcin.investmentmonitor.source.ChronosSource
import pl.marcin.investmentmonitor.source.CordiaSource
import pl.marcin.investmentmonitor.source.DeveliaSource
import pl.marcin.investmentmonitor.source.DudaSource
import pl.marcin.investmentmonitor.source.EBFSource
import pl.marcin.investmentmonitor.source.GGWSource
import pl.marcin.investmentmonitor.source.GreenbudSource
import pl.marcin.investmentmonitor.source.InwestycjeWielkopolskiSource
import pl.marcin.investmentmonitor.source.JaksBudSource
import pl.marcin.investmentmonitor.source.JakonInwestSource
import pl.marcin.investmentmonitor.source.KonimpexSource
import pl.marcin.investmentmonitor.source.LineaSource
import pl.marcin.investmentmonitor.source.MJSource
import pl.marcin.investmentmonitor.source.MurapolSource
import pl.marcin.investmentmonitor.source.PekabexSource
import pl.marcin.investmentmonitor.source.RobygSource
import pl.marcin.investmentmonitor.source.RonsonSource
import pl.marcin.investmentmonitor.source.SagarisSource
import pl.marcin.investmentmonitor.source.SivanetSource
import pl.marcin.investmentmonitor.source.SpraviaSource
import pl.marcin.investmentmonitor.source.UWISource
import pl.marcin.investmentmonitor.source.VastbouwSource
import pl.marcin.investmentmonitor.source.aggregator.RynekPierwotnySource
import pl.marcin.investmentmonitor.source.discovery.CzerwonakObwieszczeniaSource
import pl.marcin.investmentmonitor.source.discovery.PoznanUlicpSource
import pl.marcin.investmentmonitor.source.discovery.SuchyLasNppSource
import pl.marcin.investmentmonitor.source.discovery.SwarzedzWzSource
import pl.marcin.investmentmonitor.source.discovery.TarnowoPodgorneWzSource
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
        "chronos" to (URI(ChronosSource.LIST_URL) to "investment-list.html"),
        "greenbud" to (URI(GreenbudSource.LIST_URL) to "investment-list.html"),
        "jakon-inwest" to (URI(JakonInwestSource.LIST_URL) to "investment-list.html"),
        "agrobex" to (URI(AgrobexSource.LIST_URL) to "investment-list.html"),
        "linea" to (URI(LineaSource.LIST_URL) to "investment-list.html"),
        "duda" to (URI(DudaSource.LIST_URL) to "investment-list.html"),
        "ataner" to (URI(AtanerSource.LIST_URL) to "investment-list.html"),
        "uwi" to (URI(UWISource.LIST_URL) to "investment-list.html"),
        "konimpex" to (URI(KonimpexSource.LIST_URL) to "investment-list.html"),
        "pekabex" to (URI(PekabexSource.LIST_URL) to "investment-list.html"),
        "murapol" to (URI(MurapolSource.LIST_URL) to "investment-list.html"),
        "develia" to (URI(DeveliaSource.LIST_URL) to "investment-list.html"),
        "atal" to (URI(ATALSource.LIST_URL) to "investment-list.html"),
        "robyg" to (URI(RobygSource.LIST_URL) to "investment-list.html"),
        "ebf" to (URI(EBFSource.LIST_URL) to "investment-list.html"),
        "ggw" to (URI(GGWSource.LIST_URL) to "investment-list.html"),
        "spravia" to (URI(SpraviaSource.LIST_URL) to "investment-list.html"),
        "jaksbud" to (URI(JaksBudSource.LIST_URL) to "investment-list.html"),
        "sagaris" to (URI(SagarisSource.LIST_URL) to "investment-list.html"),
        "cordia" to (URI(CordiaSource.LIST_URL) to "investment-list.html"),
        "ronson" to (URI(RonsonSource.LIST_URL) to "investment-list.html"),
        "sivanet" to (URI(SivanetSource.LIST_URL) to "investment-list.html"),
        "mj" to (URI(MJSource.LIST_URL) to "investment-list.html"),
        "area" to (URI(AreaSource.LIST_URL) to "investment-list.html"),
        "inwestycje_wielkopolski" to (URI(InwestycjeWielkopolskiSource.LIST_URL) to "investment-list.html"),
        "vastbouw" to (URI(VastbouwSource.LIST_URL) to "investment-list.html"),
        "swarzedz-wz" to (URI(SwarzedzWzSource.LIST_URL) to "warunki-zabudowy.html"),
        "czerwonak-obwieszczenia" to (URI(CzerwonakObwieszczeniaSource.LIST_URL) to "announcements.html"),
        "tarnowo-podgorne-wz" to (URI(TarnowoPodgorneWzSource.LIST_URL) to "announcements.html"),
        "suchy-las-npp" to (URI(SuchyLasNppSource.LIST_URL) to "announcements.html"),
        "poznan-ulicp" to (URI(PoznanUlicpSource.LIST_URL) to "announcements.html"),
        "rynekpierwotny" to (URI(RynekPierwotnySource.LIST_URL) to "nowe-domy-poznan.html")
    )
    val fixturesDir = Path.of("src/test/resources/fixtures")

    targets.forEach { (sourceId, target) -> capture(fetcher, fixturesDir, sourceId, target.first, target.second) }

    println("Review captured HTML before committing it as a fixture.")
}

private fun capture(fetcher: JsoupPageFetcher, fixturesDir: Path, sourceId: String, uri: URI, fileName: String) {
    val result = runCatching { fetcher.fetch(uri) }

    result.fold(
        onSuccess = { html ->
            val dir = fixturesDir.resolve(sourceId)
            Files.createDirectories(dir)
            val file = dir.resolve(fileName)
            Files.writeString(file, html)
            println("Captured $sourceId -> $file (${html.length} bytes) at ${Instant.now()}")
        },
        onFailure = { error ->
            println("Failed to capture $sourceId: ${error.message}")
        }
    )
}
