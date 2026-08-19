package pl.marcin.investmentmonitor.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcin.investmentmonitor.domain.InvestmentSignal
import pl.marcin.investmentmonitor.domain.SignalType
import java.net.URI
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcSignalRepository(private val jdbcTemplate: JdbcTemplate) : SignalRepository {

    override fun findAllBySource(source: String): Map<String, InvestmentSignal> =
        jdbcTemplate.query(SELECT_BY_SOURCE, SignalRowMapper, source).associateBy { it.canonicalKey }

    override fun findAll(): List<InvestmentSignal> =
        jdbcTemplate.query(SELECT_ALL, SignalRowMapper)

    override fun upsert(signal: InvestmentSignal, seenAt: Instant) {
        val rawFactsJson = MAPPER.writeValueAsString(signal.rawFacts)
        val updatedRows = jdbcTemplate.update(
            UPDATE,
            signal.municipality,
            signal.location,
            signal.signalType.name,
            signal.title,
            signal.reference,
            signal.detectedAt.toString(),
            signal.url.toString(),
            rawFactsJson,
            seenAt.toString(),
            signal.canonicalKey
        )
        if (updatedRows == 0) {
            jdbcTemplate.update(
                INSERT,
                signal.source,
                signal.canonicalKey,
                signal.municipality,
                signal.location,
                signal.signalType.name,
                signal.title,
                signal.reference,
                signal.detectedAt.toString(),
                signal.url.toString(),
                rawFactsJson,
                seenAt.toString(),
                seenAt.toString()
            )
        }
    }

    override fun findIdByCanonicalKey(canonicalKey: String): Long? =
        jdbcTemplate.query(SELECT_ID, { rs, _ -> rs.getLong("id") }, canonicalKey).firstOrNull()

    private companion object {
        val MAPPER = jacksonObjectMapper()

        const val SELECT_BY_SOURCE = "SELECT * FROM investment_signal WHERE source = ?"
        const val SELECT_ALL = "SELECT * FROM investment_signal"
        const val SELECT_ID = "SELECT id FROM investment_signal WHERE canonical_key = ?"

        const val UPDATE = """
            UPDATE investment_signal SET
                municipality = ?, location = ?, signal_type = ?, title = ?, reference = ?,
                detected_at = ?, url = ?, raw_facts = ?, last_seen_at = ?
            WHERE canonical_key = ?
        """

        const val INSERT = """
            INSERT INTO investment_signal (
                source, canonical_key, municipality, location, signal_type, title, reference,
                detected_at, url, raw_facts, first_seen_at, last_seen_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
    }
}

private object SignalRowMapper : RowMapper<InvestmentSignal> {
    private val mapper = jacksonObjectMapper()

    override fun mapRow(rs: ResultSet, rowNum: Int): InvestmentSignal = InvestmentSignal(
        source = rs.getString("source"),
        municipality = rs.getString("municipality"),
        location = rs.getString("location"),
        signalType = SignalType.valueOf(rs.getString("signal_type")),
        title = rs.getString("title"),
        reference = rs.getString("reference"),
        detectedAt = Instant.parse(rs.getString("detected_at")),
        url = URI(rs.getString("url")),
        rawFacts = rs.getString("raw_facts")?.let { mapper.readValue(it) } ?: emptyMap()
    )
}
