package pl.marcinwieczorek.investmentmonitor.monitoring

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.marcinwieczorek.investmentmonitor.analysis.LocationActivityCollector
import pl.marcinwieczorek.investmentmonitor.domain.HotspotSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.LocationSynthesis
import pl.marcinwieczorek.investmentmonitor.llm.LocationSynthesisAnalyzer
import pl.marcinwieczorek.investmentmonitor.persistence.LocationSynthesisRepository
import pl.marcinwieczorek.investmentmonitor.persistence.UserPreferencesRepository

/**
 * Per-scan location-intelligence step (see docs/ARCHITECTURE.md phase 12):
 * assembles a deterministic [pl.marcinwieczorek.investmentmonitor.domain.LocationActivity]
 * snapshot per active location via [LocationActivityCollector], has
 * [LocationSynthesisAnalyzer] interpret each one (LLM-assisted, or its
 * deterministic fallback), and persists both the per-location syntheses
 * and a single region-wide hotspot ranking via [LocationSynthesisRepository].
 *
 * Split out of [MonitoringService] for independent testability, same
 * rationale as [CrossSourceEnrichmentService]/[AggregatorDiscoveryService].
 * Runs after correlation/deduplication so [LocationActivityCollector] sees
 * the full, current cross-source picture (see `MonitoringService.scan()`).
 */
@Service
class LocationSynthesisService(
    private val activityCollector: LocationActivityCollector,
    private val synthesisAnalyzer: LocationSynthesisAnalyzer,
    private val synthesisRepository: LocationSynthesisRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:Value("\${investment-monitor.location-intelligence.min-signals-for-synthesis:2}")
    private val minSignalsForSynthesis: Int,
    @param:Value("\${investment-monitor.location-intelligence.max-locations-per-scan:20}")
    private val maxLocationsPerScan: Int,
    @param:Value("\${investment-monitor.location-intelligence.hotspot-top-n:10}")
    private val hotspotTopN: Int
) {

    /**
     * Synthesizes every currently active location (capped at
     * [maxLocationsPerScan]) plus one region-wide hotspot ranking, and
     * persists both. Returns what was computed so it can also be folded
     * into the console [pl.marcinwieczorek.investmentmonitor.reporting.ScanReport].
     */
    fun synthesize(): LocationIntelligenceResult {
        val referenceProfile = userPreferencesRepository.effectiveScoringProfile()
        val activeLocations = activityCollector.collectActive(minSignalsForSynthesis).take(maxLocationsPerScan)

        val locationSyntheses = activeLocations.map { activity ->
            val synthesis = synthesisAnalyzer.synthesizeLocation(activity, referenceProfile)
            synthesisRepository.upsertLocation(synthesis)
            synthesis
        }
        logger.info("Location intelligence: synthesized {} active location(s)", locationSyntheses.size)

        val hotspotSynthesis = if (activeLocations.isEmpty()) {
            null
        } else {
            synthesisAnalyzer.synthesizeHotspots(activeLocations, referenceProfile, hotspotTopN).also {
                synthesisRepository.saveHotspot(it)
            }
        }

        return LocationIntelligenceResult(locationSyntheses, hotspotSynthesis)
    }

    private companion object {
        val logger = LoggerFactory.getLogger(LocationSynthesisService::class.java)
    }
}

data class LocationIntelligenceResult(
    val locationSyntheses: List<LocationSynthesis>,
    val hotspotSynthesis: HotspotSynthesis?
)
