package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class MurapolParserTest {

    private val parser = MurapolParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/murapol/investment-list.html"))
    }

    @Test
    fun `parses only the desktop row, not the duplicated mobile row`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 2
    }

    @Test
    fun `parses Murapol Helvetia with unit count`() {
        val helvetia = parser.parse(fixtureHtml).single { it.name == "Murapol Helvetia" }

        helvetia.source shouldBe SourceId("murapol")
        helvetia.developer shouldBe "Murapol"
        helvetia.location shouldBe "Poznań, ul. Szwajcarska"
        helvetia.units shouldBe 65
    }

    @Test
    fun `does not guess a total price from a per-square-metre figure`() {
        val helvetia = parser.parse(fixtureHtml).single { it.name == "Murapol Helvetia" }
        helvetia.price shouldBe null
    }
}
