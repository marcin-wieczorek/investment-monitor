package pl.marcinwieczorek.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class TarnowoPodgorneWzParserTest {

    private val parser = RekordBipParser(TarnowoPodgorneWzSource.MUNICIPALITY, TarnowoPodgorneWzSource.SOURCE_ID)

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/tarnowo-podgorne-wz/announcements.html"))
    }

    @Test
    fun `parses every published announcement`() {
        val signals = parser.parse(fixtureHtml, TarnowoPodgorneWzSource.LIST_URL)
        signals shouldHaveSize 10
    }

    @Test
    fun `parses the first announcement with reference, date and url`() {
        val signal = parser.parse(fixtureHtml, TarnowoPodgorneWzSource.LIST_URL)
            .single { it.reference == "WZP.6733.20.2026" }

        signal.source shouldBe "tarnowo-podgorne-wz"
        signal.municipality shouldBe "Tarnowo Podgórne"
        signal.url.toString() shouldBe "http://bip2.tarnowo-podgorne.pl/6037/dokument/34193"
    }
}
