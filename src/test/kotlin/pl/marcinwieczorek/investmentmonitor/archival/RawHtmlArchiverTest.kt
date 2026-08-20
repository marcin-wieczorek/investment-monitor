package pl.marcinwieczorek.investmentmonitor.archival

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RawHtmlArchiverTest {

    @Test
    fun `archives HTML under a date-source directory`(@TempDir tempDir: Path) {
        val clock = Clock.fixed(Instant.parse("2026-08-19T10:00:00Z"), ZoneOffset.UTC)
        val archiver = RawHtmlArchiver(enabled = true, basePath = tempDir.toString(), retentionDays = 30, clock = clock)

        val file = archiver.archive("chronos", "<html>test</html>")

        file.shouldNotBeNull()
        Files.exists(file).shouldBeTrue()
        file.toString().contains("2026-08-19") shouldBe true
        file.toString().contains("chronos") shouldBe true
    }

    @Test
    fun `does nothing when archival is disabled`(@TempDir tempDir: Path) {
        val archiver = RawHtmlArchiver(enabled = false, basePath = tempDir.toString(), retentionDays = 30)
        archiver.archive("chronos", "<html>test</html>").shouldBeNull()
        Files.list(tempDir).use { it.findAny().isPresent shouldBe false }
    }

    @Test
    fun `does not duplicate identical content captured twice on the same day`(@TempDir tempDir: Path) {
        val clock = Clock.fixed(Instant.parse("2026-08-19T10:00:00Z"), ZoneOffset.UTC)
        val archiver = RawHtmlArchiver(enabled = true, basePath = tempDir.toString(), retentionDays = 30, clock = clock)

        val first = archiver.archive("chronos", "<html>same</html>")
        val second = archiver.archive("chronos", "<html>same</html>")

        first shouldBe second
    }

    @Test
    fun `cleanup removes directories older than the retention window`(@TempDir tempDir: Path) {
        val oldDir = tempDir.resolve("2020-01-01").resolve("chronos")
        Files.createDirectories(oldDir)
        Files.writeString(oldDir.resolve("aaa.html"), "old")
        Files.setLastModifiedTime(tempDir.resolve("2020-01-01"), java.nio.file.attribute.FileTime.from(Instant.parse("2020-01-01T00:00:00Z")))

        val archiver = RawHtmlArchiver(enabled = true, basePath = tempDir.toString(), retentionDays = 30)
        archiver.cleanup()

        Files.exists(tempDir.resolve("2020-01-01")) shouldBe false
    }
}
