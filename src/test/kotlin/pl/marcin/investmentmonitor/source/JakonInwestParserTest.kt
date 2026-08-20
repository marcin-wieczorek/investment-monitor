package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
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

        kornicka.source shouldBe "jakon-inwest"
        kornicka.developer shouldBe "Jakon"
        kornicka.location shouldBe "Poznań"
        kornicka.url.toString() shouldBe "https://jakon-inwest.pl/pl/inwestycja-kornicka"
    }

    @Test
    fun `parses a Tarnowo Podgorne investment on an external domain`() {
        val nowe = parser.parse(fixtureHtml).single { it.name == "Nowe Tarnowo" }

        nowe.location shouldBe "Tarnowo Podgórne"
        nowe.url.toString() shouldBe "https://nowetarnowo.pl"
    }
}
