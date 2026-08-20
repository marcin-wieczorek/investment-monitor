package pl.marcinwieczorek.investmentmonitor.domain

import java.time.Instant

/**
 * How development activity for a location appears to be moving, per the
 * LLM's reading of its recent [LocationActivity] (see
 * [pl.marcinwieczorek.investmentmonitor.llm.LocationSynthesisAnalyzer]).
 * Purely descriptive/qualitative - never used by any deterministic
 * decision (identity, diffing, scoring).
 */
enum class DevelopmentTrend { ACCELERATING, STABLE, SLOWING, MINIMAL }

/** How urgently a buyer following this tool's reference profile should track a location. */
enum class RecommendedAction { WATCH_CLOSELY, MONITOR, LOW_PRIORITY }

/** Relative amount of recent discovery/investment activity in a location. */
enum class ActivityLevel { HIGH, MEDIUM, LOW }

/**
 * A synthesis of everything currently known about one [location] -
 * generated from a [LocationActivity] snapshot, either by
 * [pl.marcinwieczorek.investmentmonitor.llm.LocationSynthesisAnalyzer]'s
 * LLM call (when enabled and available) or its purely deterministic
 * template fallback (see docs/LLM.md - same "never break, always
 * degrade gracefully" contract as [InvestmentAnalysis]). [summary],
 * [opportunities], [risks], [estimatedTimeline] and [reason] are always in
 * Polish, regardless of which path produced them, since this is
 * user-facing interpretive text, not a system identifier.
 *
 * Never a source of truth: [signalCount], [investmentCount] and
 * [averageLeadTimeDays] are copied straight from the [LocationActivity]
 * that was fed in, so this row is always explainable/reproducible from
 * already-persisted deterministic data.
 */
data class LocationSynthesis(
    val location: String,
    val municipality: String?,
    val developmentTrend: DevelopmentTrend,
    val summary: String,
    val estimatedTimeline: String?,
    val keyDevelopers: List<String>,
    val opportunities: List<String>,
    val risks: List<String>,
    val recommendedAction: RecommendedAction,
    val reason: String,
    val signalCount: Int,
    val investmentCount: Int,
    val averageLeadTimeDays: Double?,
    val synthesizedAt: Instant
)
