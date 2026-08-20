package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SpraviaParserTest {

    private val parser = SpraviaParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/spravia/investment-list.html"))
    }

    @Test
    fun `parses every published investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 5
    }

    @Test
    fun `parses Lesny Marcelin with house area range`() {
        val lesny = parser.parse(fixtureHtml).single { it.name == "Leśny Marcelin" }

        lesny.source shouldBe "spravia"
        lesny.developer shouldBe "Spravia"
        lesny.url.toString() shouldBe "https://lesnymarcelin.pl/"
        lesny.houseArea?.min shouldBe 31.0
        lesny.houseArea?.max shouldBe 98.0
    }

    @Test
    fun `does not guess a total price from a per-square-metre figure`() {
        val lesny = parser.parse(fixtureHtml).single { it.name == "Leśny Marcelin" }
        lesny.price shouldBe null
    }
}
