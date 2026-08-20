package pl.marcin.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class AreaParserTest {

    private val parser = AreaParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/area/investment-list.html"))
    }

    @Test
    fun `parses all four currently marketed investments`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 4
    }

    @Test
    fun `parses jantarowa 4`() {
        val jantarowa = parser.parse(fixtureHtml).single { it.name == "jantarowa 4" }

        jantarowa.source shouldBe "area"
        jantarowa.developer shouldBe "Area Development"
        jantarowa.url.toString() shouldBe "https://areadevelopment.pl/pl/jantarowa-4"
        jantarowa.location shouldBe null
    }
}
