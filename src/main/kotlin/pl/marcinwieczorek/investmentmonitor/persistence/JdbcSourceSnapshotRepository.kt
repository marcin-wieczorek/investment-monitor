package pl.marcinwieczorek.investmentmonitor.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.domain.SourceCategory
import pl.marcinwieczorek.investmentmonitor.domain.SourceId
import java.sql.ResultSet
import java.time.Instant

@Repository
class JdbcSourceSnapshotRepository(private val jdbcTemplate: JdbcTemplate) : SourceSnapshotRepository {

    override fun find(source: SourceId): SourceSnapshot? =
        jdbcTemplate.query(SELECT, SourceSnapshotRowMapper, source.value).firstOrNull()

    /** Single atomic upsert - see [JdbcInvestmentRepository.upsert] for the rationale. */
    override fun save(snapshot: SourceSnapshot) {
        jdbcTemplate.update(
            UPSERT,
            snapshot.source.value,
            snapshot.capturedAt.toString(),
            snapshot.investmentCount,
            snapshot.contentHash,
            snapshot.sourceCategory.name
        )
    }

    private companion object {
        const val SELECT = "SELECT * FROM source_snapshot WHERE source = ?"
        const val UPSERT = """
            INSERT INTO source_snapshot (source, captured_at, investment_count, content_hash, source_category)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(source) DO UPDATE SET
                captured_at = excluded.captured_at,
                investment_count = excluded.investment_count,
                content_hash = excluded.content_hash,
                source_category = excluded.source_category
        """
    }
}

private object SourceSnapshotRowMapper : RowMapper<SourceSnapshot> {
    override fun mapRow(rs: ResultSet, rowNum: Int): SourceSnapshot = SourceSnapshot(
        source = SourceId(rs.getString("source")),
        capturedAt = Instant.parse(rs.getString("captured_at")),
        investmentCount = rs.getInt("investment_count"),
        contentHash = rs.getString("content_hash"),
        sourceCategory = rs.getString("source_category")?.let(SourceCategory::valueOf) ?: SourceCategory.DEVELOPER
    )
}
