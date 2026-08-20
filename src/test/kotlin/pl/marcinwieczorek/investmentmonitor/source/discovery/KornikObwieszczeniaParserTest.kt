package pl.marcinwieczorek.investmentmonitor.source.discovery

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path

class KornikObwieszczeniaParserTest {

    private val parser = KornikObwieszczeniaParser()

    private val indexHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/kornik-obwieszczenia/index.html"))
    }
    private val yearHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/kornik-obwieszczenia/year-2026.html"))
    }

    @Test
    fun `finds the current (highest) year's register URL from the index page`() {
        val url = parser.findCurrentYearUrl(indexHtml, KornikObwieszczeniaSource.INDEX_URL)
        // The sidebar nav tree duplicates this link with an unrelated,
        // relative href (a different section entirely) - only the
        // absolute in-body link is the real obwieszczenia year page.
        url shouldBe "https://bip.kornik.pl/2026-rok"
    }

    @Test
    fun `parses only the Wydzial Planowania Przestrzennego accordion, skipping unrelated departments`() {
        val signals = parser.parse(yearHtml, "https://bip.kornik.pl/2026-rok")
        signals shouldHaveSize 202
    }

    @Test
    fun `parses a warunki zabudowy announcement with reference, village and Polish-text date`() {
        val signals = parser.parse(yearHtml, "https://bip.kornik.pl/2026-rok")
        val signal = signals.first { it.reference == "WB1-PP.6730.92.2026" && it.title.contains("przekazaniu do uzgodnień") }

        signal.source shouldBe SourceId("kornik-obwieszczenia")
        signal.municipality shouldBe "Kórnik"
        signal.signalType shouldBe SignalType.WZ_DECISION
        signal.location shouldBe "Radzewo"
        signal.detectedAt.toString() shouldBe "2026-08-12T00:00:00Z"
        signal.url.toString() shouldBe
            "https://bip.kornik.pl/obwieszczenie-o-przekazaniu-do-uzgodnien-projektu-decyzji-w-sprawie-wydania-decyzji-o-ustaleniu-13"
    }

    @Test
    fun `falls back to EPOCH when the announcement text has no 'z dnia' date phrasing`() {
        val signals = parser.parse(yearHtml, "https://bip.kornik.pl/2026-rok")
        val signal = signals.first { it.reference == "WB1-PP.6730.84.2026" && it.title.contains("wydaniu decyzji") }

        signal.detectedAt.toString() shouldBe "1970-01-01T00:00:00Z"
    }
}
