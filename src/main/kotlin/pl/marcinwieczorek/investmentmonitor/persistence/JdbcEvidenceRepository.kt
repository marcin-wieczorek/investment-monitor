package pl.marcinwieczorek.investmentmonitor.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.domain.ExtractionMethod
import pl.marcinwieczorek.investmentmonitor.domain.SourceCategory
import pl.marcinwieczorek.investmentmonitor.domain.SourceEvidence
import java.net.URI
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcEvidenceRepository(private val jdbcTemplate: JdbcTemplate) : EvidenceRepository {

    override fun save(evidence: SourceEvidence) {
        jdbcTemplate.update(
            INSERT,
            evidence.investmentId,
            evidence.signalId,
            evidence.sourceId,
            evidence.sourceCategory.name,
            evidence.capturedAt.toString(),
            evidence.url.toString(),
            evidence.extractionMethod.name,
            evidence.fieldName,
            evidence.fieldValue
        )
    }

    override fun findByInvestment(investmentId: Long): List<SourceEvidence> =
        jdbcTemplate.query(SELECT_BY_INVESTMENT, EvidenceRowMapper, investmentId)

    override fun findBySignal(signalId: Long): List<SourceEvidence> =
        jdbcTemplate.query(SELECT_BY_SIGNAL, EvidenceRowMapper, signalId)

    private companion object {
        const val INSERT = """
            INSERT INTO source_evidence (
                investment_id, signal_id, source_id, source_category, captured_at, url,
                extraction_method, field_name, field_value
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        const val SELECT_BY_INVESTMENT = "SELECT * FROM source_evidence WHERE investment_id = ?"
        const val SELECT_BY_SIGNAL = "SELECT * FROM source_evidence WHERE signal_id = ?"
    }
}

private object EvidenceRowMapper : RowMapper<SourceEvidence> {
    override fun mapRow(rs: ResultSet, rowNum: Int): SourceEvidence = SourceEvidence(
        id = rs.getLong("id"),
        investmentId = rs.getNullableLong("investment_id"),
        signalId = rs.getNullableLong("signal_id"),
        sourceId = rs.getString("source_id"),
        sourceCategory = SourceCategory.valueOf(rs.getString("source_category")),
        capturedAt = Instant.parse(rs.getString("captured_at")),
        url = URI(rs.getString("url")),
        extractionMethod = ExtractionMethod.valueOf(rs.getString("extraction_method")),
        fieldName = rs.getString("field_name"),
        fieldValue = rs.getString("field_value")
    )

    private fun ResultSet.getNullableLong(column: String): Long? =
        getLong(column).takeUnless { wasNull() }
}
