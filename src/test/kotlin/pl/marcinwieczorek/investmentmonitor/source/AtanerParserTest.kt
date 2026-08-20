package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class AtanerParserTest {

    private val parser = AtanerParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/ataner/investment-list.html"))
    }

    @Test
    fun `parses only the desktop slider, not the duplicated mobile one`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 2
    }

    @Test
    fun `parses Swierzawska 13 with unit count and district`() {
        val swierzawska = parser.parse(fixtureHtml).single { it.name == "Świerzawska 13" }

        swierzawska.source shouldBe SourceId("ataner")
        swierzawska.developer shouldBe "Ataner"
        swierzawska.units shouldBe 42
        swierzawska.location shouldBe "Grunwald"
    }
}
