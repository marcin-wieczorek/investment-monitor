package pl.marcinwieczorek.investmentmonitor.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperCandidate
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperCandidateStatus
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperNameMatcher
import java.net.URI
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

@Repository
class JdbcDeveloperCandidateRepository(private val jdbcTemplate: JdbcTemplate) : DeveloperCandidateRepository {

    override fun save(candidate: DeveloperCandidate): Long {
        val keyHolder: KeyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ connection ->
            val statement: PreparedStatement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)
            statement.setString(1, candidate.developerName)
            statement.setString(2, candidate.discoveredUrl.toString())
            statement.setString(3, candidate.municipality)
            statement.setString(4, candidate.discoveredFromSource)
            statement.setString(5, candidate.discoveredAt.toString())
            statement.setString(6, candidate.status.name)
            statement.setString(7, candidate.evidence)
            statement
        }, keyHolder)
        return keyHolder.key?.toLong() ?: error("Insert did not return a generated key")
    }

    override fun findAll(): List<DeveloperCandidate> =
        jdbcTemplate.query(SELECT_ALL, DeveloperCandidateRowMapper)

    override fun findByName(developerName: String): DeveloperCandidate? =
        jdbcTemplate.query(SELECT_ALL, DeveloperCandidateRowMapper)
            .firstOrNull { DeveloperNameMatcher.matches(it.developerName, developerName) }

    override fun updateStatus(id: Long, status: DeveloperCandidateStatus) {
        jdbcTemplate.update(UPDATE_STATUS, status.name, id)
    }

    private companion object {
        const val INSERT = """
            INSERT INTO developer_candidate
                (developer_name, discovered_url, municipality, discovered_from_source, discovered_at, status, evidence)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """
        const val SELECT_ALL = "SELECT * FROM developer_candidate ORDER BY discovered_at DESC"
        const val UPDATE_STATUS = "UPDATE developer_candidate SET status = ? WHERE id = ?"
    }
}

private object DeveloperCandidateRowMapper : RowMapper<DeveloperCandidate> {
    override fun mapRow(rs: ResultSet, rowNum: Int): DeveloperCandidate = DeveloperCandidate(
        id = rs.getLong("id"),
        developerName = rs.getString("developer_name"),
        discoveredUrl = URI(rs.getString("discovered_url")),
        municipality = rs.getString("municipality"),
        discoveredFromSource = rs.getString("discovered_from_source"),
        discoveredAt = Instant.parse(rs.getString("discovered_at")),
        status = DeveloperCandidateStatus.valueOf(rs.getString("status")),
        evidence = rs.getString("evidence")
    )
}
