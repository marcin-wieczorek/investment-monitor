package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import java.nio.file.Files
import java.nio.file.Path

class JakonInwestParserTest {

    private val parser = JakonInwestParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/jakon-inwest/investment-list.html"))
    }

    @Test
    fun `parses every investment with a link, skipping the completed one without a link`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 19
    }

    @Test
    fun `parses a Poznań investment`() {
        val kornicka = parser.parse(fixtureHtml).single { it.name == "Kórnicka" }

        kornicka.source shouldBe SourceId("jakon-inwest")
        kornicka.developer shouldBe "Jakon"
        kornicka.location shouldBe "Poznań"
        kornicka.url.toString() shouldBe "https://jakon-inwest.pl/pl/inwestycja-kornicka"
        kornicka.status shouldBe InvestmentStatus.UNDER_CONSTRUCTION
    }

    @Test
    fun `parses a Tarnowo Podgorne investment on an external domain`() {
        val nowe = parser.parse(fixtureHtml).single { it.name == "Nowe Tarnowo" }

        nowe.location shouldBe "Tarnowo Podgórne"
        nowe.url.toString() shouldBe "https://nowetarnowo.pl"
        nowe.status shouldBe InvestmentStatus.READY_FOR_HANDOVER
    }

    @Test
    fun `maps last-units ribbon to LAST_UNITS`() {
        val konopnickiej = parser.parse(fixtureHtml).single { it.name == "Osiedle Konopnickiej" }
        konopnickiej.status shouldBe InvestmentStatus.LAST_UNITS
    }
}
