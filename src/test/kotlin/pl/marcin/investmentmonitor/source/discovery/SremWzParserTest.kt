package pl.marcin.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path

class SremWzParserTest {

    private val parser = SremWzParser()

    private val indexHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/srem-wz/index.html"))
    }
    private val yearHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/srem-wz/year-2026.html"))
    }

    @Test
    fun `finds the current (highest) year's register URL from the index page`() {
        val url = parser.findCurrentYearUrl(indexHtml, SremWzSource.INDEX_URL)
        url shouldBe "http://bip.srem.pl/public/?id=238338"
    }

    @Test
    fun `parses every announcement on the year page`() {
        val signals = parser.parse(yearHtml, "http://bip.srem.pl/public/?id=238338")
        signals.size shouldBe 23
    }

    @Test
    fun `parses a residential warunki zabudowy announcement`() {
        val signals = parser.parse(yearHtml, "http://bip.srem.pl/public/?id=238338")
        val signal = signals.first { it.title.contains("Gostyńska") }

        signal.source shouldBe "srem-wz"
        signal.municipality shouldBe "Śrem"
        signal.signalType shouldBe SignalType.WZ_DECISION
        signal.location shouldBe "Śrem"
        signal.reference shouldBe null
        signal.url.toString() shouldBe "http://bip.srem.pl/public/getFile?id=625578"
        signal.detectedAt.toString() shouldBe "2026-08-13T00:00:00Z"
    }
}
