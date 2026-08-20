package pl.marcinwieczorek.investmentmonitor.domain

import java.time.Instant

/** One location's entry within a [HotspotSynthesis] ranking. */
data class HotspotEntry(
    val location: String,
    val activityLevel: ActivityLevel,
    val trend: DevelopmentTrend,
    val reason: String,
    val relevanceToProfile: ActivityLevel
)

/**
 * A single, region-wide comparison of development activity across every
 * currently active location, generated once per scan (see
 * `MonitoringService` location-intelligence step) by
 * [pl.marcinwieczorek.investmentmonitor.llm.LocationSynthesisAnalyzer]'s
 * `synthesizeHotspots()` - or its deterministic fallback (locations ranked
 * purely by signal count) when the LLM is unavailable. [summary] and
 * [recommendation] are always in Polish, regardless of which path produced
 * them.
 *
 * Unlike per-location [LocationSynthesis], this is deliberately global and
 * comparative: its value comes from ranking/contrasting locations against
 * each other, which a per-location synthesis structurally cannot do.
 */
data class HotspotSynthesis(
    val hotspots: List<HotspotEntry>,
    val emergingAreas: List<String>,
    val summary: String,
    val recommendation: String,
    val synthesizedAt: Instant
)
