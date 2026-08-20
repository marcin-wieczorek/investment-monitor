package pl.marcin.investmentmonitor.tools

import pl.marcin.investmentmonitor.scraping.JsoupPageFetcher
import pl.marcin.investmentmonitor.scraping.PageFetcher
import pl.marcin.investmentmonitor.scraping.PlaywrightPageFetcher
import pl.marcin.investmentmonitor.source.AgrobexSource
import pl.marcin.investmentmonitor.source.AggregatorSource
import pl.marcin.investmentmonitor.source.ArchicomSource
import pl.marcin.investmentmonitor.source.AreaSource
import pl.marcin.investmentmonitor.source.ATALSource
import pl.marcin.investmentmonitor.source.AtanerSource
import pl.marcin.investmentmonitor.source.ChronosSource
import pl.marcin.investmentmonitor.source.CordiaSource
import pl.marcin.investmentmonitor.source.DeveliaSource
import pl.marcin.investmentmonitor.source.DiscoverySource
import pl.marcin.investmentmonitor.source.DudaSource
import pl.marcin.investmentmonitor.source.EBFSource
import pl.marcin.investmentmonitor.source.GGWSource
import pl.marcin.investmentmonitor.source.GreenbudSource
import pl.marcin.investmentmonitor.source.InvestmentSource
import pl.marcin.investmentmonitor.source.InwestycjeWielkopolskiSource
import pl.marcin.investmentmonitor.source.JaksBudSource
import pl.marcin.investmentmonitor.source.JakonInwestSource
import pl.marcin.investmentmonitor.source.KonimpexSource
import pl.marcin.investmentmonitor.source.LineaSource
import pl.marcin.investmentmonitor.source.MJSource
import pl.marcin.investmentmonitor.source.MurapolSource
import pl.marcin.investmentmonitor.source.NickelSource
import pl.marcin.investmentmonitor.source.PWDSource
import pl.marcin.investmentmonitor.source.PekabexSource
import pl.marcin.investmentmonitor.source.RobygSource
import pl.marcin.investmentmonitor.source.RonsonSource
import pl.marcin.investmentmonitor.source.SagarisSource
import pl.marcin.investmentmonitor.source.SivanetSource
import pl.marcin.investmentmonitor.source.SpraviaSource
import pl.marcin.investmentmonitor.source.UWISource
import pl.marcin.investmentmonitor.source.VastbouwSource
import pl.marcin.investmentmonitor.source.aggregator.RynekPierwotnySource
import pl.marcin.investmentmonitor.source.discovery.BukObwieszczeniaSource
import pl.marcin.investmentmonitor.source.discovery.CzerwonakObwieszczeniaSource
import pl.marcin.investmentmonitor.source.discovery.DopiewoWzSource
import pl.marcin.investmentmonitor.source.discovery.KornikObwieszczeniaSource
import pl.marcin.investmentmonitor.source.discovery.MurowanaGoslinaObwieszczeniaSource
import pl.marcin.investmentmonitor.source.discovery.PobiedziskaKomunikatySource
import pl.marcin.investmentmonitor.source.discovery.PoznanUlicpSource
import pl.marcin.investmentmonitor.source.discovery.SremWzSource
import pl.marcin.investmentmonitor.source.discovery.SuchyLasNppSource
import pl.marcin.investmentmonitor.source.discovery.SwarzedzWzSource
import pl.marcin.investmentmonitor.source.discovery.SzamotulyUlicpSource
import pl.marcin.investmentmonitor.source.discovery.TarnowoPodgorneWzSource
import pl.marcin.investmentmonitor.validation.SourceValidator

/**
 * Entry point for live source verification.
 *
 * Verification is intentionally separate from a normal scan and must never
 * mutate the trusted monitoring snapshot: it only fetches, parses and
 * validates each configured source, then reports diagnostics.
 */
fun main() {
    val fetcher = JsoupPageFetcher()
    val developerSources: List<InvestmentSource> = listOf(
        ChronosSource(fetcher), GreenbudSource(fetcher), JakonInwestSource(fetcher), AgrobexSource(fetcher),
        LineaSource(fetcher), DudaSource(fetcher), AtanerSource(fetcher), UWISource(fetcher),
        KonimpexSource(fetcher), PekabexSource(fetcher), MurapolSource(fetcher), DeveliaSource(fetcher),
        ATALSource(fetcher), RobygSource(fetcher), EBFSource(fetcher), GGWSource(fetcher),
        SpraviaSource(fetcher), JaksBudSource(fetcher), SagarisSource(fetcher),
        CordiaSource(fetcher), RonsonSource(fetcher), SivanetSource(fetcher), MJSource(fetcher),
        AreaSource(fetcher), InwestycjeWielkopolskiSource(fetcher), VastbouwSource(fetcher), NickelSource(fetcher)
    )
    val discoverySources: List<DiscoverySource> = listOf(
        SwarzedzWzSource(fetcher), CzerwonakObwieszczeniaSource(fetcher), TarnowoPodgorneWzSource(fetcher),
        SuchyLasNppSource(fetcher), PoznanUlicpSource(fetcher), SremWzSource(fetcher),
        MurowanaGoslinaObwieszczeniaSource(fetcher)
    )
    val aggregatorSources: List<AggregatorSource> = listOf(RynekPierwotnySource(fetcher))
    val validator = SourceValidator()

    println("SOURCE VERIFICATION")
    println(SEPARATOR)

    println("-- developer sources --")
    developerSources.forEach { source -> verifyInvestmentSource(source.id, source::fetch, validator) }

    println("-- discovery sources --")
    discoverySources.forEach { source -> verifyDiscoverySource(source) }
    verifyBrowserRequiredDiscoverySources()

    println("-- aggregator sources --")
    aggregatorSources.forEach { source -> verifyInvestmentSource(source.id, source::fetch, validator) }

    println(SEPARATOR)
    println("Live verification never updates the trusted snapshot.")
}

/**
 * Verifies every discovery source requiring [PlaywrightPageFetcher] (see
 * ADR-007) separately from the plain-HTTP ones above - Chromium needs to
 * be installed (`npx playwright install chromium`), which isn't assumed
 * to be present in every environment running this CLI. Skips all of them
 * with a clear message rather than failing the whole run if Playwright
 * can't launch. Shares one browser instance across all sources.
 */
private fun verifyBrowserRequiredDiscoverySources() {
    val playwrightFetcher: PageFetcher? = runCatching { PlaywrightPageFetcher(30_000) }.getOrNull()
    if (playwrightFetcher == null) {
        println("buk-obwieszczenia, szamotuly-ulicp, pobiedziska-komunikaty, archicom, pwd: SKIPPED (Playwright/Chromium not available - see README.md 'Optional: headless-browser fetching')")
        return
    }
    try {
        verifyDiscoverySource(BukObwieszczeniaSource(playwrightFetcher))
        verifyDiscoverySource(SzamotulyUlicpSource(playwrightFetcher))
        verifyDiscoverySource(PobiedziskaKomunikatySource(playwrightFetcher))
        verifyDiscoverySource(KornikObwieszczeniaSource(playwrightFetcher))
        verifyDiscoverySource(DopiewoWzSource(playwrightFetcher))
        verifyInvestmentSource(ArchicomSource.SOURCE_ID, ArchicomSource(playwrightFetcher)::fetch, SourceValidator())
        verifyInvestmentSource(PWDSource.SOURCE_ID, PWDSource(playwrightFetcher)::fetch, SourceValidator())
    } finally {
        (playwrightFetcher as? AutoCloseable)?.close()
    }
}

private fun verifyInvestmentSource(
    id: String,
    fetch: () -> List<pl.marcin.investmentmonitor.domain.Investment>,
    validator: SourceValidator
) {
    val result = runCatching(fetch)

    result.fold(
        onSuccess = { investments ->
            val validation = validator.validate(investments, previousCount = null)
            val status = if (validation.valid) "PASS" else "FAIL (${validation.reason})"
            println("$id: HTTP OK, ${investments.size} investments, validation=$status")
        },
        onFailure = { error ->
            println("$id: HTTP FAILED (${error.message})")
        }
    )
}

private fun verifyDiscoverySource(source: DiscoverySource) {
    val result = runCatching(source::fetch)

    result.fold(
        onSuccess = { signals ->
            println("${source.id}: HTTP OK, ${signals.size} signals (${source.municipality})")
        },
        onFailure = { error ->
            println("${source.id}: HTTP FAILED (${error.message})")
        }
    )
}

private const val SEPARATOR = "----------------------------------------"
