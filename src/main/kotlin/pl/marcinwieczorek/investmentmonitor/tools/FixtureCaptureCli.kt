package pl.marcinwieczorek.investmentmonitor.tools

import pl.marcinwieczorek.investmentmonitor.scraping.JsoupPageFetcher
import pl.marcinwieczorek.investmentmonitor.scraping.PageFetcher
import pl.marcinwieczorek.investmentmonitor.scraping.PlaywrightPageFetcher
import pl.marcinwieczorek.investmentmonitor.source.AgrobexSource
import pl.marcinwieczorek.investmentmonitor.source.ArchicomSource
import pl.marcinwieczorek.investmentmonitor.source.AreaSource
import pl.marcinwieczorek.investmentmonitor.source.ATALSource
import pl.marcinwieczorek.investmentmonitor.source.AtanerSource
import pl.marcinwieczorek.investmentmonitor.source.ChronosSource
import pl.marcinwieczorek.investmentmonitor.source.CordiaSource
import pl.marcinwieczorek.investmentmonitor.source.DeveliaSource
import pl.marcinwieczorek.investmentmonitor.source.DudaSource
import pl.marcinwieczorek.investmentmonitor.source.EBFSource
import pl.marcinwieczorek.investmentmonitor.source.GGWSource
import pl.marcinwieczorek.investmentmonitor.source.GreenbudSource
import pl.marcinwieczorek.investmentmonitor.source.InwestycjeWielkopolskiSource
import pl.marcinwieczorek.investmentmonitor.source.JaksBudSource
import pl.marcinwieczorek.investmentmonitor.source.JakonInwestSource
import pl.marcinwieczorek.investmentmonitor.source.KonimpexSource
import pl.marcinwieczorek.investmentmonitor.source.LineaSource
import pl.marcinwieczorek.investmentmonitor.source.MJSource
import pl.marcinwieczorek.investmentmonitor.source.MurapolSource
import pl.marcinwieczorek.investmentmonitor.source.NickelParser
import pl.marcinwieczorek.investmentmonitor.source.NickelSource
import pl.marcinwieczorek.investmentmonitor.source.PWDSource
import pl.marcinwieczorek.investmentmonitor.source.PekabexSource
import pl.marcinwieczorek.investmentmonitor.source.RobygSource
import pl.marcinwieczorek.investmentmonitor.source.RonsonSource
import pl.marcinwieczorek.investmentmonitor.source.SagarisSource
import pl.marcinwieczorek.investmentmonitor.source.SivanetSource
import pl.marcinwieczorek.investmentmonitor.source.SpraviaSource
import pl.marcinwieczorek.investmentmonitor.source.UWISource
import pl.marcinwieczorek.investmentmonitor.source.VastbouwSource
import pl.marcinwieczorek.investmentmonitor.source.aggregator.RynekPierwotnySource
import pl.marcinwieczorek.investmentmonitor.source.discovery.BukObwieszczeniaParser
import pl.marcinwieczorek.investmentmonitor.source.discovery.BukObwieszczeniaSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.CzerwonakObwieszczeniaSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.DopiewoWzParser
import pl.marcinwieczorek.investmentmonitor.source.discovery.DopiewoWzSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.KornikObwieszczeniaParser
import pl.marcinwieczorek.investmentmonitor.source.discovery.KornikObwieszczeniaSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.MurowanaGoslinaObwieszczeniaSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.PobiedziskaKomunikatySource
import pl.marcinwieczorek.investmentmonitor.source.discovery.PoznanUlicpSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.SremWzSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.SuchyLasNppSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.SwarzedzWzSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.SzamotulyUlicpParser
import pl.marcinwieczorek.investmentmonitor.source.discovery.SzamotulyUlicpSource
import pl.marcinwieczorek.investmentmonitor.source.discovery.TarnowoPodgorneWzSource
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
        // Śrem's register is split one page per year (see SremWzParser KDoc) -
        // capture both the index and the current year's page; the year id
        // in this URL will need manual updating once a year.
        "srem-wz" to (URI(SremWzSource.INDEX_URL) to "index.html"),
        "murowana-goslina-obwieszczenia" to (URI(MurowanaGoslinaObwieszczeniaSource.LIST_URL) to "announcements.html"),
        "rynekpierwotny" to (URI(RynekPierwotnySource.LIST_URL) to "nowe-domy-wielkopolskie-liczba-pokoi-od-4.html")
    )
    val fixturesDir = Path.of("src/test/resources/fixtures")

    targets.forEach { (sourceId, target) -> capture(fetcher, fixturesDir, sourceId, target.first, target.second) }
    captureNickel(fetcher, fixturesDir)
    captureBrowserRequiredFixtures(fixturesDir)

    println("Review captured HTML before committing it as a fixture.")
}

/**
 * Every source requiring [PlaywrightPageFetcher] (see ADR-007) shares one
 * browser instance for this capture run rather than each launching its
 * own - requires `investment-monitor.playwright` setup (`npx playwright
 * install chromium`, see README.md).
 */
private fun captureBrowserRequiredFixtures(fixturesDir: Path) {
    val playwright = PlaywrightPageFetcher(30_000)
    try {
        captureBukObwieszczenia(playwright, fixturesDir)
        captureSzamotulyUlicp(playwright, fixturesDir)
        capture(playwright, fixturesDir, "pobiedziska-komunikaty", URI(PobiedziskaKomunikatySource.LIST_URL), "announcements.html")
        capture(playwright, fixturesDir, "archicom", URI(ArchicomSource.LIST_URL), "investment-list.html")
        capturePWD(playwright, fixturesDir)
        captureYearSplitDiscovery(
            playwright, fixturesDir, "kornik-obwieszczenia", KornikObwieszczeniaSource.INDEX_URL,
            KornikObwieszczeniaParser()::findCurrentYearUrl
        )
        captureYearSplitDiscovery(
            playwright, fixturesDir, "dopiewo-wz", DopiewoWzSource.INDEX_URL,
            DopiewoWzParser()::findCurrentYearUrl
        )
    } finally {
        playwright.close()
    }
}

/**
 * Buk's BIP (see ADR-007, [BukObwieszczeniaSource]) only renders content
 * client-side, so its fixtures must be captured with
 * [PlaywrightPageFetcher] rather than the plain [JsoupPageFetcher] used
 * for every other source above - requires `investment-monitor.playwright`
 * setup (`npx playwright install chromium`, see README.md). Also a
 * two-step capture (index page, then the current year's article) since
 * the register is split one page per calendar year - the year in
 * `year-<yyyy>.html` will need manual updating once a year, same as
 * `srem-wz`.
 */
private fun captureBukObwieszczenia(playwright: PlaywrightPageFetcher, fixturesDir: Path) {
    val dir = fixturesDir.resolve("buk-obwieszczenia")
    val indexResult = runCatching { playwright.fetch(URI(BukObwieszczeniaSource.INDEX_URL)) }
    indexResult.fold(
        onSuccess = { indexHtml ->
            Files.createDirectories(dir)
            Files.writeString(dir.resolve("index.html"), indexHtml)
            println("Captured buk-obwieszczenia (index) -> ${dir.resolve("index.html")} (${indexHtml.length} bytes) at ${Instant.now()}")

            val parser = BukObwieszczeniaParser()
            val yearUrl = parser.findCurrentYearUrl(indexHtml, BukObwieszczeniaSource.INDEX_URL)
            if (yearUrl == null) {
                println("Failed to capture buk-obwieszczenia (year page): no year link found in index")
                return@fold
            }
            runCatching { playwright.fetch(URI(yearUrl)) }.fold(
                onSuccess = { yearHtml ->
                    val yearFile = dir.resolve("year-${YEAR_IN_URL.find(yearUrl)?.groupValues?.get(1) ?: "current"}.html")
                    Files.writeString(yearFile, yearHtml)
                    println("Captured buk-obwieszczenia (year) -> $yearFile (${yearHtml.length} bytes) at ${Instant.now()}")
                },
                onFailure = { error -> println("Failed to capture buk-obwieszczenia (year page): ${error.message}") }
            )
        },
        onFailure = { error -> println("Failed to capture buk-obwieszczenia (index): ${error.message}") }
    )
}

/**
 * Szamotuły's register (see [SzamotulyUlicpParser] KDoc) needs the list
 * page plus every announcement's own article page - captures the list,
 * then every article it links to, numbering fixture files in list order
 * (`article-1.html`, `article-2.html`, ...) since article ids aren't
 * meaningful test identifiers on their own.
 */
private fun captureSzamotulyUlicp(playwright: PlaywrightPageFetcher, fixturesDir: Path) {
    val dir = fixturesDir.resolve("szamotuly-ulicp")
    val listResult = runCatching { playwright.fetch(URI(SzamotulyUlicpSource.LIST_URL)) }
    listResult.fold(
        onSuccess = { listHtml ->
            Files.createDirectories(dir)
            Files.writeString(dir.resolve("list.html"), listHtml)
            println("Captured szamotuly-ulicp (list) -> ${dir.resolve("list.html")} (${listHtml.length} bytes) at ${Instant.now()}")

            val parser = SzamotulyUlicpParser()
            val articleUrls = parser.findArticleUrls(listHtml, SzamotulyUlicpSource.LIST_URL)
            articleUrls.forEachIndexed { index, url ->
                runCatching { playwright.fetch(URI(url)) }.fold(
                    onSuccess = { articleHtml ->
                        val file = dir.resolve("article-${index + 1}.html")
                        Files.writeString(file, articleHtml)
                        println("Captured szamotuly-ulicp (article ${index + 1}) -> $file (${articleHtml.length} bytes) at ${Instant.now()}")
                    },
                    onFailure = { error -> println("Failed to capture szamotuly-ulicp article $url: ${error.message}") }
                )
            }
        },
        onFailure = { error -> println("Failed to capture szamotuly-ulicp (list): ${error.message}") }
    )
}

private val YEAR_IN_URL = Regex("(\\d{4})-rok")

/**
 * PWD Deweloper (see [PWDSource] KDoc) publishes each stage as its own
 * static URL - captures both directly, no discovery step needed.
 */
private fun capturePWD(playwright: PlaywrightPageFetcher, fixturesDir: Path) {
    val dir = fixturesDir.resolve("pwd")
    Files.createDirectories(dir)
    listOf(PWDSource.STAGE_1_URL to "etap-1.html", PWDSource.STAGE_2_URL to "etap-2.html").forEach { (url, fileName) ->
        runCatching { playwright.fetch(URI(url)) }.fold(
            onSuccess = { html ->
                val file = dir.resolve(fileName)
                Files.writeString(file, html)
                println("Captured pwd ($fileName) -> $file (${html.length} bytes) at ${Instant.now()}")
            },
            onFailure = { error -> println("Failed to capture pwd ($fileName): ${error.message}") }
        )
    }
}

/**
 * Nickel's search results (see [NickelParser]/[NickelSource] KDoc) are
 * paginated - captures page 1, discovers the last page number from it,
 * then captures every remaining page as `page-<n>.html`. Uses plain
 * [JsoupPageFetcher], not Playwright - this source is fully
 * server-rendered.
 */
private fun captureNickel(fetcher: JsoupPageFetcher, fixturesDir: Path) {
    val dir = fixturesDir.resolve("nickel")
    val firstPageResult = runCatching { fetcher.fetch(URI(NickelSource.LIST_URL)) }
    firstPageResult.fold(
        onSuccess = { firstPageHtml ->
            Files.createDirectories(dir)
            Files.writeString(dir.resolve("page-1.html"), firstPageHtml)
            println("Captured nickel (page 1) -> ${dir.resolve("page-1.html")} (${firstPageHtml.length} bytes) at ${Instant.now()}")

            val lastPage = NickelParser().findLastPage(firstPageHtml)
            (2..lastPage).forEach { page ->
                runCatching { fetcher.fetch(URI("${NickelSource.LIST_URL}/p/$page")) }.fold(
                    onSuccess = { html ->
                        val file = dir.resolve("page-$page.html")
                        Files.writeString(file, html)
                        println("Captured nickel (page $page) -> $file (${html.length} bytes) at ${Instant.now()}")
                    },
                    onFailure = { error -> println("Failed to capture nickel page $page: ${error.message}") }
                )
            }
        },
        onFailure = { error -> println("Failed to capture nickel (page 1): ${error.message}") }
    )
}

/**
 * Generic two-step (index -> current year -> year page) capture, shared
 * by [KornikObwieszczeniaSource]/[DopiewoWzSource] - see
 * `captureBukObwieszczenia` for the one exception (Buk) that needs its
 * own function because it also parses a differently-shaped year URL for
 * the fixture-file year suffix.
 */
private fun captureYearSplitDiscovery(
    playwright: PlaywrightPageFetcher,
    fixturesDir: Path,
    sourceId: String,
    indexUrl: String,
    findCurrentYearUrl: (String, String) -> String?
) {
    val dir = fixturesDir.resolve(sourceId)
    val indexResult = runCatching { playwright.fetch(URI(indexUrl)) }
    indexResult.fold(
        onSuccess = { indexHtml ->
            Files.createDirectories(dir)
            Files.writeString(dir.resolve("index.html"), indexHtml)
            println("Captured $sourceId (index) -> ${dir.resolve("index.html")} (${indexHtml.length} bytes) at ${Instant.now()}")

            val yearUrl = findCurrentYearUrl(indexHtml, indexUrl)
            if (yearUrl == null) {
                println("Failed to capture $sourceId (year page): no year link found in index")
                return@fold
            }
            runCatching { playwright.fetch(URI(yearUrl)) }.fold(
                onSuccess = { yearHtml ->
                    val yearFile = dir.resolve("year-${CALENDAR_YEAR.find(yearUrl)?.value ?: "current"}.html")
                    Files.writeString(yearFile, yearHtml)
                    println("Captured $sourceId (year) -> $yearFile (${yearHtml.length} bytes) at ${Instant.now()}")
                },
                onFailure = { error -> println("Failed to capture $sourceId (year page): ${error.message}") }
            )
        },
        onFailure = { error -> println("Failed to capture $sourceId (index): ${error.message}") }
    )
}

private val CALENDAR_YEAR = Regex("\\d{4}")

private fun capture(fetcher: PageFetcher, fixturesDir: Path, sourceId: String, uri: URI, fileName: String) {
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
