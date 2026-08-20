package pl.marcinwieczorek.investmentmonitor.testsupport

import org.flywaydb.core.Flyway
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.nio.file.Files
import java.nio.file.Path

/**
 * A real, migrated SQLite database backing a single [JdbcTemplate] for
 * persistence-layer tests - standalone (no Spring test context), matching
 * the codebase's existing preference for hand-written fakes over
 * framework magic (see AGENTS.md "Tests" convention).
 *
 * Uses a temp *file* database (not `:memory:`) because plain in-memory
 * SQLite is scoped per-connection - two connections would see two
 * different empty databases. [SingleConnectionDataSource] keeps exactly
 * one JDBC connection alive for the lifetime of the test, which combined
 * with a file-backed database also makes multi-connection semantics
 * (like `PRAGMA busy_timeout`) irrelevant for these single-threaded tests.
 *
 * Call [close] (e.g. from a JUnit `@AfterEach`) to release the connection
 * and delete the backing file.
 */
class TestDatabase private constructor(private val path: Path, val jdbcTemplate: JdbcTemplate, private val dataSource: SingleConnectionDataSource) {

    fun close() {
        dataSource.destroy()
        Files.deleteIfExists(path)
    }

    companion object {
        fun create(): TestDatabase {
            val path = Files.createTempFile("investment-monitor-test-", ".db")
            val dataSource = SingleConnectionDataSource("jdbc:sqlite:$path", true)
            dataSource.setDriverClassName("org.sqlite.JDBC")
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()
            return TestDatabase(path, JdbcTemplate(dataSource), dataSource)
        }
    }
}
