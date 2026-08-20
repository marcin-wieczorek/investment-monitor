package pl.marcinwieczorek.investmentmonitor.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.domain.DuplicateConfidence
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentDuplicate
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcInvestmentDuplicateRepository(private val jdbcTemplate: JdbcTemplate) : InvestmentDuplicateRepository {

    override fun save(duplicate: InvestmentDuplicate) {
        val idA = minOf(duplicate.investmentIdA, duplicate.investmentIdB)
        val idB = maxOf(duplicate.investmentIdA, duplicate.investmentIdB)
        if (exists(idA, idB)) return
        jdbcTemplate.update(
            INSERT,
            idA,
            idB,
            duplicate.confidence.name,
            MAPPER.writeValueAsString(duplicate.matchedFeatures),
            duplicate.reason,
            duplicate.createdAt.toString()
        )
    }

    override fun findByInvestment(investmentId: Long): List<InvestmentDuplicate> =
        jdbcTemplate.query(SELECT_BY_INVESTMENT, InvestmentDuplicateRowMapper, investmentId, investmentId)

    override fun exists(investmentIdA: Long, investmentIdB: Long): Boolean {
        val idA = minOf(investmentIdA, investmentIdB)
        val idB = maxOf(investmentIdA, investmentIdB)
        return jdbcTemplate.query(SELECT_EXISTS, { rs, _ -> rs.getInt(1) }, idA, idB)
            .firstOrNull()?.let { it > 0 } ?: false
    }

    private companion object {
        val MAPPER = jacksonObjectMapper()

        const val INSERT = """
            INSERT INTO investment_duplicate
                (investment_id_a, investment_id_b, confidence, matched_features, reason, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """
        const val SELECT_BY_INVESTMENT =
            "SELECT * FROM investment_duplicate WHERE investment_id_a = ? OR investment_id_b = ?"
        const val SELECT_EXISTS =
            "SELECT COUNT(*) FROM investment_duplicate WHERE investment_id_a = ? AND investment_id_b = ?"
    }
}

private object InvestmentDuplicateRowMapper : RowMapper<InvestmentDuplicate> {
    private val mapper = jacksonObjectMapper()

    override fun mapRow(rs: ResultSet, rowNum: Int): InvestmentDuplicate = InvestmentDuplicate(
        id = rs.getLong("id"),
        investmentIdA = rs.getLong("investment_id_a"),
        investmentIdB = rs.getLong("investment_id_b"),
        confidence = DuplicateConfidence.valueOf(rs.getString("confidence")),
        matchedFeatures = mapper.readValue(rs.getString("matched_features")),
        reason = rs.getString("reason"),
        createdAt = Instant.parse(rs.getString("created_at"))
    )
}
