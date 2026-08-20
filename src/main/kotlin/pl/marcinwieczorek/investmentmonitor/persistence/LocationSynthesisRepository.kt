package pl.marcinwieczorek.investmentmonitor.persistence

import pl.marcinwieczorek.investmentmonitor.domain.HotspotSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.LocationSynthesis

interface LocationSynthesisRepository {
    /** Upserts by [LocationSynthesis.location] - a location only ever has one current synthesis. */
    fun upsertLocation(synthesis: LocationSynthesis)
    fun findByLocation(location: String): LocationSynthesis?
    fun findAllLocations(): List<LocationSynthesis>

    /** Replaces the single current region-wide hotspot ranking - only "now" is ever meaningful, not history. */
    fun saveHotspot(synthesis: HotspotSynthesis)
    fun findLatestHotspot(): HotspotSynthesis?
}
