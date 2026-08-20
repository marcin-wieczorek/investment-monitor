package pl.marcinwieczorek.investmentmonitor.source

import pl.marcinwieczorek.investmentmonitor.domain.SourceId

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class GGWParserTest {

    private val parser = GGWParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/ggw/investment-list.html"))
    }

    @Test
    fun `parses both investments hosted on their own external domains`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 2
    }

    @Test
    fun `parses Osiedle Bojerowa`() {
        val bojerowa = parser.parse(fixtureHtml).single { it.name == "Osiedle Bojerowa" }

        bojerowa.source shouldBe SourceId("ggw")
        bojerowa.developer shouldBe "GGW Development"
        bojerowa.url.toString() shouldBe "https://bojerowa.pl"
        bojerowa.location shouldBe null
    }
}
