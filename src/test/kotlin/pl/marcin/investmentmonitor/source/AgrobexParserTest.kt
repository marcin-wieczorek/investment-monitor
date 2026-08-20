package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class AgrobexParserTest {

    private val parser = AgrobexParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/agrobex/investment-list.html"))
    }

    @Test
    fun `parses every published investment`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 9
    }

    @Test
    fun `parses Osiedle Moderno with status`() {
        val moderno = parser.parse(fixtureHtml).single { it.name == "Osiedle Moderno" }

        moderno.source shouldBe "agrobex"
        moderno.developer shouldBe "Agrobex"
        moderno.location shouldBe "Środa Wielkopolska"
        moderno.url.toString() shouldBe "https://agrobex.pl/osiedle-moderno/"
    }

    @Test
    fun `leaves unpublished fields null`() {
        val moderno = parser.parse(fixtureHtml).single { it.name == "Osiedle Moderno" }
        moderno.units shouldBe null
        moderno.price shouldBe null
        moderno.houseArea shouldBe null
    }
}
