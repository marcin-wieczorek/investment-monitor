package pl.marcin.investmentmonitor.tools

import pl.marcin.investmentmonitor.scraping.JsoupPageFetcher
import pl.marcin.investmentmonitor.source.ChronosSource
import pl.marcin.investmentmonitor.source.GreenbudSource
import pl.marcin.investmentmonitor.source.InvestmentSource
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
    val sources: List<InvestmentSource> = listOf(ChronosSource(fetcher), GreenbudSource(fetcher))
    val validator = SourceValidator()

    println("SOURCE VERIFICATION")
    println(SEPARATOR)

    sources.forEach { source -> verify(source, validator) }

    println(SEPARATOR)
    println("Live verification never updates the trusted snapshot.")
}

private fun verify(source: InvestmentSource, validator: SourceValidator) {
    val result = runCatching { source.fetch() }

    result.fold(
        onSuccess = { investments ->
            val validation = validator.validate(investments, previousCount = null)
            val status = if (validation.valid) "PASS" else "FAIL (${validation.reason})"
            println("${source.id}: HTTP OK, ${investments.size} investments, validation=$status")
        },
        onFailure = { error ->
            println("${source.id}: HTTP FAILED (${error.message})")
        }
    )
}

private const val SEPARATOR = "----------------------------------------"
