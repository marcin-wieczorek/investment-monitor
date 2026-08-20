package pl.marcinwieczorek.investmentmonitor.analysis

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentSignal
import pl.marcinwieczorek.investmentmonitor.domain.LocationActivity
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.persistence.CorrelationRepository
import pl.marcinwieczorek.investmentmonitor.persistence.InvestmentRepository
import pl.marcinwieczorek.investmentmonitor.persistence.SignalRepository
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Deterministically assembles a [LocationActivity] snapshot per location -
 * every known [pl.marcinwieczorek.investmentmonitor.domain.Investment],
 * every recent [InvestmentSignal], and every matching correlation - purely
 * by aggregating already-persisted facts from
 * [InvestmentRepository]/[SignalRepository]/[CorrelationRepository]. No LLM
 * involvement here (see docs/ARCHITECTURE.md LLM role section): this class
 * only ever produces the deterministic *input* that
 * [pl.marcinwieczorek.investmentmonitor.llm.LocationSynthesisAnalyzer]
 * later interprets.
 *
 * A "location" here is any name in [LocationCatalog.ALL_LOCATIONS] - either
 * a top-level municipality (e.g. "Swarzędz") or one of its outlying
 * villages (e.g. "Jasin", whose [LocationActivity.municipality] resolves
 * to "Swarzędz" via [LocationCatalog.parentMunicipality]).
 *
 * Investments are included regardless of age (a known investment doesn't
 * become less relevant context just because it's old), but signals are
 * filtered to the last `activityPeriodDays` by their [InvestmentSignal.detectedAt]
 * (the official decision/announcement date, not the scrape date) - the
 * activity window is about how much has happened in the discovery
 * pipeline recently, not about investment age.
 */
@Component
class LocationActivityCollector(
    private val investmentRepository: InvestmentRepository,
    private val signalRepository: SignalRepository,
    private val correlationRepository: CorrelationRepository,
    @param:Value("\${investment-monitor.location-intelligence.activity-period-days:365}")
    private val activityPeriodDays: Long,
    private val clock: Clock = Clock.systemUTC()
) {

    /** Assembles the activity snapshot for exactly one location, whether or not it currently has any activity. */
    fun collectForLocation(location: String): LocationActivity {
        val since = Instant.now(clock).minus(activityPeriodDays, ChronoUnit.DAYS)
        val allInvestments = investmentRepository.findAll()
        val allSignals = signalRepository.findAll()
        val allCorrelations = correlationRepository.findAllWithLeadTime()

        val investments = allInvestments.filter { it.location.matches(location) }
        val signals = allSignals.filter { signal -> signal.effectiveLocation().matches(location) && signal.detectedAt >= since }
        val correlations = allCorrelations.filter { it.investmentLocation.matches(location) }

        return LocationActivity(
            location = location,
            municipality = LocationCatalog.parentMunicipality(location),
            locationProfile = LocationProfiles.find(location),
            investments = investments,
            signals = signals,
            correlations = correlations
        )
    }

    /** Every location that has at least one investment or one signal (of any age) recorded against it. */
    fun collectAll(): List<LocationActivity> = discoverLocationNames().map { collectForLocation(it) }

    /** Only locations with enough recent activity to be worth synthesizing (see [LocationActivity.hasActivity]). */
    fun collectActive(minSignals: Int): List<LocationActivity> = collectAll().filter { it.hasActivity(minSignals) }

    private fun discoverLocationNames(): Set<String> {
        val fromInvestments = investmentRepository.findAll().mapNotNull { it.location }
        val fromSignals = signalRepository.findAll().map { it.effectiveLocation() }.filterNotNull()
        return (fromInvestments + fromSignals).toSet()
    }

    private fun InvestmentSignal.effectiveLocation(): String? = location ?: municipality

    private fun String?.matches(other: String): Boolean = this?.equals(other, ignoreCase = true) == true
}
