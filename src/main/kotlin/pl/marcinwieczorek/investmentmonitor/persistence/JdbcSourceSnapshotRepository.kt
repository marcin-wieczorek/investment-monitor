package pl.marcinwieczorek.investmentmonitor.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.domain.SourceCategory
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcSourceSnapshotRepository(private val jdbcTemplate: JdbcTemplate) : SourceSnapshotRepository {

    override fun find(source: String): SourceSnapshot? =
        jdbcTemplate.query(SELECT, SourceSnapshotRowMapper, source).firstOrNull()

    override fun save(snapshot: SourceSnapshot) {
        val updatedRows = jdbcTemplate.update(
            UPDATE,
            snapshot.capturedAt.toString(),
            snapshot.investmentCount,
            snapshot.contentHash,
            snapshot.sourceCategory.name,
            snapshot.source
        )
        if (updatedRows == 0) {
            jdbcTemplate.update(
                INSERT,
                snapshot.source,
                snapshot.capturedAt.toString(),
                snapshot.investmentCount,
                snapshot.contentHash,
                snapshot.sourceCategory.name
            )
        }
    }

    private companion object {
        const val SELECT = "SELECT * FROM source_snapshot WHERE source = ?"
        const val UPDATE =
            "UPDATE source_snapshot SET captured_at = ?, investment_count = ?, content_hash = ?, source_category = ? WHERE source = ?"
        const val INSERT =
            "INSERT INTO source_snapshot (source, captured_at, investment_count, content_hash, source_category) VALUES (?, ?, ?, ?, ?)"
    }
}

private object SourceSnapshotRowMapper : RowMapper<SourceSnapshot> {
    override fun mapRow(rs: ResultSet, rowNum: Int): SourceSnapshot = SourceSnapshot(
        source = rs.getString("source"),
        capturedAt = Instant.parse(rs.getString("captured_at")),
        investmentCount = rs.getInt("investment_count"),
        contentHash = rs.getString("content_hash"),
        sourceCategory = rs.getString("source_category")?.let(SourceCategory::valueOf) ?: SourceCategory.DEVELOPER
    )
}
