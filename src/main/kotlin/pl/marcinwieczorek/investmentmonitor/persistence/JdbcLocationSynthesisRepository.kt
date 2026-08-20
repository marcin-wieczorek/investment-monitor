package pl.marcinwieczorek.investmentmonitor.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.domain.DevelopmentTrend
import pl.marcinwieczorek.investmentmonitor.domain.HotspotEntry
import pl.marcinwieczorek.investmentmonitor.domain.HotspotSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.LocationSynthesis
import pl.marcinwieczorek.investmentmonitor.domain.RecommendedAction
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcLocationSynthesisRepository(private val jdbcTemplate: JdbcTemplate) : LocationSynthesisRepository {

    override fun upsertLocation(synthesis: LocationSynthesis) {
        jdbcTemplate.update(
            UPSERT_LOCATION,
            synthesis.location,
            synthesis.municipality,
            synthesis.developmentTrend.name,
            synthesis.summary,
            synthesis.estimatedTimeline,
            MAPPER.writeValueAsString(synthesis.keyDevelopers),
            MAPPER.writeValueAsString(synthesis.opportunities),
            MAPPER.writeValueAsString(synthesis.risks),
            synthesis.recommendedAction.name,
            synthesis.reason,
            synthesis.signalCount,
            synthesis.investmentCount,
            synthesis.averageLeadTimeDays,
            synthesis.synthesizedAt.toString()
        )
    }

    override fun findByLocation(location: String): LocationSynthesis? =
        jdbcTemplate.query(SELECT_BY_LOCATION, LocationSynthesisRowMapper, location).firstOrNull()

    override fun findAllLocations(): List<LocationSynthesis> =
        jdbcTemplate.query(SELECT_ALL_LOCATIONS, LocationSynthesisRowMapper)

    override fun saveHotspot(synthesis: HotspotSynthesis) {
        jdbcTemplate.update(DELETE_HOTSPOT)
        jdbcTemplate.update(
            INSERT_HOTSPOT,
            MAPPER.writeValueAsString(synthesis.hotspots),
            MAPPER.writeValueAsString(synthesis.emergingAreas),
            synthesis.summary,
            synthesis.recommendation,
            synthesis.synthesizedAt.toString()
        )
    }

    override fun findLatestHotspot(): HotspotSynthesis? =
        jdbcTemplate.query(SELECT_LATEST_HOTSPOT, HotspotSynthesisRowMapper).firstOrNull()

    private companion object {
        val MAPPER = jacksonObjectMapper()

        const val UPSERT_LOCATION = """
            INSERT INTO location_synthesis (
                location, municipality, development_trend, summary, estimated_timeline,
                key_developers, opportunities, risks, recommended_action, reason,
                signal_count, investment_count, average_lead_time_days, synthesized_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(location) DO UPDATE SET
                municipality = excluded.municipality,
                development_trend = excluded.development_trend,
                summary = excluded.summary,
                estimated_timeline = excluded.estimated_timeline,
                key_developers = excluded.key_developers,
                opportunities = excluded.opportunities,
                risks = excluded.risks,
                recommended_action = excluded.recommended_action,
                reason = excluded.reason,
                signal_count = excluded.signal_count,
                investment_count = excluded.investment_count,
                average_lead_time_days = excluded.average_lead_time_days,
                synthesized_at = excluded.synthesized_at
        """
        const val SELECT_BY_LOCATION = "SELECT * FROM location_synthesis WHERE location = ?"
        const val SELECT_ALL_LOCATIONS = "SELECT * FROM location_synthesis ORDER BY signal_count DESC"

        // Only the latest region-wide hotspot ranking is ever meaningful (see V15 migration comment).
        const val DELETE_HOTSPOT = "DELETE FROM hotspot_synthesis"
        const val INSERT_HOTSPOT = """
            INSERT INTO hotspot_synthesis (hotspots, emerging_areas, summary, recommendation, synthesized_at)
            VALUES (?, ?, ?, ?, ?)
        """
        const val SELECT_LATEST_HOTSPOT = "SELECT * FROM hotspot_synthesis ORDER BY synthesized_at DESC LIMIT 1"
    }
}

private object LocationSynthesisRowMapper : RowMapper<LocationSynthesis> {
    private val mapper = jacksonObjectMapper()

    override fun mapRow(rs: ResultSet, rowNum: Int): LocationSynthesis = LocationSynthesis(
        location = rs.getString("location"),
        municipality = rs.getString("municipality"),
        developmentTrend = DevelopmentTrend.valueOf(rs.getString("development_trend")),
        summary = rs.getString("summary"),
        estimatedTimeline = rs.getString("estimated_timeline"),
        keyDevelopers = mapper.readValue(rs.getString("key_developers")),
        opportunities = mapper.readValue(rs.getString("opportunities")),
        risks = mapper.readValue(rs.getString("risks")),
        recommendedAction = RecommendedAction.valueOf(rs.getString("recommended_action")),
        reason = rs.getString("reason"),
        signalCount = rs.getInt("signal_count"),
        investmentCount = rs.getInt("investment_count"),
        averageLeadTimeDays = rs.getObject("average_lead_time_days")?.let { (it as Number).toDouble() },
        synthesizedAt = Instant.parse(rs.getString("synthesized_at"))
    )
}

private object HotspotSynthesisRowMapper : RowMapper<HotspotSynthesis> {
    private val mapper = jacksonObjectMapper()

    override fun mapRow(rs: ResultSet, rowNum: Int): HotspotSynthesis = HotspotSynthesis(
        hotspots = mapper.readValue<List<HotspotEntry>>(rs.getString("hotspots")),
        emergingAreas = mapper.readValue(rs.getString("emerging_areas")),
        summary = rs.getString("summary"),
        recommendation = rs.getString("recommendation"),
        synthesizedAt = Instant.parse(rs.getString("synthesized_at"))
    )
}
