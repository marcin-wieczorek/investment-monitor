package pl.marcinwieczorek.investmentmonitor.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.domain.Correlation
import pl.marcinwieczorek.investmentmonitor.domain.CorrelationConfidence
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcCorrelationRepository(private val jdbcTemplate: JdbcTemplate) : CorrelationRepository {

    /**
     * Single atomic `INSERT ... ON CONFLICT DO NOTHING` instead of a
     * separate `exists()` check followed by an insert - halves the
     * round-trips for the common "already correlated" case (see
     * [JdbcInvestmentRepository.upsert] for the same rationale).
     */
    override fun save(correlation: Correlation) {
        jdbcTemplate.update(
            INSERT,
            correlation.investmentId,
            correlation.signalId,
            correlation.confidence.name,
            MAPPER.writeValueAsString(correlation.matchedFeatures),
            correlation.reason,
            correlation.createdAt.toString()
        )
    }

    override fun findByInvestment(investmentId: Long): List<Correlation> =
        jdbcTemplate.query(SELECT_BY_INVESTMENT, CorrelationRowMapper, investmentId)

    override fun exists(investmentId: Long, signalId: Long): Boolean =
        jdbcTemplate.query(SELECT_EXISTS, { rs, _ -> rs.getInt(1) }, investmentId, signalId)
            .firstOrNull()?.let { it > 0 } ?: false

    override fun findAllWithLeadTime(): List<CorrelationLeadTime> =
        jdbcTemplate.query(SELECT_LEAD_TIME) { rs, _ ->
            CorrelationLeadTime(
                investmentName = rs.getString("investment_name"),
                signalTitle = rs.getString("signal_title"),
                leadTimeDays = rs.getObject("lead_time_days")?.let { (it as Number).toLong() }
            )
        }

    private companion object {
        val MAPPER = jacksonObjectMapper()

        const val INSERT = """
            INSERT INTO correlation (investment_id, signal_id, confidence, matched_features, reason, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(investment_id, signal_id) DO NOTHING
        """
        const val SELECT_BY_INVESTMENT = "SELECT * FROM correlation WHERE investment_id = ?"
        const val SELECT_EXISTS = "SELECT COUNT(*) FROM correlation WHERE investment_id = ? AND signal_id = ?"
        const val SELECT_LEAD_TIME = """
            SELECT
                i.name AS investment_name,
                s.title AS signal_title,
                CAST(julianday(i.first_seen_at) - julianday(s.first_seen_at) AS INTEGER) AS lead_time_days
            FROM correlation c
            JOIN investment i ON i.id = c.investment_id
            JOIN investment_signal s ON s.id = c.signal_id
            ORDER BY c.created_at DESC
        """
    }
}

private object CorrelationRowMapper : RowMapper<Correlation> {
    private val mapper = jacksonObjectMapper()

    override fun mapRow(rs: ResultSet, rowNum: Int): Correlation = Correlation(
        id = rs.getLong("id"),
        investmentId = rs.getLong("investment_id"),
        signalId = rs.getLong("signal_id"),
        confidence = CorrelationConfidence.valueOf(rs.getString("confidence")),
        matchedFeatures = mapper.readValue(rs.getString("matched_features")),
        reason = rs.getString("reason"),
        createdAt = Instant.parse(rs.getString("created_at"))
    )
}
