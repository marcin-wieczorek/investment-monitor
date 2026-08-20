package pl.marcinwieczorek.investmentmonitor.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.analysis.ScoringResult
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcInvestmentScoreRepository(private val jdbcTemplate: JdbcTemplate) : InvestmentScoreRepository {

    override fun save(investmentCanonicalKey: String, scoring: ScoringResult, scoredAt: Instant) {
        jdbcTemplate.update(
            UPSERT,
            investmentCanonicalKey,
            scoring.overallScore,
            scoring.propertyTypeMatch,
            scoring.locationTierMatch,
            scoring.houseAreaScore,
            scoring.plotAreaScore,
            scoring.priceScore,
            scoring.largePlotBonus,
            scoring.plotToHouseRatio,
            scoredAt.toString()
        )
    }

    override fun find(investmentCanonicalKey: String): ScoringResult? =
        jdbcTemplate.query(SELECT_BY_KEY, ScoringResultRowMapper, investmentCanonicalKey).firstOrNull()

    private companion object {
        const val UPSERT = """
            INSERT INTO investment_score
                (investment_canonical_key, overall_score, property_type_match, location_tier_match,
                 house_area_score, plot_area_score, price_score, large_plot_bonus, plot_to_house_ratio, scored_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(investment_canonical_key) DO UPDATE SET
                overall_score = excluded.overall_score,
                property_type_match = excluded.property_type_match,
                location_tier_match = excluded.location_tier_match,
                house_area_score = excluded.house_area_score,
                plot_area_score = excluded.plot_area_score,
                price_score = excluded.price_score,
                large_plot_bonus = excluded.large_plot_bonus,
                plot_to_house_ratio = excluded.plot_to_house_ratio,
                scored_at = excluded.scored_at
        """
        const val SELECT_BY_KEY = "SELECT * FROM investment_score WHERE investment_canonical_key = ?"
    }
}

private object ScoringResultRowMapper : RowMapper<ScoringResult> {
    override fun mapRow(rs: ResultSet, rowNum: Int): ScoringResult = ScoringResult(
        propertyTypeMatch = rs.getBoolean("property_type_match"),
        locationTierMatch = nullableBoolean(rs, "location_tier_match"),
        houseAreaScore = rs.getObject("house_area_score") as? Double,
        plotAreaScore = rs.getObject("plot_area_score") as? Double,
        priceScore = rs.getObject("price_score") as? Double,
        largePlotBonus = rs.getBoolean("large_plot_bonus"),
        plotToHouseRatio = rs.getObject("plot_to_house_ratio") as? Double,
        overallScore = rs.getDouble("overall_score")
    )

    /** SQLite stores booleans as INTEGER (0/1); JDBC may return them as any Number type. */
    private fun nullableBoolean(rs: ResultSet, column: String): Boolean? {
        val value = rs.getObject(column) ?: return null
        return (value as? Number)?.toInt() == 1
    }
}
