package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class DeveliaParserTest {

    private val parser = DeveliaParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/develia/investment-list.html"))
    }

    @Test
    fun `parses every published investment without duplication`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 4
    }

    @Test
    fun `parses Krolowej Jadwigi 51`() {
        val krolowej = parser.parse(fixtureHtml).single { it.name == "Królowej Jadwigi 51" }

        krolowej.source shouldBe "develia"
        krolowej.developer shouldBe "Develia"
        krolowej.url.toString() shouldBe "https://develia.pl/pl/mieszkania/poznan/krolowej-jadwigi-51/"
        krolowej.location shouldBe "Królowej Jadwigi 51, Wilda, Poznań"
    }
}
