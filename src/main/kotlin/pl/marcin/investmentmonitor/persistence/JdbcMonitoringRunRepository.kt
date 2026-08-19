package pl.marcin.investmentmonitor.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.Statement
import java.time.Instant

@Repository
class JdbcMonitoringRunRepository(private val jdbcTemplate: JdbcTemplate) : MonitoringRunRepository {

    override fun start(startedAt: Instant): Long {
        val keyHolder: KeyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ connection ->
            val statement: PreparedStatement = connection.prepareStatement(
                "INSERT INTO monitoring_run (started_at, status) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            statement.setString(1, startedAt.toString())
            statement.setString(2, "RUNNING")
            statement
        }, keyHolder)
        return keyHolder.key!!.toLong()
    }

    override fun finish(
        id: Long,
        finishedAt: Instant,
        status: String,
        sourcesChecked: Int,
        sourcesFailed: Int,
        newInvestments: Int
    ) {
        jdbcTemplate.update(
            UPDATE,
            finishedAt.toString(),
            status,
            sourcesChecked,
            sourcesFailed,
            newInvestments,
            id
        )
    }

    private companion object {
        const val UPDATE = """
            UPDATE monitoring_run
            SET finished_at = ?, status = ?, sources_checked = ?, sources_failed = ?, new_investments = ?
            WHERE id = ?
        """
    }
}
