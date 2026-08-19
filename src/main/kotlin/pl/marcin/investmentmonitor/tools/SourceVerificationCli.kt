package pl.marcin.investmentmonitor.tools

import pl.marcin.investmentmonitor.scraping.JsoupPageFetcher
import pl.marcin.investmentmonitor.source.AggregatorSource
import pl.marcin.investmentmonitor.source.ChronosSource
import pl.marcin.investmentmonitor.source.DiscoverySource
import pl.marcin.investmentmonitor.source.GreenbudSource
import pl.marcin.investmentmonitor.source.InvestmentSource
import pl.marcin.investmentmonitor.source.aggregator.RynekPierwotnySource
import pl.marcin.investmentmonitor.source.discovery.SwarzedzWzSource
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
    val developerSources: List<InvestmentSource> = listOf(ChronosSource(fetcher), GreenbudSource(fetcher))
    val discoverySources: List<DiscoverySource> = listOf(SwarzedzWzSource(fetcher))
    val aggregatorSources: List<AggregatorSource> = listOf(RynekPierwotnySource(fetcher))
    val validator = SourceValidator()

    println("SOURCE VERIFICATION")
    println(SEPARATOR)

    println("-- developer sources --")
    developerSources.forEach { source -> verifyInvestmentSource(source.id, source::fetch, validator) }

    println("-- discovery sources --")
    discoverySources.forEach { source -> verifyDiscoverySource(source) }

    println("-- aggregator sources --")
    aggregatorSources.forEach { source -> verifyInvestmentSource(source.id, source::fetch, validator) }

    println(SEPARATOR)
    println("Live verification never updates the trusted snapshot.")
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
