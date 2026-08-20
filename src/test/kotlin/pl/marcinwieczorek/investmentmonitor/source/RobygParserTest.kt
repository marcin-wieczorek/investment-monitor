package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class RobygParserTest {

    private val parser = RobygParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/robyg/investment-list.html"))
    }

    @Test
    fun `parses every investment in the clean features slider`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 3
    }

    @Test
    fun `parses Elektrownia Garbary`() {
        val elektrownia = parser.parse(fixtureHtml).single { it.name == "Elektrovnia Garbary" }

        elektrownia.source shouldBe SourceId("robyg")
        elektrownia.developer shouldBe "ROBYG"
        elektrownia.url.toString() shouldBe "https://robyg.pl/poznan/inwestycje/elektrovnia-garbary"
        elektrownia.location shouldBe "MAŁA WYSPA, UL. PANNY MARII"
    }

    @Test
    fun `resolves a relative href against the base URI`() {
        val poczatek = parser.parse(fixtureHtml).single { it.name == "Początek Piątkowo" }
        poczatek.url.toString() shouldBe "https://robyg.pl/poznan/inwestycje/poczatek-piatkowo"
    }
}
