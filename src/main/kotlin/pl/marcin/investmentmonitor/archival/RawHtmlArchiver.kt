package pl.marcin.investmentmonitor.archival

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory

/**
 * Archives the raw HTML fetched for a source during a monitoring run, so a
 * parser failure can be debugged against exactly what the source looked
 * like at capture time (see docs/ARCHITECTURE.md raw source archival
 * section).
 *
 * Storage layout: `<basePath>/<yyyy-MM-dd>/<sourceId>/<sha256-prefix>.html`.
 * Retention is configurable and enforced by [cleanup], which removes date
 * directories older than the configured window - archival must not grow
 * without bound on a machine that runs scans indefinitely.
 */
@Component
class RawHtmlArchiver(
    @param:Value("\${investment-monitor.archival.enabled:true}") private val enabled: Boolean,
    @param:Value("\${investment-monitor.archival.path:raw}") basePath: String,
    @param:Value("\${investment-monitor.archival.retention-days:30}") private val retentionDays: Long,
    private val clock: Clock = Clock.systemUTC()
) {
    private val basePath: Path = Path.of(basePath)

    fun archive(sourceId: String, html: String): Path? {
        if (!enabled) return null

        return runCatching {
            val today = Instant.now(clock).atZone(ZoneOffset.UTC).format(DATE_FORMAT)
            val dir = basePath.resolve(today).resolve(sourceId)
            Files.createDirectories(dir)

            val hash = sha256(html).take(12)
            val file = dir.resolve("$hash.html")
            if (!file.exists()) {
                Files.writeString(file, html)
            }
            file
        }.onFailure { error ->
            logger.warn("Failed to archive raw HTML for source '{}': {}", sourceId, error.message)
        }.getOrNull()
    }

    /** Deletes date directories older than [retentionDays]. Never throws - archival hygiene must not fail a scan. */
    fun cleanup() {
        if (!enabled || !basePath.exists()) return

        runCatching {
            val cutoff = Instant.now(clock).minusSeconds(retentionDays * 24 * 60 * 60)
            Files.list(basePath).use { entries ->
                entries.filter { it.isDirectory() }
                    .filter { it.getLastModifiedTime().toInstant().isBefore(cutoff) }
                    .forEach { deleteRecursively(it) }
            }
        }.onFailure { error ->
            logger.warn("Raw archive cleanup failed: {}", error.message)
        }
    }

    private fun deleteRecursively(dir: Path) {
        Files.walk(dir).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(RawHtmlArchiver::class.java)
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
