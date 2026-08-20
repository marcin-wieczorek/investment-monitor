package pl.marcinwieczorek.investmentmonitor.source

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class PekabexParserTest {

    private val parser = PekabexParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/pekabex/investment-list.html"))
    }

    @Test
    fun `deduplicates the Webflow infinite-loop carousel by URL`() {
        val investments = parser.parse(fixtureHtml)
        investments shouldHaveSize 10
    }

    @Test
    fun `parses a Poznan investment`() {
        val jaSielska = parser.parse(fixtureHtml).single { it.name == "Osiedle JA_SIELSKA" }

        jaSielska.source shouldBe "pekabex"
        jaSielska.developer shouldBe "Pekabex Development"
        jaSielska.url.toString() shouldBe "https://pekabexdevelopment.com/inwestycja/mieszkania-w-poznaniu-ja_sielska/"
    }

    @Test
    fun `skips slides with a placeholder hash link`() {
        parser.parse(fixtureHtml).none { it.name == "Casa Fiore" } shouldBe true
    }
}
