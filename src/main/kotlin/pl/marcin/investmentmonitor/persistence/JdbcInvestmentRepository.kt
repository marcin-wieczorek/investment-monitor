package pl.marcin.investmentmonitor.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcin.investmentmonitor.domain.AreaRange
import pl.marcin.investmentmonitor.domain.Investment
import pl.marcin.investmentmonitor.domain.InvestmentStatus
import pl.marcin.investmentmonitor.domain.PriceRange
import pl.marcin.investmentmonitor.domain.PropertyType
import java.net.URI
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcInvestmentRepository(private val jdbcTemplate: JdbcTemplate) : InvestmentRepository {

    override fun findAllBySource(source: String): Map<String, Investment> =
        jdbcTemplate.query(SELECT_BY_SOURCE, InvestmentRowMapper, source)
            .associateBy { it.canonicalKey }

    override fun findAll(): List<Investment> =
        jdbcTemplate.query(SELECT_ALL, InvestmentRowMapper)

    override fun upsert(investment: Investment, seenAt: Instant) {
        val updatedRows = jdbcTemplate.update(UPDATE, *updateArgs(investment, seenAt))
        if (updatedRows == 0) {
            jdbcTemplate.update(INSERT, *insertArgs(investment, seenAt))
        }
    }

    override fun findIdByCanonicalKey(canonicalKey: String): Long? =
        jdbcTemplate.query(SELECT_ID, { rs, _ -> rs.getLong("id") }, canonicalKey).firstOrNull()

    private fun updateArgs(investment: Investment, seenAt: Instant): Array<Any?> = arrayOf(
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
        investment.imageUrl,
        seenAt.toString(),
        investment.canonicalKey
    )

    private fun insertArgs(investment: Investment, seenAt: Instant): Array<Any?> = arrayOf(
        investment.source,
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
        investment.imageUrl,
        seenAt.toString(),
        seenAt.toString()
    )

    private companion object {
        const val SELECT_BY_SOURCE = "SELECT * FROM investment WHERE source = ?"
        const val SELECT_ALL = "SELECT * FROM investment"
        const val SELECT_ID = "SELECT id FROM investment WHERE canonical_key = ?"

        const val UPDATE = """
            UPDATE investment SET
                developer = ?, name = ?, url = ?, location = ?, property_type = ?,
                units = ?, house_area_min = ?, house_area_max = ?,
                plot_area_min = ?, plot_area_max = ?, price_min = ?, price_max = ?,
                status = ?, image_url = ?, last_seen_at = ?
            WHERE canonical_key = ?
        """

        const val INSERT = """
            INSERT INTO investment (
                source, canonical_key, developer, name, url, location, property_type,
                units, house_area_min, house_area_max, plot_area_min, plot_area_max,
                price_min, price_max, status, image_url, first_seen_at, last_seen_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
    }
}

private object InvestmentRowMapper : RowMapper<Investment> {

    override fun mapRow(rs: ResultSet, rowNum: Int): Investment = Investment(
        source = rs.getString("source"),
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
        imageUrl = rs.getString("image_url")
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
