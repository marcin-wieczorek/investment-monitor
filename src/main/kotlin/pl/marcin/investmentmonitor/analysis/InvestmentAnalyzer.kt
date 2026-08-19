package pl.marcin.investmentmonitor.analysis

import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.LocationProfile

enum class Priority { HIGH, MEDIUM, LOW, UNKNOWN }

/**
 * Result of interpreting a single investment.
 *
 * Deliberately all-nullable/UNKNOWN-able: an analyzer that could not run
 * (e.g. no local LLM configured) must say so explicitly rather than
 * fabricate a score. See docs/ADR-002-deterministic-diff.md - identity and
 * change detection are deterministic; this layer is interpretation only,
 * never a source of truth for facts already known from parsing.
 */
data class InvestmentAnalysis(
    val investmentScore: Double?,
    val locationScore: Double?,
    val referenceProfileScore: Double?,
    val priority: Priority,
    val reason: String
)

/**
 * Interprets a newly detected investment against optional location context.
 *
 * Facts (price, area, location, property type, ...) must come from
 * deterministic parsing, never from the analyzer. The analyzer only scores
 * and explains; it must not be treated as a second source of truth for
 * facts already available from the source.
 */
interface InvestmentAnalyzer {
    fun analyze(investment: Investment, locationProfile: LocationProfile? = null): InvestmentAnalysis
}
