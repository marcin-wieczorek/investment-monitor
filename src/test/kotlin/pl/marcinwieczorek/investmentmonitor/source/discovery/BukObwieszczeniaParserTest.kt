package pl.marcinwieczorek.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path

class BukObwieszczeniaParserTest {

    private val parser = BukObwieszczeniaParser()

    private val indexHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/buk-obwieszczenia/index.html"))
    }
    private val yearHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/buk-obwieszczenia/year-2026.html"))
    }

    @Test
    fun `finds the current (highest) year's register URL from the index page`() {
        val url = parser.findCurrentYearUrl(indexHtml, BukObwieszczeniaSource.INDEX_URL)
        url shouldBe "https://bip.buk.gmina.pl/a,17792,obwieszczenia-i-komunikaty-2026-rok.html"
    }

    @Test
    fun `parses every announcement on the year page`() {
        val signals = parser.parse(yearHtml, "https://bip.buk.gmina.pl/a,17792,obwieszczenia-i-komunikaty-2026-rok.html")
        signals shouldHaveSize 205
    }

    @Test
    fun `parses a residential warunki zabudowy announcement with a case reference`() {
        val signals = parser.parse(yearHtml, "https://bip.buk.gmina.pl/a,17792,obwieszczenia-i-komunikaty-2026-rok.html")
        val signal = signals.first { it.title.contains("15 budynków mieszkalnych jednorodzinnych") }

        signal.source shouldBe "buk-obwieszczenia"
        signal.municipality shouldBe "Buk"
        signal.signalType shouldBe SignalType.WZ_DECISION
        signal.location shouldBe "Wielka Wieś"
        signal.reference shouldBe "GP.6730.432.2025"
        signal.url.toString() shouldBe "https://bip.buk.gmina.pl/api/files/47676"
        signal.detectedAt.toString() shouldBe "2026-01-08T12:02:27Z"
    }

    @Test
    fun `classifies a public-purpose siting announcement without a case reference`() {
        val signals = parser.parse(yearHtml, "https://bip.buk.gmina.pl/a,17792,obwieszczenia-i-komunikaty-2026-rok.html")
        val signal = signals.first {
            it.title.contains("budowy sieci wodociągowej w Niepruszewie w ul. Poziomkowej")
        }

        signal.signalType shouldBe SignalType.LAND_DEVELOPMENT_SIGNAL
        // "Niepruszewie" is the locative (inflected) case of "Niepruszewo" -
        // LocationCatalog only matches exact whole-word forms (no Polish
        // declension handling), so this is correctly left unmatched rather
        // than guessed.
        signal.location shouldBe null
        signal.reference shouldBe null
    }

    @Test
    fun `classifies an unrelated announcement as OTHER`() {
        val signals = parser.parse(yearHtml, "https://bip.buk.gmina.pl/a,17792,obwieszczenia-i-komunikaty-2026-rok.html")
        val signal = signals.first { it.title.contains("KWALIFIKACJI WOJSKOWEJ") }

        signal.signalType shouldBe SignalType.OTHER
    }
}
