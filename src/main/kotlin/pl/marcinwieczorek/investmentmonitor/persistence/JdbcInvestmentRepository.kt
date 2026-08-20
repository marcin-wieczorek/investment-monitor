package pl.marcinwieczorek.investmentmonitor.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import pl.marcinwieczorek.investmentmonitor.domain.PriceRange
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import java.net.URI
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcInvestmentRepository(private val jdbcTemplate: JdbcTemplate) : InvestmentRepository {

    override fun findAllBySource(source: SourceId): Map<String, Investment> =
        jdbcTemplate.query(SELECT_BY_SOURCE, InvestmentRowMapper, source.value)
            .associateBy { it.canonicalKey }

    override fun findAll(): List<Investment> =
        jdbcTemplate.query(SELECT_ALL, InvestmentRowMapper)

    /**
     * Single atomic upsert (`INSERT ... ON CONFLICT ... DO UPDATE`) instead of a
     * separate UPDATE-then-INSERT probe - see [JdbcInvestmentScoreRepository] for
     * the same pattern. `first_seen_at` is deliberately excluded from the
     * `DO UPDATE SET` clause so it is only ever set once, on the initial insert.
     */
    override fun upsert(investment: Investment, seenAt: Instant) {
        jdbcTemplate.update(UPSERT, *upsertArgs(investment, seenAt))
    }

    override fun findIdByCanonicalKey(canonicalKey: String): Long? =
        jdbcTemplate.query(SELECT_ID, { rs, _ -> rs.getLong("id") }, canonicalKey).firstOrNull()

    override fun updateAggregatorOnlyDiscoveryFlag(canonicalKey: String, isAggregatorOnly: Boolean) {
        jdbcTemplate.update(UPDATE_AGGREGATOR_ONLY_FLAG, if (isAggregatorOnly) 1 else 0, canonicalKey)
    }

    private fun upsertArgs(investment: Investment, seenAt: Instant): Array<Any?> = arrayOf(
        investment.source.value,
        investment.canonicalKey,
        investment.developer,
        investment.name,
        investment.url.toString(),
        investment.location,
        investment.propertyType?.name,
        investment.units,
        investment.houseArea?.min,
        investment.houseArea?.max,
        investment.plotArea?.min,
        investment.plotArea?.max,
        investment.price?.min,
        investment.price?.max,
        investment.status?.name,
        investment.imageUrl?.toString(),
        seenAt.toString(),
        seenAt.toString()
    )

    private companion object {
        const val SELECT_BY_SOURCE = "SELECT * FROM investment WHERE source = ?"
        const val SELECT_ALL = "SELECT * FROM investment"
        const val SELECT_ID = "SELECT id FROM investment WHERE canonical_key = ?"
        const val UPDATE_AGGREGATOR_ONLY_FLAG = "UPDATE investment SET aggregator_only_discovery = ? WHERE canonical_key = ?"

        const val UPSERT = """
            INSERT INTO investment (
                source, canonical_key, developer, name, url, location, property_type,
                units, house_area_min, house_area_max, plot_area_min, plot_area_max,
                price_min, price_max, status, image_url, first_seen_at, last_seen_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(canonical_key) DO UPDATE SET
                developer = excluded.developer,
                name = excluded.name,
                url = excluded.url,
                location = excluded.location,
                property_type = excluded.property_type,
                units = excluded.units,
                house_area_min = excluded.house_area_min,
                house_area_max = excluded.house_area_max,
                plot_area_min = excluded.plot_area_min,
                plot_area_max = excluded.plot_area_max,
                price_min = excluded.price_min,
                price_max = excluded.price_max,
                status = excluded.status,
                image_url = excluded.image_url,
                last_seen_at = excluded.last_seen_at
        """
    }
}

private object InvestmentRowMapper : RowMapper<Investment> {

    override fun mapRow(rs: ResultSet, rowNum: Int): Investment = Investment(
        source = SourceId(rs.getString("source")),
        developer = rs.getString("developer"),
        name = rs.getString("name"),
        url = URI(rs.getString("url")),
        location = rs.getString("location"),
        propertyType = rs.getString("property_type")?.let(PropertyType::valueOf),
        units = rs.getNullableInt("units"),
        houseArea = areaRange(rs, "house_area_min", "house_area_max"),
        plotArea = areaRange(rs, "plot_area_min", "plot_area_max"),
        price = priceRange(rs),
        status = rs.getString("status")?.let(InvestmentStatus::valueOf),
        imageUrl = rs.getString("image_url")?.let(::URI)
    )

    private fun areaRange(rs: ResultSet, minColumn: String, maxColumn: String): AreaRange? {
        val min = rs.getNullableDouble(minColumn)
        val max = rs.getNullableDouble(maxColumn)
        return if (min == null && max == null) null else AreaRange(min, max)
    }

    private fun priceRange(rs: ResultSet): PriceRange? {
        val min = rs.getNullableInt("price_min")
        val max = rs.getNullableInt("price_max")
        return if (min == null && max == null) null else PriceRange(min, max)
    }

    private fun ResultSet.getNullableInt(column: String): Int? =
        getInt(column).takeUnless { wasNull() }

    private fun ResultSet.getNullableDouble(column: String): Double? =
        getDouble(column).takeUnless { wasNull() }
}
