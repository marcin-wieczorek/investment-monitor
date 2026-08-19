package pl.marcin.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcin.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path

class SwarzedzWzParserTest {

    private val parser = SwarzedzWzParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/swarzedz-wz/warunki-zabudowy.html"))
    }

    @Test
    fun `parses decision entries and skips graphic attachments`() {
        val signals = parser.parse(fixtureHtml)
        signals.shouldNotBeEmpty()
        signals.none { it.reference == null } shouldBe true
    }

    @Test
    fun `parses a residential decision naming a village`() {
        val signal = parser.parse(fixtureHtml).single { it.url.toString().endsWith("23z2026_decyzja.pdf") }

        signal.source shouldBe "swarzedz-wz"
        signal.municipality shouldBe "Swarzędz"
        signal.signalType shouldBe SignalType.WZ_DECISION
        signal.location shouldBe "Kruszewnia"
        signal.reference shouldBe "WAU.6730.23.2026"
        signal.title.contains("74 budynków mieszkalnych") shouldBe true
        signal.url.toString() shouldBe
            "https://bip.swarzedz.pl/fileadmin/BIP/Zagospodarowanie_przestrzenne/Warunki_zabudowy/2026/03_06_2026/23z2026_decyzja.pdf"
    }

    @Test
    fun `derives the detected date from the document's own URL path`() {
        val signal = parser.parse(fixtureHtml).single { it.url.toString().endsWith("23z2026_decyzja.pdf") }
        signal.detectedAt.toString() shouldBe "2026-06-03T00:00:00Z"
    }

    @Test
    fun `the same case reference can recur across multiple filing stages`() {
        val signals = parser.parse(fixtureHtml).filter { it.reference == "WAU.6730.23.2026" }
        signals.size shouldBe 5
    }

    @Test
    fun `parses hundreds of real decision entries from the live register`() {
        val signals = parser.parse(fixtureHtml)
        signals.size shouldBe 279
    }
}
