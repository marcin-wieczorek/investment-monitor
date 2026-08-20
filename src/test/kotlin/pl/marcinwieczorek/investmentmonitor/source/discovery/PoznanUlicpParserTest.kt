package pl.marcinwieczorek.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path

class PoznanUlicpParserTest {

    private val parser = PoznanUlicpParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/poznan-ulicp/announcements.html"))
    }

    @Test
    fun `parses every published announcement`() {
        val signals = parser.parse(fixtureHtml)
        signals shouldHaveSize 19
    }

    @Test
    fun `extracts the investment description, not the legal boilerplate`() {
        val signal = parser.parse(fixtureHtml).single { it.reference == "UA-IV.6733.137.2026" }

        signal.source shouldBe "poznan-ulicp"
        signal.municipality shouldBe "Poznań"
        signal.title shouldBe "Budowa sieci wodociągowej"
        signal.signalType shouldBe SignalType.LAND_DEVELOPMENT_SIGNAL
        signal.url.toString() shouldBe
            "https://bip.poznan.pl/bip/wydzial-urbanistyki-i-architektury,31/news/ua-iv-6733-137-2026,c,8440/ua-iv-6733-137-2026,284450.html"
    }

    @Test
    fun `parses the validity-window start date`() {
        val signal = parser.parse(fixtureHtml).single { it.reference == "UA-IV.6733.137.2026" }
        signal.detectedAt.toString() shouldBe "2026-08-19T00:00:00Z"
    }
}
