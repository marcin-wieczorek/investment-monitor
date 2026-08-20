package pl.marcinwieczorek.investmentmonitor.domain

import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationLeadTime

/**
 * Everything currently known about a single [location] - every developer/
 * aggregator [Investment], every discovery [InvestmentSignal], and every
 * [Correlation] involving them, within a rolling activity window (see
 * `investment-monitor.location-intelligence.activity-period-days`).
 *
 * This is purely an aggregation of already-persisted, already-deterministic
 * facts - built by
 * [pl.marcinwieczorek.investmentmonitor.analysis.LocationActivityCollector],
 * never by an LLM. It is the *input* to the LLM-driven
 * [LocationSynthesis]/[HotspotSynthesis] layer, not a replacement for it:
 * the LLM only ever interprets a [LocationActivity] that has already been
 * assembled deterministically (see docs/ARCHITECTURE.md LLM role section).
 */
data class LocationActivity(
    val location: String,
    val municipality: String?,
    val locationProfile: LocationProfile?,
    val investments: List<Investment>,
    val signals: List<InvestmentSignal>,
    val correlations: List<CorrelationLeadTime>
) {
    val activeDevelopers: List<String>
        get() = investments.map { it.developer }.distinct().sorted()

    val dominantSignalTypes: List<SignalType>
        get() = signals.groupingBy { it.signalType }.eachCount().entries
            .sortedByDescending { it.value }
            .map { it.key }

    val averageLeadTimeDays: Double?
        get() = correlations.mapNotNull { it.leadTimeDays }.takeIf { it.isNotEmpty() }?.average()

    val signalCount: Int get() = signals.size
    val investmentCount: Int get() = investments.size
    val correlationCount: Int get() = correlations.size

    /** True if there is enough activity for a synthesis to be worth generating. */
    fun hasActivity(minSignals: Int): Boolean = signalCount >= minSignals || investmentCount > 0
}
